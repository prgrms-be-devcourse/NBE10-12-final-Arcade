import re

_SEVERITY = {
    "critical": ("🔴", 0),
    "warning": ("🟡", 1),
    "info": ("🟢", 2),
}
_FALLBACK = ("🟡", 1)

_MARKER_TO_SEVERITY = {"🔴": "critical", "🟡": "warning", "🟢": "info"}

_PROBABLE_NOTE = "> 확정되지 않은 지적입니다."

MAX_COMMENTS = 10


def _marker(severity: str, confidence: str) -> str:
    """severity와 confidence → 이모지 마커."""
    mark, _ = _SEVERITY.get(severity, _FALLBACK)
    if severity == "critical" and confidence == "probable":
        return mark + "?"
    return mark


def _rank(comment: dict) -> int:
    """comment → 정렬 순서(critical=0, warning=1, info=2)."""
    _, order = _SEVERITY.get(comment.get("severity"), _FALLBACK)
    return order


def render_comment(comment: dict) -> str:
    """comment → 인라인 코멘트 본문."""
    marker = _marker(comment.get("severity", ""), comment.get("confidence", ""))
    text = comment.get("text", "").strip()
    body = f"{marker} {text}"
    if comment.get("confidence") == "probable":
        body += "\n\n> 확정되지 않은 지적입니다. 위 내용을 확인한 뒤 판단해 주세요."
    return body


def to_github_comments(review: dict) -> list[dict]:
    """리뷰 결과 → reviews API의 comments 배열. 최대 MAX_COMMENTS개."""
    out = []
    for c in sorted(review.get("comments", []), key=_rank):
        path, line = c.get("file"), c.get("line")
        if not path or not isinstance(line, int):
            continue
        out.append({
            "path": path,
            "line": line,
            "side": "RIGHT",
            "body": render_comment(c),
        })
        if len(out) >= MAX_COMMENTS:
            break
    return out


def render_counts(comments: list[dict]) -> str:
    """comment 목록 → 심각도별 개수 한 줄."""
    counts = {}
    for c in comments:
        sev = c.get("severity", "unknown")
        counts[sev] = counts.get(sev, 0) + 1

    if not counts:
        return "지적 사항 없음"

    parts = [
        f"{_SEVERITY.get(s, _FALLBACK)[0]} {counts[s]}"
        for s in ("critical", "warning", "info")
        if s in counts
    ]
    return " · ".join(parts)


def _escape_cell(text: str) -> str:
    """문자열 → 표 칸에 넣을 수 있게 이스케이프한 문자열."""
    return text.replace("|", "\\|").replace("\n", " ").strip()


def render_status(comment: dict) -> str:
    """comment → 표에 쓸 상태 문구."""
    if comment.get("resolved"):
        return "✅ 해결됨"
    if comment.get("outdated"):
        return "🔄 코드 변경됨"
    return "⏳ 미해결"


def _table_order(comment: dict):
    """comment → 표 정렬 키. 미해결을 먼저, 그 안에서 심각도 순."""
    return (1 if comment.get("resolved") else 0, _rank(comment))


def render_issues_table(review: dict) -> str:
    """리뷰 결과 → 이슈 표 마크다운. 지적이 없으면 빈 문자열."""
    comments = sorted(review.get("comments", []), key=_table_order)
    if not comments:
        return ""

    rows = ["\n### 🔍 발견된 이슈\n",
            "| 파일 | 라인 | 심각도 | 상태 | 내용 |\n",
            "|------|------|--------|------|------|\n"]

    for c in comments:
        name = str(c.get("file") or "-").rsplit("/", 1)[-1]
        line = c.get("line") if isinstance(c.get("line"), int) else "-"
        marker = _marker(c.get("severity", ""), c.get("confidence", ""))
        rows.append(
            f"| `{_escape_cell(name)}` | {line} | {marker} | {render_status(c)} "
            f"| {_escape_cell(c.get('text', ''))} |\n"
        )
    return "".join(rows)


def filter_by_valid_lines(review: dict, valid_lines: dict[str, set[int]]) -> dict:
    """리뷰 결과와 유효 라인 맵 → comments와 dropped_comments로 나눈 리뷰 결과."""
    kept, dropped = [], []
    for c in review.get("comments", []):
        path, line = c.get("file"), c.get("line")
        if isinstance(line, int) and line in valid_lines.get(path, set()):
            kept.append(c)
        else:
            dropped.append(c)
    return {**review, "comments": kept, "dropped_comments": dropped}


# 주의: 아래 함수는 render_comment()가 만든 형식에 의존한다.
#       render_comment()의 출력 형식을 바꾸면 파싱이 조용히 실패한다.
def parse_rendered_comment(raw: dict) -> dict | None:
    """PR에 달린 인라인 코멘트 → comment 딕셔너리. 형식이 다르면 None."""
    body = (raw.get("body") or "").strip()
    matched = re.match(r"^(🔴|🟡|🟢)(\?)?\s+(.*)$", body, re.DOTALL)
    if not matched:
        return None

    marker, probable, text = matched.groups()
    text = text.split(_PROBABLE_NOTE)[0].strip()
    return {
        "file": raw.get("path"),
        "line": raw.get("line"),
        "severity": _MARKER_TO_SEVERITY[marker],
        "confidence": "probable" if probable else "confirmed",
        "text": text,
    }
