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
        Some(t) => t.parse::<i32>().unwrap_or(1),
        None => 1,
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
Comment: misc test, GCD and LCM calculation
*/

// GCD and LCM calculation
// Calculate greatest common divisor and least common multiple
fn gcd(a: i32, b: i32) -> i32 {
    if (b == 0) {
        return a;
    }
    return gcd(b, a % b);
}

fn lcm(a: i32, b: i32) -> i32 {
    return (a * b) / gcd(a, b);
}

fn absolute(x: i32) -> i32 {
    if (x < 0) {
        return -x;
    }
    return x;
}

fn main() {
    let a: i32 = getInt();
    let b: i32 = getInt();
    
    let abs_a: i32 = absolute(a);
    let abs_b: i32 = absolute(b);
    
    let gcd_result: i32 = gcd(abs_a, abs_b);
    let lcm_result: i32 = lcm(abs_a, abs_b);
    
    printInt(gcd_result);
    printInt(lcm_result);
    exit(0);
}
