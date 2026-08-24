#!/bin/sh
echo "Git hooks 설정 중..."

TOPLEVEL=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$TOPLEVEL" ]; then
    echo "설정 실패 — git 저장소가 아닙니다."
    exit 1
fi
cd "$TOPLEVEL" || exit 1

git config core.hooksPath .githooks
chmod +x .githooks/commit-msg .githooks/pre-commit .githooks/pre-push .githooks/*.sh

# hooks path 설정
set_path=$(git config core.hooksPath)
if [ "$set_path" != ".githooks" ]; then
    echo "설정 실패 — hooksPath 현재 값: $set_path"
    exit 1
fi

# 권한 오류
for hook in commit-msg pre-commit pre-push; do
    if [ ! -x ".githooks/$hook" ]; then
        echo "설정 실패 — 실행 권한 없음: .githooks/$hook"
        exit 1
    fi
done

echo "완료 — hooksPath: $set_path"
