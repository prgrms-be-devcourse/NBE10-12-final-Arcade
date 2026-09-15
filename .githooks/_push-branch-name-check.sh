#!/bin/sh

SCRIPT_DIR="$(dirname "$0")"

LABELS_FILE="${SCRIPT_DIR}/_labels.txt"

if [ ! -f "$LABELS_FILE" ]; then
  echo "라벨 파일을 찾을 수 없습니다: $LABELS_FILE"
  exit 1
fi

# 현재 체크아웃된 브랜치명을 가져옵니다.
branch_name=$(git symbolic-ref --quiet --short HEAD 2>/dev/null)

# detached HEAD 상태에서는 브랜치명이 없으므로 검사를 건너뜁니다.
if [ -z "$branch_name" ]; then
  exit 0
fi

# main & develop 브랜치 push 금지
if printf '%s\n' "$branch_name" | grep -Eq '^(main|develop)$'; then
  echo "'$branch_name' 브랜치에는 직접 push할 수 없습니다."
  echo "작업 브랜치에서 작업 후 PR로 병합하세요."
  exit 1
fi

LABEL_PATTERN=$(grep -v '^[[:space:]]*$' "$LABELS_FILE" | paste -sd'|' -)

# 작업 브랜치는 라벨/arc-숫자-작업내용 형식만 허용합니다.
if printf '%s\n' "$branch_name" | grep -Eq "^(${LABEL_PATTERN})/arc-[0-9]+-[A-Za-z0-9._가-힣]([A-Za-z0-9._가-힣-]*[A-Za-z0-9._가-힣])?$"; then
  exit 0
fi

cat <<EOF
브랜치명 규칙에 맞지 않습니다: $branch_name

최소 1개의 작업내용이 필요합니다.

허용되는 브랜치명:
  feat/arc-1-signup-login

브랜치명 형식:
  라벨/arc-숫자-작업내용
  라벨/arc-숫자-작업내용-작업내용

사용 가능한 라벨:
  $(grep -v '^[[:space:]]*$' "$LABELS_FILE" | paste -sd, - | sed 's/,/, /g')

사용 가능한 문자:
  영문 대소문자, 숫자, 마침표(.), 밑줄(_), 하이픈(-)
EOF

exit 1
