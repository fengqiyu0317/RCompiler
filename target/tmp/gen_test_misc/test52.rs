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
Comment: Tower of Hanoi recursive solution with iterative move counting
*/

// Tower of Hanoi recursive solution with move counting
// Classic recursive problem from ancient Indian mathematical puzzle
fn hanoi_moves(n: i32) -> i32 {
    if (n == 1) {
        return 1;
    } else {
        return 2 * hanoi_moves(n - 1) + 1;
    }
}

// Simulate multiple towers with different disc counts
fn total_hanoi_moves() -> i32 {
    let mut total_moves: i32 = 0;
    let mut discs: i32 = 1;
    
    while (discs <= 6) {
        let moves: i32 = hanoi_moves(discs);
        total_moves += moves;
        
        // Add complexity with nested calculation
        let mut multiplier: i32 = 1;
        while (multiplier <= discs) {
            if (multiplier % 2 == 0) {
                total_moves += 1;
            }
            multiplier += 1;
        }
        
        discs += 1;
    }
    
    return total_moves;
}

fn main() {
    let result: i32 = total_hanoi_moves();
    printInt(result);
    exit(0);
}
