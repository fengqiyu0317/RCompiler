# test_if dataset test report

Date: 2026-02-05

## Commands used

Generate .out logs (per-test timeout):

```
for f in tests/test_if/*.rx; do base="$(basename "${f%.rx}")"; ./test_ir.sh "$f" > "tests/test_if/$base.out" 2>&1; done
```

Compare outputs:

```
TMPDIR=target/tmp tests/compare_outputs.sh
```

Note: The compare script scans all test suites under tests/.

## Per-test execution status

The per-test timing/exit codes were recorded in `target/tmp/test_if_status.txt`.
Final run results:

- test1: rc=0, time=19s
- test2: rc=0, time=26s
- test3: rc=0, time=20s
- test4: rc=1, time=25s
- test5: rc=0, time=24s
- test6: rc=0, time=28s
- test7: rc=0, time=21s
- test8: rc=0, time=25s
- test9: rc=0, time=24s
- test10: rc=0, time=19s

## compare_outputs.sh summary

- All existing suites in tests/ reported [OK].
- `tests/test_if/test4` reported [DIFF].

## Failure details (test4)

`tests/test_if/test4.out` indicates IR generation failed due to type checking:

```
Type checking errors:
  Left side of assignment is not assignable
  Type of expression node is not set yet.
  Type of expression node is not set yet.
  Type of expression node is not set yet.
make: *** [Makefile:63: run] Error 1
```

This matches the [DIFF] because the output file contains the compiler error log instead of the expected output in `tests/test_if/test4.ans`.

## Artifacts

- Outputs: `tests/test_if/test1.out` ... `tests/test_if/test10.out`
- Expected outputs: `tests/test_if/test1.ans` ... `tests/test_if/test10.ans`
- Status log: `target/tmp/test_if_status.txt`
