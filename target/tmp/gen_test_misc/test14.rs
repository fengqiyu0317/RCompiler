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
Comment: misc test, power calculation with iterative and recursive methods
*/

// Power calculation using iterative method
// Calculate base^exponent efficiently
fn power_iterative(base: i32, mut exp: i32) -> i32 {
    if (exp == 0) {
        return 1;
    }
    
    let mut result: i32 = 1;
    let mut current_base: i32 = base;
    
    while (exp > 0) {
        if (exp % 2 == 1) {
            result *= current_base;
        }
        current_base *= current_base;
        exp /= 2;
    }
    
    return result;
}

fn power_recursive(base: i32, exp: i32) -> i32 {
    if (exp == 0) {
        return 1;
    }
    if (exp == 1) {
        return base;
    }
    
    let half_power: i32 = power_recursive(base, exp / 2);
    
    if (exp % 2 == 0) {
        return half_power * half_power;
    } else {
        return base * half_power * half_power;
    }
}

fn main() {
    let base: i32 = getInt();
    let exponent: i32 = getInt();
    let method: i32 = getInt(); // 1 for iterative, 2 for recursive
    
    if (exponent < 0) {
        printInt(0); // Invalid exponent
        return;
    }
    
    let result: i32 = if (method == 1) {
        power_iterative(base, exponent)
    } else {
        power_recursive(base, exponent)
    };
    
    printInt(result);
    exit(0);
}
