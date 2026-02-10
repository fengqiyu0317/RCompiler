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
Comment: Hofstadter Q-sequence self-referential recursive computation
*/

// Hofstadter Q-sequence: Q(n) = Q(n-Q(n-1)) + Q(n-Q(n-2)) for n > 2
// Self-referential recursive sequence with chaotic behavior
fn hofstadter_q(n: i32) -> i32 {
    if (n <= 0) {
        return 0;
    } else if (n == 1 || n == 2) {
        return 1;
    } else {
        let q_n_minus_1: i32 = hofstadter_q(n - 1);
        let q_n_minus_2: i32 = hofstadter_q(n - 2);
        return hofstadter_q(n - q_n_minus_1) + hofstadter_q(n - q_n_minus_2);
    }
}

// Calculate sum of Q-sequence values using iteration
fn sum_q_sequence(limit: i32) -> i32 {
    let mut sum: i32 = 0;
    let mut i: i32 = 1;
    
    while (i <= limit) {
        sum += hofstadter_q(i);
        i += 1;
    }
    
    return sum;
}

fn main() {
    let n: i32 = 6;
    let result: i32 = sum_q_sequence(n);
    printInt(result);
    exit(0);
}
