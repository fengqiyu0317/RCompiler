# Testing guide (tests/test_basic)

This folder contains runnable `.rx` programs plus expected output files.

- `tests/test_basic/*.rx`: test inputs
- `tests/test_basic/*.ans`: expected outputs (what the program should print)
- `tests/test_basic/*.out`: captured run logs (generated)

## Prerequisites

- `clang` and `reimu` are available in `PATH`
- Build tools are installed (`make`, `javac`)
- Default runtime parameters for all tests in this repo:
  - `STACK_SIZE=64M`
  - `MEMORY_SIZE=256M`

## Run a single test (tests/test_basic)

From repo root:

```bash
STACK_SIZE=64M MEMORY_SIZE=256M ./test_ir.sh tests/test_basic/test16.rx tests/test_basic/test16.in > tests/test_basic/test16.out 2>&1
```

Check expected output:

```bash
cat tests/test_basic/test16.ans
```

## Run all tests in tests/test_basic

From repo root:

```bash
for f in tests/test_basic/*.rx; do
  base="$(basename "${f%.rx}")"
  STACK_SIZE=64M MEMORY_SIZE=256M ./test_ir.sh "$f" "tests/test_basic/$base.in" > "tests/test_basic/$base.out" 2>&1
done
```

## Run a single test (tests/test_misc)

From repo root:

```bash
STACK_SIZE=64M MEMORY_SIZE=256M ./test_ir.sh tests/test_misc/test16.rx tests/test_misc/test16.in > tests/test_misc/test16.out 2>&1
```

## Run all tests in tests/test_misc

From repo root:

```bash
for f in tests/test_misc/*.rx; do
  base="$(basename "${f%.rx}")"
  STACK_SIZE=64M MEMORY_SIZE=256M ./test_ir.sh "$f" "tests/test_misc/$base.in" > "tests/test_misc/$base.out" 2>&1
done
```

## Compare outputs vs expected

The comparator normalizes log noise and compares `.out` to `.ans`.

```bash
tests/compare_outputs.sh
```

If everything matches, the script prints `[OK]` lines and exits 0.
