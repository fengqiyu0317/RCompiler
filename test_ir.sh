#!/bin/bash
# test_ir.sh - 测试 IR 代码生成
# 用法: ./test_ir.sh <input.rx> [input_data]

INPUT=$1
INPUT_DATA=$2
BASE_WORKDIR=/tmp/reimu_run
ARCH=${ARCH:-rv32im}
ABI=${ABI:-ilp32}
STACK_SIZE=${STACK_SIZE:-}
MEMORY_SIZE=${MEMORY_SIZE:-}
CLANG_FLAGS="-S -target riscv32 -march=$ARCH -mabi=$ABI -fno-plt -fno-pic -fno-pie -fno-asynchronous-unwind-tables -Wno-override-module"

clean_output() {
    tr -d '\000' | sed -E 's/\x1B\[[0-9;]*[A-Za-z]//g'
}

strip_asm_noise() {
    local file="$1"
    sed -i -E \
        -e '/^[[:space:]]*\\.attribute/d' \
        -e '/^[[:space:]]*\\.file/d' \
        -e '/^[[:space:]]*\\.type/d' \
        -e '/^[[:space:]]*\\.cfi_/d' \
        -e '/^[[:space:]]*\\.size/d' \
        -e '/^[[:space:]]*\\.addrsig/d' \
        -e '/^[[:space:]]*\\.addrsig_sym/d' \
        -e '/^[[:space:]]*\\.ident/d' \
        -e '/^[[:space:]]*\\.unknown/d' \
        "$file"
    sed -i -E 's/^[[:space:]]*\\.section[[:space:]]+\\.rodata.*/\\t.rodata/' "$file"
    sed -i -E '/^[[:space:]]*\\.section[[:space:]]+"?\\.note\\.GNU-stack"?/d' "$file"
}

normalize_long_jumps() {
    local file="$1"
    python3 - <<'PY' "$file"
import re
import sys

path = sys.argv[1]
lines = open(path, "r", encoding="utf-8", errors="ignore").read().splitlines()

expanded = []
label_re = re.compile(r'^\s*([A-Za-z0-9_.$]+):\s*$')
jump_re = re.compile(r'^(\s*)jump\s+([^,\s]+)\s*,\s*([A-Za-z0-9]+)\s*$')
jump_simple_re = re.compile(r'^(\s*)jump\s+([^,\s]+)\s*$')
j_re = re.compile(r'^(\s*)j\s+([^,\s]+)\s*$')

for line in lines:
    m = jump_re.match(line)
    if m:
        indent, label, reg = m.group(1), m.group(2), m.group(3)
        expanded.append(f"{indent}__LONGJUMP_AUIPC__ {reg} {label}")
        expanded.append(f"{indent}__LONGJUMP_JALR__ {reg} {label}")
        continue
    m = jump_simple_re.match(line)
    if m:
        indent, label = m.group(1), m.group(2)
        expanded.append(f"{indent}__LONGJUMP_AUIPC__ t0 {label}")
        expanded.append(f"{indent}__LONGJUMP_JALR__ t0 {label}")
        continue
    m = j_re.match(line)
    if m:
        indent, label = m.group(1), m.group(2)
        expanded.append(f"{indent}__LONGJUMP_AUIPC__ t0 {label}")
        expanded.append(f"{indent}__LONGJUMP_JALR__ t0 {label}")
        continue
    expanded.append(line)

# Pass 1: label addresses (track text section + directive sizes)
addr = 0
label_addr = {}
instr_addrs = []
in_text = False

def align(addr, n):
    if n <= 0:
        return addr
    return (addr + n - 1) // n * n

def count_operands(s):
    return len([x for x in s.split(',') if x.strip() != ""])

num_re = re.compile(r"(-?\d+)")

for line in expanded:
    stripped = line.strip()
    if stripped.startswith(".text") or re.match(r"^\.section\s+\.text\b", stripped):
        in_text = True
        instr_addrs.append(None)
        continue
    if re.match(r"^\.section\s+\.(rodata|data|bss|sdata|sbss)\b", stripped):
        in_text = False
        instr_addrs.append(None)
        continue
    m = label_re.match(line)
    if m:
        if in_text:
            label_addr[m.group(1)] = addr
        instr_addrs.append(None)
        continue
    if not stripped:
        instr_addrs.append(None)
        continue
    if stripped.startswith('.'):
        if in_text:
            if stripped.startswith(".p2align"):
                m = num_re.search(stripped)
                if m:
                    addr = align(addr, 1 << int(m.group(1)))
            elif stripped.startswith(".align") or stripped.startswith(".balign"):
                m = num_re.search(stripped)
                if m:
                    addr = align(addr, int(m.group(1)))
            elif stripped.startswith(".zero") or stripped.startswith(".space"):
                m = num_re.search(stripped)
                if m:
                    addr += int(m.group(1))
            elif stripped.startswith(".byte"):
                addr += count_operands(stripped[len(".byte"):]) * 1
            elif stripped.startswith(".2byte") or stripped.startswith(".half"):
                addr += count_operands(stripped.split(None, 1)[1] if ' ' in stripped else "") * 2
            elif stripped.startswith(".4byte") or stripped.startswith(".word") or stripped.startswith(".long"):
                addr += count_operands(stripped.split(None, 1)[1] if ' ' in stripped else "") * 4
            elif stripped.startswith(".8byte") or stripped.startswith(".quad") or stripped.startswith(".dword"):
                addr += count_operands(stripped.split(None, 1)[1] if ' ' in stripped else "") * 8
        instr_addrs.append(None)
        continue
    if in_text:
        instr_addrs.append(addr)
        addr += 4
    else:
        instr_addrs.append(None)

# Pass 2: emit final lines
out = []
pending_jalr = None
for line, cur_addr in zip(expanded, instr_addrs):
    if cur_addr is None:
        out.append(line)
        continue
    if "__LONGJUMP_AUIPC__" in line:
        _, reg, label = line.strip().split()
        if label not in label_addr:
            raise SystemExit(f"Unknown label in long jump: {label}")
        offset = label_addr[label] - cur_addr
        if abs(offset) <= 1048575:
            out.append(f"\tj\t{label}")
            pending_jalr = "__SKIP__"
            continue
        imm20 = (offset + 0x800) >> 12
        imm12 = offset - (imm20 << 12)
        imm20_enc = imm20 & 0xFFFFF
        out.append(f"\tauipc\t{reg}, {imm20_enc}")
        pending_jalr = (reg, imm12)
        continue
    if "__LONGJUMP_JALR__" in line:
        if pending_jalr == "__SKIP__":
            pending_jalr = None
            continue
        if pending_jalr is None:
            raise SystemExit("JALR placeholder without AUIPC")
        reg, imm12 = pending_jalr
        out.append(f"\tjalr\tx0, {imm12}({reg})")
        pending_jalr = None
        continue
    out.append(line)

open(path, "w", encoding="utf-8").write("\n".join(out) + "\n")
PY
}

needs_rebuild() {
    # If we don't have cached outputs, rebuild.
    if [ ! -f "$STAMP_FILE" ] || [ ! -f "$TEST_S" ] || [ ! -f "$BUILTIN_S" ] || [ ! -f "$OUTPUT_LL" ]; then
        return 0
    fi
    # Input program changed.
    if [ "$INPUT" -nt "$STAMP_FILE" ]; then
        return 0
    fi
    # Compiler or builtin changed.
    if [ "builtin.ll" -nt "$STAMP_FILE" ]; then
        return 0
    fi
    if find src/main/java -name "*.java" -newer "$STAMP_FILE" -print -quit | grep -q .; then
        return 0
    fi
    if [ -d src/main/resources ] && find src/main/resources -type f -newer "$STAMP_FILE" -print -quit | grep -q .; then
        return 0
    fi
    return 1
}

COMPILER_STAMP=target/classes/.compiler_build.stamp

needs_compiler_build() {
    # If compiled classes are missing, we must build.
    if [ ! -d target/classes ]; then
        return 0
    fi
    # If we don't have a build stamp, assume we need to build.
    if [ ! -f "$COMPILER_STAMP" ]; then
        return 0
    fi
    # If any compiler source/resource is newer than the build stamp, rebuild.
    if find src/main/java -name "*.java" -newer "$COMPILER_STAMP" -print -quit | grep -q .; then
        return 0
    fi
    if [ -d src/main/resources ] && find src/main/resources -type f -newer "$COMPILER_STAMP" -print -quit | grep -q .; then
        return 0
    fi
    return 1
}
if [ -z "$INPUT" ]; then
    echo "用法: ./test_ir.sh <input.rx> [input_data]"
    echo "  input.rx   - Rust 源代码文件"
    echo "  input_data - 可选，程序运行时的输入数据文件"
    exit 1
fi

if [ ! -f "$INPUT" ]; then
    echo "错误: 文件 '$INPUT' 不存在"
    exit 1
fi

INPUT_KEY="$(python3 - <<'PY' "$INPUT"
import hashlib
import os
import sys

path = os.path.realpath(sys.argv[1])
print(hashlib.sha1(path.encode("utf-8")).hexdigest())
PY
)"
WORKDIR="$BASE_WORKDIR/$INPUT_KEY"
OUTPUT_LL="$WORKDIR/output.ll"
DECLS_LL="$WORKDIR/builtin_decls.ll"
TEST_LL="$WORKDIR/test_with_decls.ll"
BUILTIN_LL="$WORKDIR/builtin_filtered.ll"
TEST_S="$WORKDIR/test.s"
BUILTIN_S="$WORKDIR/builtin.s"
STAMP_FILE="$WORKDIR/.ir_build.stamp"

mkdir -p "$WORKDIR"
if [ -n "$NO_CACHE" ]; then
    echo "=== 已设置 NO_CACHE，强制重新生成 ==="
    rm -f "$STAMP_FILE"
fi
if needs_rebuild; then
    echo "=== 检测到改动，重新编译/生成 ==="
    if needs_compiler_build; then
        echo "=== 编译 RCompiler ==="
        make build
        if [ $? -ne 0 ]; then
            echo "错误: 编译失败"
            exit 1
        fi
        touch "$COMPILER_STAMP"
    else
        echo "=== 编译器未改动，跳过编译，仅重新生成 ==="
    fi

    echo ""
    echo "=== 生成 IR ==="
    cat "$INPUT" | make run > "$OUTPUT_LL" 2>/dev/null
    COMPILER_EXIT=$?

    if [ $COMPILER_EXIT -ne 0 ]; then
        echo "错误: IR 生成失败 (exit code: $COMPILER_EXIT)"
        echo "错误信息:"
        cat "$INPUT" | make run 2>&1 >/dev/null
        exit 1
    fi

    echo "生成的 IR: 已写入 ir_result.out"
    cat "$OUTPUT_LL" > ir_result.out

    echo ""
    echo "=== 用 clang 生成 RISC-V 汇编 ==="
    # 为 test.s 生成 builtin 函数声明，避免未声明报错
    {
        grep -E '^declare ' builtin.ll
        grep -E '^define ' builtin.ll | sed -E 's/^define /declare /; s/[{].*$//'
    } > "$DECLS_LL"
    cat "$DECLS_LL" "$OUTPUT_LL" > "$TEST_LL"

    clang $CLANG_FLAGS "$TEST_LL" -o "$TEST_S" 2>&1
    if [ $? -ne 0 ]; then
        echo "错误: clang 生成 test.s 失败"
        exit 1
    fi
    sed -i -E 's/@plt//g' "$TEST_S"
    strip_asm_noise "$TEST_S"
    normalize_long_jumps "$TEST_S"
    # 过滤掉包含 :: 的 builtin 定义，避免 REIMU 无法解析带引号的符号名
    awk '
      BEGIN { skip=0 }
      /^define / {
        if ($0 ~ /@"[^"]*::[^"]*"/) { skip=1; next }
      }
      skip {
        if ($0 ~ /^}/) { skip=0 }
        next
      }
      { print }
    ' builtin.ll > "$BUILTIN_LL"

    clang $CLANG_FLAGS "$BUILTIN_LL" -o "$BUILTIN_S" 2>&1
    if [ $? -ne 0 ]; then
        echo "错误: clang 生成 builtin.s 失败"
        exit 1
    fi
    sed -i -E 's/@plt//g' "$BUILTIN_S"
    strip_asm_noise "$BUILTIN_S"
    normalize_long_jumps "$BUILTIN_S"
    cat >> "$BUILTIN_S" <<'EOF'

.text
.p2align 2
.globl exit
exit:
  li ra, 4
  ret
EOF
    echo "生成成功: $TEST_S, $BUILTIN_S"
    touch "$STAMP_FILE"
else
    echo "=== 未检测到改动，跳过编译/生成，复用上次产物 ==="
    if [ -f "$OUTPUT_LL" ]; then
        cat "$OUTPUT_LL" > ir_result.out
    fi
fi

echo ""
echo "=== 运行 REIMU ==="
if [ -n "$INPUT_DATA" ] && [ -f "$INPUT_DATA" ]; then
    echo "(使用输入文件: $INPUT_DATA)"
    cp "$INPUT_DATA" "$WORKDIR/test.in"
    if [ -n "$STACK_SIZE" ] || [ -n "$MEMORY_SIZE" ]; then
        REIMU_OUTPUT="$((cd "$WORKDIR" && reimu ${STACK_SIZE:+-s "$STACK_SIZE"} ${MEMORY_SIZE:+-m "$MEMORY_SIZE"} -i test.in) 2>&1)"
    else
        REIMU_OUTPUT="$((cd "$WORKDIR" && reimu -i test.in) 2>&1)"
    fi
else
    if [ -n "$STACK_SIZE" ] || [ -n "$MEMORY_SIZE" ]; then
        REIMU_OUTPUT="$((cd "$WORKDIR" && reimu ${STACK_SIZE:+-s "$STACK_SIZE"} ${MEMORY_SIZE:+-m "$MEMORY_SIZE"}) 2>&1)"
    else
        REIMU_OUTPUT="$((cd "$WORKDIR" && reimu) 2>&1)"
    fi
fi
PROG_EXIT=$?
if [ -n "$REIMU_OUTPUT" ]; then
    printf "%s\n" "$REIMU_OUTPUT" | clean_output
fi

echo ""
echo "=== 结果 ==="
echo "程序 exit code: $PROG_EXIT"
