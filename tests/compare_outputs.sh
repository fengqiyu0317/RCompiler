#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
FAIL=0

normalize_output() {
  tr -d '\000' | sed -E \
    -e 's/\x1B\[[0-9;]*[A-Za-z]//g' \
    -e '/^=== /d' \
    -e '/^生成的 IR/d' \
    -e '/^用 clang /d' \
    -e '/^生成成功:/d' \
    -e '/^错误:/d' \
    -e '/^==============================/d' \
    -e '/^=+ Interpret time:/d' \
    -e '/^程序 exit code:/d' \
    -e '/^Exit code:/d' \
    -e '/^Total cycles:/d' \
    -e '/^Instruction parsed:/d' \
    -e '/^\\(使用输入文件: /d' \
    -e '/^# /d' \
    -e '/^Warning/d' \
    -e '/^\.\/test_ir\.sh: line [0-9]+: warning: command substitution: ignored null byte in input$/d' \
    -e '/^Instruction counts:/d' \
    -e '/^[[:space:]]*$/d'
}

compare_dir() {
  local dir="$1"
  local found=0

  for ans in "$dir"/*.ans; do
    if [ ! -f "$ans" ]; then
      continue
    fi
    found=1
    local base
    base="$(basename "$ans" .ans)"
    local out="$dir/$base.out"

    if [ ! -f "$out" ]; then
      echo "[MISS] $dir/$base.out"
      FAIL=1
      continue
    fi

    local ans_norm out_norm
    ans_norm="$(mktemp)"
    out_norm="$(mktemp)"
    normalize_output < "$ans" > "$ans_norm"
    normalize_output < "$out" > "$out_norm"

    if diff -u "$ans_norm" "$out_norm" >/dev/null 2>&1; then
      echo "[OK]   $dir/$base"
    else
      echo "[DIFF] $dir/$base"
      diff -u "$ans_norm" "$out_norm" || true
      FAIL=1
    fi
    rm -f "$ans_norm" "$out_norm"
  done

  if [ $found -eq 0 ]; then
    echo "[WARN] no .ans files in $dir"
  fi
}

for d in "$ROOT_DIR"/test_*; do
  if [ -d "$d" ]; then
    compare_dir "$d"
  fi
done

if [ $FAIL -ne 0 ]; then
  exit 1
fi
