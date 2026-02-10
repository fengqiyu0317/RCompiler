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
Comment: Moser circle problem with recursive combinations and iterative analysis
*/

// Moser's circle problem: maximum regions formed by n chords in a circle
// Rare combinatorial problem: M(n) = C(n,4) + C(n,2) + 1
fn combination_recursive(n: i32, r: i32) -> i32 {
    if (r == 0 || (r == n)) {
        return 1;
    } else if (r == 1) {
        return n;
    } else if (r > n) {
        return 0;
    } else {
        return combination_recursive(n - 1, r - 1) + combination_recursive(n - 1, r);
    }
}

// Calculate Moser's circle regions
fn moser_circle_regions(n: i32) -> i32 {
    let c_n_4: i32 = combination_recursive(n, 4);
    let c_n_2: i32 = combination_recursive(n, 2);
    return c_n_4 + c_n_2 + 1;
}

// Sum of Moser numbers for different circle configurations
fn sum_moser_numbers() -> i32 {
    let mut total: i32 = 0;
    let mut n: i32 = 0;
    
    while (n <= 6) {
        let regions: i32 = moser_circle_regions(n);
        total += regions;
        
        // Additional loop for complexity
        let mut multiplier: i32 = 1;
    while (multiplier <= n && multiplier <= 3) {
            total += multiplier * regions;
            multiplier += 1;
        }
        
        n += 1;
    }
    
    return total;
}

fn main() {
    let result: i32 = sum_moser_numbers();
    printInt(result);
    exit(0);
}
