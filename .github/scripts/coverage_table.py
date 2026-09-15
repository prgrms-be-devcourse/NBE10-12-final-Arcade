import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

THRESHOLD = {"LINE": 80, "BRANCH": 70}
DOMAIN = "com.back.domain."
GLOBAL = "com.back.global."


def add(into: dict, node) -> None:
    """jacoco 노드의 counter를 into에 누적한다. {타입: [덮음, 전체]}"""
    for c in node.findall("counter"):
        missed, covered = int(c.get("missed")), int(c.get("covered"))
        acc = into.setdefault(c.get("type"), [0, 0])
        acc[0] += covered
        acc[1] += covered + missed


def group_of(name: str):
    """패키지명 → (어느 표, 표에 쓸 이름). 도메인은 하위 도메인까지, 글로벌은 모듈 단위로 묶는다."""
    if name.startswith(DOMAIN):
        return "domain", ".".join(name[len(DOMAIN):].split(".")[:2])
    if name.startswith(GLOBAL):
        return "global", name[len(GLOBAL):].split(".")[0]
    if name == "com.back":
        return "global", "(루트)"
    return "global", name.removeprefix("com.back.")


def cell(pair, threshold=None) -> str:
    """[덮음, 전체] → 표 칸. 셀 대상이 없으면 n/a."""
    if not pair or pair[1] == 0:
        return "n/a"
    covered, total = pair
    percent = round(covered * 100 / total)
    mark = "" if threshold is None else (" ✅" if percent >= threshold else " ⚠️")
    return f"{percent}%{mark} ({covered}/{total})"


def row(name: str, c: dict) -> str:
    return (
        f"| {name} "
        f"| {cell(c.get('LINE'), THRESHOLD['LINE'])} "
        f"| {cell(c.get('BRANCH'), THRESHOLD['BRANCH'])} "
        f"| {cell(c.get('CLASS'))} |"
    )


def totals(groups: dict) -> dict:
    """그룹 묶음 → 합산 counter."""
    total = {}
    for c in groups.values():
        for kind, (covered, whole) in c.items():
            acc = total.setdefault(kind, [0, 0])
            acc[0] += covered
            acc[1] += whole
    return total


def table(title: str, groups: dict, collapsed=False) -> str:
    if not groups:
        return ""

    total = totals(groups)

    # 라인 커버리지가 낮은 쪽을 위에 둔다. 보강할 곳을 먼저 보여주는 것이 목적이다.
    def rate(item):
        line = item[1].get("LINE", [0, 0])
        return line[0] / line[1] if line[1] else 1.0

    body = [
        "| 영역 | 라인 | 브랜치 | 클래스 |",
        "|------|------|--------|--------|",
        row("**합계**", total),
    ]
    body += [row(f"`{name}`", c) for name, c in sorted(groups.items(), key=rate)]

    if not collapsed:
        return "\n".join([f"### {title}", ""] + body)

    # details 안에서 표가 렌더링되려면 summary 뒤에 빈 줄이 있어야 한다.
    line = total.get("LINE", [0, 0])
    percent = round(line[0] * 100 / line[1]) if line[1] else 0
    return "\n".join(
        ["<details>", f"<summary>{title} — 라인 {percent}%</summary>", ""]
        + body
        + ["", "</details>"]
    )


def main():
    root = ET.parse(sys.argv[1]).getroot()

    grouped = {"domain": defaultdict(dict), "global": defaultdict(dict)}
    for package in root.findall("package"):
        where, name = group_of(package.get("name").replace("/", "."))
        add(grouped[where][name], package)

    print(table("도메인", grouped["domain"]))
    print()
    print(table("글로벌", grouped["global"], collapsed=True))


if __name__ == "__main__":
    main()
