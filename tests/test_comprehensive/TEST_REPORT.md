# Test Report: tests/test_comprehensive

Date: 2026-02-09
Config: STACK_SIZE=4M, MEMORY_SIZE=256M unless noted.
Normalization: build/interpret time lines ignored; log noise stripped.

## Summary
- Total cases: 50
- Executed: 50
- Passed: 47
- Failed: 3

## Passed (content match after normalization)
1–8, 12–50 (except 9–11)

Notes:
- Output normalization ignores build/interpret time lines and REIMU log noise.

## Failures (details)
- test9: runtime error
  - Error: Store out of bound at 0xfffffff0 (even with STACK_SIZE=64M)

- test10: runtime error (assembler parse failure)
  - Error: Fail to parse source assembly (unexpected token in test.s)

- test11: runtime error
  - Error: Load out of bound at 0x10000650

## Engine/IR fixes applied during this run
- SELF_CONSTRUCTOR handling in IRGenerator:
  - treat self-by-value vs &self correctly in convertSelfType
  - adjust method receiver argument based on callee signature
- test_ir.sh jump normalization:
  - expand out-of-range jumps with PC-relative auipc/jalr
  - keep in-range jumps as `j label`

## Recommendations
1) Investigate runtime OOB in tests 9 and 11 (likely stack/heap addressing or jump expansion correctness).
2) Fix assembler parse failure in test10 (unexpected token in test.s).

