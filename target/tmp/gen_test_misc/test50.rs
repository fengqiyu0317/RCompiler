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
Comment: Integer partition recursive counting with iterative summation
*/

// Integer partition problem: count ways to write n as sum of positive integers
// Classic number theory problem using recursion with memoization-like approach
fn partition_recursive(n: i32, max_val: i32) -> i32 {
    if (n == 0) {
        return 1; // One way to partition 0 (empty partition)
    } else if (n < 0 || max_val <= 0) {
        return 0; // No valid partitions
    } else {
        // Include max_val in partition or exclude it
        return partition_recursive(n - max_val, max_val) + partition_recursive(n, max_val - 1);
    }
}

// Calculate partition numbers for multiple values
fn calculate_partition_sum(limit: i32) -> i32 {
    let mut total: i32 = 0;
    let mut n: i32 = 1;
    
    while (n <= limit) {
        let partitions: i32 = partition_recursive(n, n);
        total += partitions;
        n += 1;
    }
    
    return total;
}

fn main() {
    let limit: i32 = 5;
    let result: i32 = calculate_partition_sum(limit);
    printInt(result);
    exit(0);
}
