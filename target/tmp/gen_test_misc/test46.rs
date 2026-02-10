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

/*
Test Package: Semantic-1
Test Target: misc
Author: Wenxin Zheng
Time: 2025-08-08
Verdict: Success
Comment: Ackermann function implementation using pure recursion
*/

// Ackermann function implementation using pure recursion
// One of the fastest-growing recursive functions in computability theory
fn ackermann(m: i32, n: i32) -> i32 {
    if (m == 0) {
        return n + 1;
    } else if (n == 0) {
        return ackermann(m - 1, 1);
    } else {
        return ackermann(m - 1, ackermann(m, n - 1));
    }
}

// Iterative wrapper for safety with small inputs
fn safe_ackermann(m: i32, n: i32) -> i32 {
    let mut counter: i32 = 0;
    while (counter < 5) { // Safety limit for demonstration
        if (counter == 0 && m <= 3 && n <= 3) {
            return ackermann(m, n);
        }
        counter += 1;
    }
    return -1; // Error case
}

fn main() {
    let m: i32 = 2;
    let n: i32 = 3;
    let result: i32 = safe_ackermann(m, n);
    printInt(result);
    exit(0);
}
