use std::io::{self, Read, Write};
use std::process;

static mut INPUT: Option<Vec<String>> = None;
static mut INDEX: usize = 0;

fn init_input() {
    unsafe {
        if INPUT.is_some() {
            return;
        }
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
Comment: Collatz conjecture recursive implementation with iterative analysis
*/

// Collatz conjecture (3n+1 problem) recursive implementation
// Famous unsolved problem in mathematics combining recursion and iterative tracking
fn collatz_recursive(n: i32, depth: i32) -> i32 {
    if (n == 1) {
        return depth;
    } else if (n % 2 == 0) {
        return collatz_recursive(n / 2, depth + 1);
    } else {
        return collatz_recursive(3 * n + 1, depth + 1);
    }
}

// Find maximum collatz sequence length in a range
fn find_max_collatz_length(start: i32, end: i32) -> i32 {
    let mut max_length: i32 = 0;
    let mut current: i32 = start;
    
    while (current <= end) {
        let length: i32 = collatz_recursive(current, 0);
        if (length > max_length) {
            max_length = length;
        }
        current += 1;
    }
    
    return max_length;
}

fn main() {
    let start: i32 = 1;
    let end: i32 = 10;
    let result: i32 = find_max_collatz_length(start, end);
    printInt(result);
    exit(0);
}
