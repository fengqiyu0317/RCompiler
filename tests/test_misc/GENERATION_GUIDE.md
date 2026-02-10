# test_misc dataset generation guide

This guide explains how to generate the `tests/test_misc` dataset, including
input files, summaries, and expected outputs (`.ans`). It mirrors the workflow
we are following in this repo.

## 1) Source and selection

- Source root: `RCompiler-Testcases/semantic-1`
- Subset: `misc*/misc*.rx`
- Include only **Verdict: Success** cases.
- Exclude failed cases (write them to `tests/test_misc/failed.list`).

## 2) Copy and rename

Create `tests/test_misc/` and copy each successful case into sequential names:

- `misc1` → `test1.rx`
- `misc2` → `test2.rx`
- ...

Keep a 1‑based sequence with no gaps.

## 3) Write per-test summaries

For each `tests/test_misc/testN.rx`, add a one‑line purpose summary into:

- `tests/test_misc/NOTES.md`

Format (example):

```
- test12: palindrome check for integer; prints 1 and digit count if palindrome else 0 and -1.
```

## 4) Create input files (`.in`)

Read each `tests/test_misc/testN.rx` and design **valid, meaningful inputs**.
Do not use empty input for cases that call `getInt()`.

Guidelines:
- Ensure indices are in range.
- Avoid division/modulo by zero.
- Keep sizes small to ensure fast execution.
- For graph/DS cases, use small, non‑trivial structures.
- For algorithms with randomness or heavy recursion, choose inputs that finish quickly.

Write inputs to:

- `tests/test_misc/testN.in`

Inputs are whitespace‑separated, with an optional trailing newline.

## 5) Generate expected outputs (`.ans`) via Rust mirror

**Do not derive outputs manually.**

Create a Rust mirror runner that:
1) Provides RCompiler builtin equivalents: `getInt`, `getString`, `printInt`, `printlnInt`, `print`, `println`, `exit`.
2) Reads `testN.in` and runs the mirrored `testN.rx` logic compiled as Rust.
3) Captures stdout and writes it to `testN.ans`.

### Suggested Rust prelude

```rust
use std::io::{self, Read, Write};
use std::process;

static mut INPUT: Option<Vec<String>> = None;
static mut INDEX: usize = 0;

fn init_input() {
    unsafe {
        if INPUT.is_some() { return; }
        let mut s = String::new();
        io::stdin().read_to_string(&mut s).unwrap();
        let tokens: Vec<String> = s.split_whitespace().map(|t| t.to_string()).collect();
        INPUT = Some(tokens);
        INDEX = 0;
    }
}

fn next_token() -> Option<String> {
    init_input();
    unsafe {
        if let Some(ref v) = INPUT {
            if INDEX < v.len() {
                let tok = v[INDEX].clone();
                INDEX += 1;
                return Some(tok);
            }
        }
    }
    None
}

fn getInt() -> i32 {
    match next_token() {
        Some(t) => t.parse::<i32>().unwrap_or(0),
        None => 0,
    }
}

fn getString() -> String {
    match next_token() {
        Some(t) => t,
        None => String::new(),
    }
}

fn printInt(n: i32) { print!("{}", n); }
fn printlnInt(n: i32) { println!("{}", n); }
fn print(s: &str) { print!("{}", s); }
fn println(s: &str) { println!("{}", s); }
fn exit(code: i32) { let _ = io::stdout().flush(); process::exit(code); }
```

### Execution sketch

For each test:
1) Build a temporary Rust file: `prelude + testN.rx contents`.
2) Compile with `rustc`.
3) Run with stdin from `testN.in`.
4) Write stdout to `testN.ans`.

## 6) Optional `.out` logs and comparison

If you also want `.out`:

```
./test_ir.sh tests/test_misc/testN.rx tests/test_misc/testN.in > tests/test_misc/testN.out 2>&1
```

Then compare:

```
mkdir -p target/tmp && TMPDIR=target/tmp tests/compare_outputs.sh
```

## 7) Record excluded cases

Keep `tests/test_misc/failed.list` with one line per excluded case, e.g.:

```
misc56: <reason>
```

---

This guide intentionally enforces **Rust‑based output generation** to keep
results reproducible and independent of RCompiler.
