import json
import os
import re
import subprocess
import time
from pathlib import Path

from google import genai
from google.genai import types

from pr_review_prompt import (
    build_response_schema,
    build_system_prompt,
    build_user_message,
)
from pr_review_render import (
    filter_by_valid_lines,
    merge_comments,
    parse_rendered_comment,
    render_counts,
    render_issues_table,
    render_summary,
    to_github_comments,
)

REPO = os.environ["REPO"]
PR_NUMBER = os.environ["PR_NUMBER"]
REVIEW_TAG = "<!-- ai-code-review -->"
REPORT_TAG = "<!-- ai-code-review-report -->"
REVIEW_MODE = os.environ.get("REVIEW_MODE", "report")
COMMENT_TITLE = "## CodeReview"
MODEL = os.environ.get("MODEL", "").strip() or "gemini-3.6-flash"
MAX_INPUT_TOKENS = 800_000
MAX_OUTPUT_TOKENS = 60_000
TEMPERATURE = 0.8
SAFE_INPUT_CHARS = 200_000
TRUNCATION_NOTE = "\n... (Diff truncated due to size)"


def run_gh(args, input_text=None):
    """gh 명령 인자와 표준입력 → 실행 결과(CompletedProcess)."""
    return subprocess.run(
        ["gh", *args], input=input_text, capture_output=True, text=True, check=True
    )


def format_error(e: Exception) -> str:
    """예외 → 코드와 메시지만 남긴 한 줄 문자열."""
    code = getattr(e, "code", None)
    message = getattr(e, "message", None) or str(e)
    return f"{code} {message}" if code else message


def count_input_tokens(client, system_prompt: str, user_message: str) -> int:
    """시스템 프롬프트와 사용자 메시지 → 입력 토큰 수."""
    result = client.models.count_tokens(
        model=MODEL,
        contents=user_message,
        config=types.CountTokensConfig(system_instruction=system_prompt),
    )
    return result.total_tokens


def fit_to_budget(client, system_prompt, base_diff, pr_title, pr_body):
    """원본 diff와 PR 제목·본문 → (라인 번호가 붙은 diff, 파일별 유효 라인, 사용자 메시지, 잘렸는지)."""
    diff, truncated = base_diff, False

    for _ in range(4):
        text = diff + TRUNCATION_NOTE if truncated else diff
        annotated, valid_lines = annotate_diff(text)
        message = build_user_message(pr_title, pr_body, annotated)

        if len(message) <= SAFE_INPUT_CHARS:
            return annotated, valid_lines, message, truncated

        used = count_input_tokens(client, system_prompt, message)
        if used <= MAX_INPUT_TOKENS:
            return annotated, valid_lines, message, truncated

        print(f"입력 {used:,}토큰이 예산 {MAX_INPUT_TOKENS:,}을 넘어 diff를 줄입니다.")
        diff = base_diff[: int(len(diff) * MAX_INPUT_TOKENS / used * 0.95)]
        truncated = True

    return annotated, valid_lines, message, truncated


def finish_reason(result) -> str:
    """모델 응답 → finish_reason 문자열."""
    candidates = getattr(result, "candidates", None) or []
    if not candidates:
        return "candidates 없음"
    return str(getattr(candidates[0], "finish_reason", "unknown"))


def preview(text, limit=3000) -> str:
    """응답 텍스트 → 양끝만 남긴 로그용 문자열."""
    if text is None:
        return "(없음)"
    if len(text) <= limit:
        return text
    half = limit // 2
    return f"{text[:half]}\n...({len(text) - limit}자 생략)...\n{text[-half:]}"


def annotate_diff(diff_text: str):
    """unified diff → (라인 번호가 붙은 diff, 파일별 유효 라인 집합)."""
    annotated, valid_lines = [], {}
    current_file, new_line_num = None, 0

    for line in diff_text.split("\n"):
        file_match = re.match(r"^\+\+\+ b/(.+)$", line)
        if file_match:
            current_file = file_match.group(1)
            valid_lines.setdefault(current_file, set())
            annotated.append(line)
            continue

        hunk_match = re.match(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@", line)
        if hunk_match:
            new_line_num = int(hunk_match.group(1))
            annotated.append(line)
            continue

        if current_file is None:
            annotated.append(line)
        elif line.startswith("+") and not line.startswith("+++"):
            annotated.append(f"{new_line_num:>5} {line}")
            valid_lines[current_file].add(new_line_num)
            new_line_num += 1
        elif line.startswith("-") and not line.startswith("---"):
            annotated.append(f"      {line}")
        elif line.startswith(" "):
            annotated.append(f"{new_line_num:>5} {line}")
            valid_lines[current_file].add(new_line_num)
            new_line_num += 1
        else:
            annotated.append(line)

    return "\n".join(annotated), valid_lines


def generate_with_retry(client, system_prompt, user_message, schema, retries=3):
    """프롬프트와 응답 스키마 → 모델 응답."""
    for attempt in range(retries):
        try:
            return client.models.generate_content(
                model=MODEL,
                contents=user_message,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    response_mime_type="application/json",
                    response_schema=schema,
                    max_output_tokens=MAX_OUTPUT_TOKENS,
                    temperature=TEMPERATURE,
                ),
            )
        except Exception as e:
            code = getattr(e, "code", None)
            if code in (429, 503) and attempt < retries - 1:
                wait_s = (attempt + 1) * 10
                print(f"Gemini {code}, {wait_s}s 후 재시도... ({attempt + 1}/{retries})")
                time.sleep(wait_s)
            else:
                raise


def fetch_bot_inline_comments() -> list[dict]:
    """(입력 없음) → 봇이 남긴 인라인 코멘트 중 line이 유효한 [{path, line, body}]."""
    result = run_gh([
        "api", f"repos/{REPO}/pulls/{PR_NUMBER}/comments", "--paginate",
        "--jq", '.[] | select(.user.type == "Bot") | {path, line, body}',
    ])
    rows = [json.loads(line) for line in result.stdout.splitlines() if line.strip()]
    return [r for r in rows if r.get("line")]


def find_report_comment():
    """(입력 없음) → 보고서 코멘트의 (id, body). 없으면 (None, "")."""
    result = run_gh([
        "api", f"repos/{REPO}/issues/{PR_NUMBER}/comments", "--paginate",
        "--jq", f'.[] | select(.user.type == "Bot")'
                f' | select(.body | contains("{REPORT_TAG}")) | {{id, body}}',
    ])
    rows = [json.loads(line) for line in result.stdout.splitlines() if line.strip()]
    return (rows[0]["id"], rows[0]["body"]) if rows else (None, "")


def next_revision(prev_body: str) -> int:
    """이전 보고서 본문 → 이번 갱신 회차. 최초 생성은 0."""
    if not prev_body:
        return 0
    matched = re.search(r"<!-- report-rev:(\d+) -->", prev_body)
    return int(matched.group(1)) + 1 if matched else 1


def upsert_report(comment_id, body: str) -> str:
    """코멘트 id와 본문 → "갱신" 또는 "생성"."""
    payload = json.dumps({"body": body})
    if comment_id:
        run_gh(
            ["api", "--method", "PATCH", f"repos/{REPO}/issues/comments/{comment_id}",
             "--input", "-"],
            input_text=payload,
        )
        return "갱신"

    run_gh(
        ["api", "--method", "POST", f"repos/{REPO}/issues/{PR_NUMBER}/comments",
         "--input", "-"],
        input_text=payload,
    )
    return "생성"


def next_review_number() -> int:
    """(입력 없음) → 이번에 제출할 리뷰 번호."""
    result = run_gh([
        "api", f"repos/{REPO}/pulls/{PR_NUMBER}/reviews",
        "--paginate", "--jq", ".[].body",
    ])
    return sum(1 for b in result.stdout.splitlines() if REVIEW_TAG in b) + 1


def build_inline_payload(review, valid_line_map, existing, number):
    """리뷰 결과·유효 라인·기존 코멘트·리뷰 번호 → reviews API 페이로드. 새로 달 것이 없으면 None."""
    filtered = filter_by_valid_lines(review, valid_line_map)
    already = {(c["path"], c["line"]) for c in existing}
    fresh = [
        c for c in filtered["comments"]
        if (c.get("file"), c.get("line")) not in already
    ]

    inline = to_github_comments({"comments": fresh})
    if not inline:
        return None

    body = f"{REVIEW_TAG}\n{COMMENT_TITLE} #{number}\n\n**{render_counts(fresh)}**"
    dropped = filtered.get("dropped_comments", [])
    if dropped:
        body += f"\n\n> {len(dropped)}건은 diff 범위 밖이라 인라인으로 달지 못했습니다."
    return {"event": "COMMENT", "body": body, "comments": inline}


def build_report_body(review, existing, revision=0):
    """리뷰 결과·기존 인라인 코멘트·갱신 회차 → 보고서 코멘트 본문."""
    carried = [p for p in map(parse_rendered_comment, existing) if p]
    comments = merge_comments(review.get("comments", []), carried)
    source = {**review, "comments": comments}

    suffix = f" ({revision}번째 갱신)" if revision else ""
    body = (
        f"{REPORT_TAG}\n"
        f"<!-- report-rev:{revision} -->\n"
        f"{COMMENT_TITLE}{suffix}\n\n"
        f"{render_summary(source)}\n"
        f"{render_issues_table(source)}"
    )
    added = len(comments) - len(review.get("comments", []))
    if added:
        body += f"\n> 이 중 {added}건은 draft 단계에서 인라인으로 지적된 내용입니다.\n"
    return body


def main():
    diff_output = open("diff.txt", encoding="utf-8").read().strip()
    if not diff_output:
        print("검토할 변경 사항이 없습니다.")
        return

    tech_stack_path = Path("docs/tech-stack.md")
    tech_stack = (
        tech_stack_path.read_text(encoding="utf-8")
        if tech_stack_path.is_file()
        else ""
    )
    system_prompt = build_system_prompt(tech_stack, REVIEW_MODE)
    client = genai.Client(api_key=os.environ["MODEL_API_KEY"])

    _, valid_line_map, user_message, truncated = fit_to_budget(
        client,
        system_prompt,
        diff_output,
        os.environ.get("PR_TITLE", "No Title"),
        os.environ.get("PR_BODY", "No Body"),
    )
    if truncated:
        print("::warning::diff가 토큰 예산을 넘어 뒷부분이 잘린 채 리뷰했습니다.")

    try:
        result = generate_with_retry(
            client, system_prompt, user_message, build_response_schema(REVIEW_MODE)
        )
    except Exception as e:
        print(f"::error::리뷰 생성 실패 (API 오류: {format_error(e)})")
        raise

    try:
        review = json.loads(result.text)
    except (json.JSONDecodeError, TypeError) as e:
        print(f"::error::리뷰 응답 파싱 실패 ({type(e).__name__}: {e},"
              f" finish_reason: {finish_reason(result)})")
        print("Raw response:", preview(result.text))
        raise

    existing = fetch_bot_inline_comments()

    if REVIEW_MODE != "inline":
        comment_id, prev_body = find_report_comment()
        revision = next_revision(prev_body)
        action = upsert_report(comment_id, build_report_body(review, existing, revision))
        print(f"보고서 {action} 완료 (갱신 {revision}회차).")
        return

    number = next_review_number()
    payload = build_inline_payload(review, valid_line_map, existing, number)
    if payload is None:
        print("이미 지적한 위치뿐이라 새로 달 인라인 코멘트가 없습니다.")
        return

    run_gh(
        ["api", f"repos/{REPO}/pulls/{PR_NUMBER}/reviews", "--input", "-"],
        input_text=json.dumps(payload),
    )
    print(f"리뷰 #{number} 제출 완료 (인라인 {len(payload['comments'])}개).")


if __name__ == "__main__":
    main()
