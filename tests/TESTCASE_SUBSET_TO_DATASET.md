# How to turn a subset of TestCases into a tests/ dataset

This guide describes how to build a test dataset from a subset of
`RCompiler-Testcases` (e.g. `semantic-1/expr*`) and place it under `tests/`.
It follows the exact workflow we used, but is written as a reusable recipe.

## Step 1: Pick the subset

Decide the subset of cases to import, for example:

- Source root: `RCompiler-Testcases/semantic-1`
- Subset pattern: `expr*`
- Files: `RCompiler-Testcases/semantic-1/expr*/expr*.rx`

## Step 2: Filter to “can compile”

Only include cases that are expected to compile. Two practical options:

1) **Metadata filter**: keep files who can be successfully compiled.
2) **Trial compile** (optional): run `./test_ir.sh <file>` and keep those
   that succeed.

Use whichever matches your constraint (no-runtime vs. runtime-verified).

## Step 3: Decide dataset naming

Choose a target folder under `tests/`, for example:

- `tests/test_expr`

Rename incoming files to match existing test naming conventions:

- `expr17.rx` → `test1.rx`
- `expr19.rx` → `test2.rx`
- …

(Keep a simple sequential mapping.)

## Step 4: Ensure outputs exist in the program

Each test must print its some important results using builtin output functions
(e.g. `printInt` / `printlnInt` / `println`).

If a test has no output, **add proper output** (print some important variables)
so that the expected results can be defined.

## Step 5: Create expected output (`.ans`)

Use a **Rust implementation** to mirror the algorithm and compute expected
outputs (do not use RCompiler itself). **Do not derive outputs manually.**
Always run the Rust mirror program to generate the `.ans` results so the
process is reproducible.

Store outputs as plain text, one line per `print*` call, ending with `\n`.

Example (minimal Rust mirror):

```rust
fn main() {
    // Mirror the algorithm from testN.rx.
    let mut acc = 0i32;
    for i in 0..5 {
        acc += i * 2;
    }
    // Expected prints: one line per print* call.
    println!("{acc}");
}
```

## Step 6: Copy sources and write `.ans`

Create the target dataset directory and copy files:

- `tests/test_expr/testN.rx`
- `tests/test_expr/testN.ans`

If you want to track excluded cases, write a short note into:

- `tests/test_expr/failed.list`

## Step 7: (Optional) Generate `.out`

If you choose to also store run logs:

```
./test_ir.sh tests/test_expr/testN.rx > tests/test_expr/testN.out 2>&1
```

This is optional. The core dataset uses only `.rx` and `.ans`.

## Step 8: (Optional) Compare outputs

After `.out` files exist, compare via:

```
mkdir -p target/tmp && TMPDIR=target/tmp tests/compare_outputs.sh
```

This will normalize logs and validate `.out` vs `.ans`.

---

## Example mapping (expr subset)

When using `semantic-1/expr*`, a minimal valid set might be:

- `expr17` → `test1`
- `expr19` → `test2`
- `expr34` → `test3`
- `expr36` → `test4`
- `expr38` → `test5`

(Adjust to your needs.)
