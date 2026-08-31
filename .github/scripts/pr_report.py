import json
import os
import re
import subprocess

from pr_review_render import parse_rendered_comment, render_counts, render_issues_table

REPO = os.environ["REPO"]
PR_NUMBER = os.environ["PR_NUMBER"]
REPORT_TAG = "<!-- ai-code-review-report -->"
COMMENT_TITLE = "## CodeReview"
BOT_LOGIN = "github-actions"

# 주의: 해결 여부는 리뷰 스레드에만 있다. REST의 pulls/{n}/comments에는 없으므로
#       GraphQL을 REST로 되돌리면 해결 표시가 조용히 사라진다.
THREAD_QUERY = """
query($owner: String!, $name: String!, $number: Int!, $cursor: String) {
  repository(owner: $owner, name: $name) {
    pullRequest(number: $number) {
      reviewThreads(first: 100, after: $cursor) {
        pageInfo { hasNextPage endCursor }
        nodes {
          isResolved
          isOutdated
          comments(first: 1) {
            nodes { path line originalLine body author { login } }
          }
        }
      }
    }
  }
}
"""


def run_gh(args, input_text=None):
    """gh 명령 인자와 표준입력 → 실행 결과(CompletedProcess)."""
    return subprocess.run(
        ["gh", *args], input=input_text, capture_output=True, text=True, check=True
    )


def fetch_threads() -> list[dict]:
    """(입력 없음) → 이 PR의 리뷰 스레드 목록."""
    owner, name = REPO.split("/", 1)
    threads, cursor = [], None

    while True:
        args = [
            "api", "graphql", "-f", f"query={THREAD_QUERY}",
            "-F", f"owner={owner}", "-F", f"name={name}", "-F", f"number={PR_NUMBER}",
        ]
        if cursor:
            args += ["-F", f"cursor={cursor}"]

        page = json.loads(run_gh(args).stdout)
        page = page["data"]["repository"]["pullRequest"]["reviewThreads"]
        threads += page["nodes"]

        if not page["pageInfo"]["hasNextPage"]:
            return threads
        cursor = page["pageInfo"]["endCursor"]


def to_comment(thread: dict) -> dict | None:
    """리뷰 스레드 → 해결 여부가 붙은 comment 딕셔너리. 봇이 남긴 것이 아니면 None."""
    nodes = thread.get("comments", {}).get("nodes") or []
    if not nodes:
        return None

    head = nodes[0]
    if not (head.get("author") or {}).get("login", "").startswith(BOT_LOGIN):
        return None

    parsed = parse_rendered_comment({
        "path": head.get("path"),
        "line": head.get("line") or head.get("originalLine"),
        "body": head.get("body"),
    })
    if parsed is None:
        return None

    parsed["resolved"] = bool(thread.get("isResolved"))
    parsed["outdated"] = bool(thread.get("isOutdated"))
    return parsed


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


def build_body(comments: list[dict], revision: int) -> str:
    """comment 목록과 갱신 회차 → 보고서 코멘트 본문."""
    open_ones = [c for c in comments if not c.get("resolved")]
    resolved = len(comments) - len(open_ones)

    if not comments:
        head = "지적 사항 없음"
    elif open_ones:
        head = render_counts(open_ones)
        if resolved:
            head += f" · 해결 {resolved}"
    else:
        head = f"미해결 없음 · 해결 {resolved}"

    suffix = f" ({revision}번째 갱신)" if revision else ""
    return (
        f"{REPORT_TAG}\n"
        f"<!-- report-rev:{revision} -->\n"
        f"{COMMENT_TITLE}{suffix}\n\n"
        f"**{head}**\n"
        f"{render_issues_table({'comments': comments})}"
    )


def upsert(comment_id, body: str) -> str:
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


def main():
    comments = [c for c in map(to_comment, fetch_threads()) if c]
    comment_id, prev_body = find_report_comment()
    revision = next_revision(prev_body)

    action = upsert(comment_id, build_body(comments, revision))
    resolved = sum(1 for c in comments if c.get("resolved"))
    print(f"보고서 {action} 완료 (갱신 {revision}회차, 지적 {len(comments)}건 중 해결 {resolved}건).")


if __name__ == "__main__":
    main()
