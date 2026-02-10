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
Comment: Catalan numbers recursive computation with iterative verification
*/

// Catalan numbers: C(n) = (2n)! / ((n+1)! * n!) = C(0)*C(n-1) + C(1)*C(n-2) + ... + C(n-1)*C(0)
// Important in combinatorics for counting binary trees, parentheses arrangements
fn catalan_recursive(n: i32) -> i32 {
    if (n <= 1) {
        return 1;
    } else {
        let mut sum: i32 = 0;
        let mut i: i32 = 0;
        
        while (i < n) {
            sum += catalan_recursive(i) * catalan_recursive(n - 1 - i);
            i += 1;
        }
        
        return sum;
    }
}

// Calculate sum of first several Catalan numbers
fn sum_catalan_numbers(limit: i32) -> i32 {
    let mut total: i32 = 0;
    let mut n: i32 = 0;
    
    while (n <= limit) {
        let catalan_n: i32 = catalan_recursive(n);
        total += catalan_n;
        n += 1;
    }
    
    return total;
}

fn main() {
    let limit: i32 = 4;
    let result: i32 = sum_catalan_numbers(limit);
    printInt(result);
    exit(0);
}
