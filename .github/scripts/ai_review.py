import json
import os
import re
import subprocess
import time
from pathlib import Path

from google import genai
from google.genai import types

from pr_report import fetch_threads, to_comment
from pr_review_prompt import (
    build_response_schema,
    build_system_prompt,
    build_user_message,
)
from pr_review_render import (
    filter_by_valid_lines,
    render_counts,
    to_github_comments,
)

REPO = os.environ["REPO"]
PR_NUMBER = os.environ["PR_NUMBER"]
REVIEW_TAG = "<!-- ai-code-review -->"
COMMENT_TITLE = "## CodeReview"
MODELS = [m.strip() for m in os.environ.get("MODELS", "").split(",") if m.strip()] \
    or ["gemini-3.6-flash"]
MAX_INPUT_TOKENS = 800_000
MAX_OUTPUT_TOKENS = 60_000
TEMPERATURE = 0.8
RETRY_DELAYS = (1, 3, 7)
INCLUDE_FILE_CONTENTS = os.environ.get("INCLUDE_FILE_CONTENTS", "true").strip().lower() == "true"
MAX_FILE_CHARS = 100_000
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


def read_changed_files(list_path="files.txt") -> list[tuple[str, str]]:
    """변경 파일 목록 경로 → [(경로, 전문)]. 옵션이 꺼져 있으면 빈 목록."""
    if not INCLUDE_FILE_CONTENTS or not Path(list_path).is_file():
        return []

    out = []
    for name in Path(list_path).read_text(encoding="utf-8").split("\n"):
        name = name.strip()
        path = Path(name)
        if not name or not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        if len(text) > MAX_FILE_CHARS:
            print(f"{name}은 {len(text):,}자라 전문에서 제외합니다.")
            continue
        out.append((name, text))
    return out


def count_input_tokens(client, system_prompt: str, user_message: str) -> int:
    """시스템 프롬프트와 사용자 메시지 → 입력 토큰 수."""
    result = client.models.count_tokens(
        model=MODELS[0],
        contents=user_message,
        config=types.CountTokensConfig(system_instruction=system_prompt),
    )
    return result.total_tokens


def fit_to_budget(client, system_prompt, base_diff, pr_title, pr_body,
                  reported=(), files=()):
    """원본 diff·PR 제목·본문·기존 지적·파일 전문 → (라인 번호가 붙은 diff, 파일별 유효 라인, 사용자 메시지, 잘렸는지)."""
    diff, truncated = base_diff, False

    for _ in range(5):
        text = diff + TRUNCATION_NOTE if truncated else diff
        annotated, valid_lines = annotate_diff(text)
        message = build_user_message(pr_title, pr_body, annotated, reported, files)

        if len(message) <= SAFE_INPUT_CHARS:
            return annotated, valid_lines, message, truncated

        used = count_input_tokens(client, system_prompt, message)
        if used <= MAX_INPUT_TOKENS:
            return annotated, valid_lines, message, truncated

        if files:
            print(f"입력 {used:,}토큰이 예산 {MAX_INPUT_TOKENS:,}을 넘어 파일 전문을 뺍니다.")
            files = ()
            continue

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


def note_model(model: str) -> None:
    """사용한 모델 → Actions 어노테이션과 실행 요약에 기록."""
    fallback = model != MODELS[0]
    if fallback:
        print(f"::notice::폴백: {MODELS[0]} → {model}")
    else:
        print(f"리뷰 모델: {model}")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as f:
            f.write(f"- 리뷰 모델: `{model}`{' (폴백)' if fallback else ''}\n")


def generate_with_retry(client, system_prompt, user_message, schema):
    """프롬프트와 응답 스키마 → (모델 응답, 사용한 모델)."""
    last_error = None
    attempts = len(RETRY_DELAYS) + 1

    for model in MODELS:
        for attempt in range(attempts):
            try:
                return client.models.generate_content(
                    model=model,
                    contents=user_message,
                    config=types.GenerateContentConfig(
                        system_instruction=system_prompt,
                        response_mime_type="application/json",
                        response_schema=schema,
                        max_output_tokens=MAX_OUTPUT_TOKENS,
                        temperature=TEMPERATURE,
                    ),
                ), model
            except Exception as e:
                code = getattr(e, "code", None)
                last_error = e

                if code == 429:
                    print(f"::warning::{model} 쿼터 소진 ({format_error(e)})")
                    break

                if code is not None and code >= 500:
                    if attempt < len(RETRY_DELAYS):
                        wait_s = RETRY_DELAYS[attempt]
                        print(f"{model} {code}, {wait_s}s 후 재시도... ({attempt + 1}/{attempts})")
                        time.sleep(wait_s)
                        continue
                    print(f"::warning::{model} {code} 반복")
                    break

                raise

    raise last_error


def fetch_reported() -> list[dict]:
    """(입력 없음) → 이미 지적된 내용. 해결 여부가 붙어 있다."""
    return [c for c in map(to_comment, fetch_threads()) if c]


def next_review_number() -> int:
    """(입력 없음) → 이번에 제출할 리뷰 번호."""
    result = run_gh([
        "api", f"repos/{REPO}/pulls/{PR_NUMBER}/reviews",
        "--paginate", "--jq", ".[].body",
    ])
    return sum(1 for b in result.stdout.splitlines() if REVIEW_TAG in b) + 1


def build_inline_payload(review, valid_line_map, existing, number, model=""):
    """리뷰 결과·유효 라인·이미 지적된 내용·리뷰 번호·모델 → reviews API 페이로드. 새로 달 것이 없으면 None."""
    filtered = filter_by_valid_lines(review, valid_line_map)
    already = {(c.get("file"), c.get("line")) for c in existing}
    fresh = [
        c for c in filtered["comments"]
        if (c.get("file"), c.get("line")) not in already
    ]

    inline = to_github_comments({"comments": fresh})
    if not inline:
        return None

    body = f"{REVIEW_TAG}\n{COMMENT_TITLE} #{number}\n\n**{render_counts(fresh)}**"
    if model:
        body += f" · `{model}`"
    dropped = filtered.get("dropped_comments", [])
    if dropped:
        body += f"\n\n> {len(dropped)}건은 diff 범위 밖이라 인라인으로 달지 못했습니다."
    return {"event": "COMMENT", "body": body, "comments": inline}


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
    system_prompt = build_system_prompt(tech_stack)
    client = genai.Client(api_key=os.environ["MODEL_API_KEY"])
    reported = fetch_reported()

    _, valid_line_map, user_message, truncated = fit_to_budget(
        client,
        system_prompt,
        diff_output,
        os.environ.get("PR_TITLE", "No Title"),
        os.environ.get("PR_BODY", "No Body"),
        reported,
        read_changed_files(),
    )
    if truncated:
        print("::warning::diff가 토큰 예산을 넘어 뒷부분이 잘린 채 리뷰했습니다.")

    try:
        result, model = generate_with_retry(
            client, system_prompt, user_message, build_response_schema()
        )
    except Exception as e:
        print(f"::error::리뷰 생성 실패 (API 오류: {format_error(e)}, 시도한 모델: {', '.join(MODELS)})")
        raise

    note_model(model)

    try:
        review = json.loads(result.text)
    except (json.JSONDecodeError, TypeError) as e:
        print(f"::error::리뷰 응답 파싱 실패 ({type(e).__name__}: {e},"
              f" finish_reason: {finish_reason(result)})")
        print("Raw response:", preview(result.text))
        raise

    number = next_review_number()
    payload = build_inline_payload(review, valid_line_map, reported, number, model)
    if payload is None:
        payload = {
            "event": "COMMENT",
            "comments": [],
            "body": (
                f"{REVIEW_TAG}\n{COMMENT_TITLE} #{number}\n\n"
                f"**새로 달 지적 없음** · `{model}`\n\n"
                "이미 지적된 위치 외에 새로 발견된 내용이 없습니다."
            ),
        }

    run_gh(
        ["api", f"repos/{REPO}/pulls/{PR_NUMBER}/reviews", "--input", "-"],
        input_text=json.dumps(payload),
    )
    print(f"리뷰 #{number} 제출 완료 (모델 {model}, 인라인 {len(payload['comments'])}개).")


if __name__ == "__main__":
    main()
