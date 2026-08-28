_SEVERITY = {
    "critical": ("🔴", 0),
    "warning": ("🟡", 1),
    "info": ("🟢", 2),
}
_FALLBACK = ("🟡", 1)

MAX_COMMENTS = 10


def _marker(severity: str, confidence: str) -> str:
    mark, _ = _SEVERITY.get(severity, _FALLBACK)
    if severity == "critical" and confidence == "probable":
        return mark + "?"
    return mark


def _rank(comment: dict) -> int:
    _, order = _SEVERITY.get(comment.get("severity"), _FALLBACK)
    return order


def render_comment(comment: dict) -> str:
    marker = _marker(comment.get("severity", ""), comment.get("confidence", ""))
    text = comment.get("text", "").strip()
    body = f"{marker} {text}"
    if comment.get("confidence") == "probable":
        body += "\n\n> 확정되지 않은 지적입니다. 위 내용을 확인한 뒤 판단해 주세요."
    return body


def to_github_comments(review: dict) -> list[dict]:
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


def render_summary(review: dict) -> str:
    counts = {}
    for c in review.get("comments", []):
        sev = c.get("severity", "unknown")
        counts[sev] = counts.get(sev, 0) + 1

    if not counts:
        head = "지적 사항 없음"
    else:
        parts = [
            f"{_SEVERITY.get(s, _FALLBACK)[0]} {counts[s]}"
            for s in ("critical", "warning", "info")
            if s in counts
        ]
        head = " · ".join(parts)

    return f"**{head}**\n\n{review.get('summary_report', '').strip()}"


def _escape_cell(text: str) -> str:
    return text.replace("|", "\\|").replace("\n", " ").strip()


def render_issues_table(review: dict) -> str:
    comments = sorted(review.get("comments", []), key=_rank)
    if not comments:
        return ""

    rows = ["\n### 🔍 발견된 이슈\n",
            "| 파일 | 라인 | 심각도 | 내용 |\n",
            "|------|------|--------|------|\n"]

    for c in comments:
        path = c.get("file") or "-"
        line = c.get("line") if isinstance(c.get("line"), int) else "-"
        marker = _marker(c.get("severity", ""), c.get("confidence", ""))
        rows.append(
            f"| `{_escape_cell(str(path))}` | {line} | {marker} "
            f"| {_escape_cell(c.get('text', ''))} |\n"
        )
    return "".join(rows)


def filter_by_valid_lines(review: dict, valid_lines: dict[str, set[int]]) -> dict:
    kept, dropped = [], []
    for c in review.get("comments", []):
        path, line = c.get("file"), c.get("line")
        if isinstance(line, int) and line in valid_lines.get(path, set()):
            kept.append(c)
        else:
            dropped.append(c)
    return {**review, "comments": kept, "dropped_comments": dropped}
