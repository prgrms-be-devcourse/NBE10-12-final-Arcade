import sys
import xml.etree.ElementTree as ET

THRESHOLD = {"line": 80, "branch": 70}


def counters(node) -> dict:
    """jacoco 노드 → {타입: (덮음, 전체)}. 해당 counter가 없으면 키도 없다."""
    out = {}
    for c in node.findall("counter"):
        missed, covered = int(c.get("missed")), int(c.get("covered"))
        out[c.get("type")] = (covered, covered + missed)
    return out


def cell(pair, threshold=None) -> str:
    """(덮음, 전체) → 표 칸. 셀 대상이 없으면 n/a."""
    if pair is None or pair[1] == 0:
        return "n/a"
    covered, total = pair
    percent = round(covered * 100 / total)
    mark = "" if threshold is None else (" ✅" if percent >= threshold else " ⚠️")
    return f"{percent}%{mark} ({covered}/{total})"


def row(name, c) -> str:
    return (
        f"| {name} "
        f"| {cell(c.get('LINE'), THRESHOLD['line'])} "
        f"| {cell(c.get('BRANCH'), THRESHOLD['branch'])} "
        f"| {cell(c.get('CLASS'))} |"
    )


def main():
    root = ET.parse(sys.argv[1]).getroot()

    lines = [
        "| 패키지 | 라인 | 브랜치 | 클래스 |",
        "|--------|------|--------|--------|",
        row("**전체**", counters(root)),
    ]

    # 라인 커버리지가 낮은 패키지를 위로 둔다. 보강할 곳을 먼저 보여주는 것이 목적이다.
    packages = []
    for p in root.findall("package"):
        c = counters(p)
        line = c.get("LINE", (0, 0))
        rate = line[0] / line[1] if line[1] else 1.0
        packages.append((rate, p.get("name").replace("/", "."), c))

    for _, name, c in sorted(packages):
        lines.append(row(f"`{name}`", c))

    print("\n".join(lines))


if __name__ == "__main__":
    main()
