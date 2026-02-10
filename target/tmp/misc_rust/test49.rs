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
Comment: Josephus problem recursive solution with iterative variations
*/

// Josephus problem: find the survivor position in a circle elimination game
// Classic recursion problem with historical significance from ancient Rome
fn josephus_recursive(n: i32, k: i32) -> i32 {
    if (n == 1) {
        return 0; // Base case: only one person left at position 0
    } else {
        return (josephus_recursive(n - 1, k) + k) % n;
    }
}

// Multiple Josephus calculations with different parameters
fn josephus_variations() -> i32 {
    let mut total: i32 = 0;
    let mut n: i32 = 3;
    
    while (n <= 7) {
        let mut k: i32 = 2;
        while (k <= 4) {
            let survivor_pos: i32 = josephus_recursive(n, k);
            total += survivor_pos + 1; // Convert to 1-indexed position
            k += 1;
        }
        n += 1;
    }
    
    return total;
}

fn main() {
    let result: i32 = josephus_variations();
    printInt(result);
    exit(0);
}
