#!/usr/bin/env bash
set -euo pipefail

dir="${1:-tests/test_comprehensive}"
if [[ ! -d "$dir" ]]; then
  echo "Dir not found: $dir" >&2
  exit 1
fi

normalize_out() {
  # Extract program output: after Build time, before Exit code.
  awk 'BEGIN{p=0} /Build time/ {p=1; next} /^Exit code:/ {p=0} p {print}' "$1" \
    | sed -e 's/\r$//' -e 's/[[:space:]]\+$//' -e '/^$/d'
}

normalize_ans() {
  sed -e 's/\r$//' -e 's/[[:space:]]\+$//' -e '/^$/d' "$1"
}

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

mismatch=0
newline_only=0
total=0

for out in "$dir"/*.out; do
  [[ -e "$out" ]] || continue
  total=$((total + 1))
  base="${out%.out}"
  ans="$base.ans"
  if [[ ! -f "$ans" ]]; then
    echo "MISSING: $(basename "$ans")"
    mismatch=$((mismatch + 1))
    continue
  fi

  out_norm="$tmpdir/$(basename "$out").norm"
  ans_norm="$tmpdir/$(basename "$ans").norm"
  normalize_out "$out" > "$out_norm"
  normalize_ans "$ans" > "$ans_norm"

  if diff -q "$out_norm" "$ans_norm" >/dev/null; then
    continue
  fi

  out_trim="$out_norm.trim"
  ans_trim="$ans_norm.trim"
  perl -0777 -pe 's/\n\z//' "$out_norm" > "$out_trim"
  perl -0777 -pe 's/\n\z//' "$ans_norm" > "$ans_trim"

  if diff -q "$out_trim" "$ans_trim" >/dev/null; then
    echo "NEWLINE_ONLY: $(basename "$out")"
    newline_only=$((newline_only + 1))
  else
    echo "DIFF: $(basename "$out")"
    mismatch=$((mismatch + 1))
  fi

done

echo "total_out=$total diff=$mismatch newline_only=$newline_only match=$((total - mismatch - newline_only))"
