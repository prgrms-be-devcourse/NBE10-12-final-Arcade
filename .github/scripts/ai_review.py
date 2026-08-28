import json
import os
import re
import subprocess
import time
from pathlib import Path

from google import genai
from google.genai import types

from pr_review_prompt import build_system_prompt, build_user_message
from pr_review_render import (
    filter_by_valid_lines,
    render_issues_table,
    render_summary,
    to_github_comments,
)

REPO = os.environ["REPO"]
PR_NUMBER = os.environ["PR_NUMBER"]
REVIEW_TAG = "<!-- ai-code-review -->"
COMMENT_TITLE = "## CodeReview"
MODEL = "gemini-3.6-flash"
MAX_DIFF_CHARS = 40000


def run_gh(args, input_text=None):
    return subprocess.run(
        ["gh", *args], input=input_text, capture_output=True, text=True, check=True
    )


def post_plain_comment(body_text: str):
    run_gh(["pr", "comment", PR_NUMBER, "--body", body_text])


def annotate_diff(diff_text: str):
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


def generate_with_retry(client, system_prompt, user_message, retries=3):
    for attempt in range(retries):
        try:
            return client.models.generate_content(
                model=MODEL,
                contents=user_message,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    response_mime_type="application/json",
                ),
            )
        except Exception as e:
            status = getattr(e, "status_code", None) or getattr(e, "status", None)
            if status in (429, 503) and attempt < retries - 1:
                wait_s = (attempt + 1) * 10
                print(f"Gemini {status}, {wait_s}s 후 재시도... ({attempt + 1}/{retries})")
                time.sleep(wait_s)
            else:
                raise


def next_review_number() -> int:
    result = run_gh([
        "api", f"repos/{REPO}/pulls/{PR_NUMBER}/reviews",
        "--paginate", "--jq", ".[].body",
    ])
    return sum(1 for b in result.stdout.splitlines() if REVIEW_TAG in b) + 1


def main():
    diff_output = open("diff.txt", encoding="utf-8").read().strip()
    if not diff_output:
        print("검토할 변경 사항이 없습니다.")
        return

    if len(diff_output) > MAX_DIFF_CHARS:
        diff_output = diff_output[:MAX_DIFF_CHARS] + "\n... (Diff truncated due to size)"

    tech_stack_path = Path("docs/tech-stack.md")
    tech_stack = (
        tech_stack_path.read_text(encoding="utf-8")
        if tech_stack_path.is_file()
        else ""
    )
    annotated_diff, valid_line_map = annotate_diff(diff_output)

    system_prompt = build_system_prompt(tech_stack)
    user_message = build_user_message(
        os.environ.get("PR_TITLE", "No Title"),
        os.environ.get("PR_BODY", "No Body"),
        annotated_diff,
    )

    client = genai.Client(api_key=os.environ["MODEL_API_KEY"])

    try:
        result = generate_with_retry(client, system_prompt, user_message)
    except Exception as e:
        post_plain_comment(
            f"{REVIEW_TAG}\n{COMMENT_TITLE}\n\n⚠️ 리뷰 생성에 실패했습니다 (API 오류: {e}).\n"
            "Actions 로그를 확인해주세요."
        )
        return

    try:
        review = json.loads(result.text)
    except json.JSONDecodeError:
        print("JSON Parse Error. Raw response:", result.text)
        post_plain_comment(
            f"{REVIEW_TAG}\n{COMMENT_TITLE}\n\n⚠️ 리뷰 응답을 해석하지 못했습니다 (JSON 파싱 오류).\n"
            "Actions 로그를 확인해주세요."
        )
        return

    filtered = filter_by_valid_lines(review, valid_line_map)
    inline = to_github_comments(filtered)

    table_source = {**review, "comments": review.get("comments", [])}

    number = next_review_number()
    body = (
        f"{REVIEW_TAG}\n"
        f"{COMMENT_TITLE} #{number}\n\n"
        f"{render_summary(review)}\n"
        f"{render_issues_table(table_source)}"
    )
    dropped = filtered.get("dropped_comments", [])
    if dropped:
        body += f"\n> 위 {len(dropped)}건은 diff 범위 밖이라 인라인으로 달지 못했습니다.\n"

    payload = {"event": "COMMENT", "body": body, "comments": inline}
    run_gh(
        ["api", f"repos/{REPO}/pulls/{PR_NUMBER}/reviews", "--input", "-"],
        input_text=json.dumps(payload),
    )
    print(f"리뷰 #{number} 제출 완료 (인라인 {len(inline)}개, 제외 {len(dropped)}건).")


if __name__ == "__main__":
    main()
