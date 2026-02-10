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
Comment: Pollard's rho algorithm for integer factorization with recursive optimization
*/

// Pollard's rho algorithm for integer factorization
// Uses Floyd's cycle detection to find non-trivial factors
fn gcd(mut a: i32, mut b: i32) -> i32 {
    while (b != 0) {
        let temp: i32 = b;
        b = a % b;
        a = temp;
    }
    return a;
}

fn f(x: i32, n: i32) -> i32 {
    return (x * x + 1) % n;
}

fn pollard_rho(n: i32) -> i32 {
    if (n % 2 == 0) {
        return 2;
    }
    
    let mut x: i32 = 2;
    let mut y: i32 = 2;
    let mut d: i32 = 1;
    
    while (d == 1) {
        x = f(x, n);
        y = f(f(y, n), n);
        
    let diff: i32 = if (x > y) { x - y } else { y - x };
        d = gcd(diff, n);
    }
    
    return d;
}

fn find_factor_recursive(n: i32, depth: i32) -> i32 {
    if (depth > 10) {
        return n;
    }
    
    let factor: i32 = pollard_rho(n);
    if (factor == n) {
        return n;
    }
    
    let remaining: i32 = n / factor;
    return find_factor_recursive(remaining, depth + 1);
}

fn main() {
    let n: i32 = getInt();
    let factor: i32 = find_factor_recursive(n, 0);
    printInt(factor);
    exit(0);
}
