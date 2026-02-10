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
Comment: misc test, palindrome number checker
*/

// Palindrome number checker
// Check if a number reads the same forwards and backwards
fn reverse_number(mut n: i32) -> i32 {
    let mut reversed: i32 = 0;
    
    while (n > 0) {
        reversed = reversed * 10 + n % 10;
        n /= 10;
    }
    
    return reversed;
}

fn is_palindrome(n: i32) -> bool {
    if (n < 0) {
        return false;
    }
    
    let reversed: i32 = reverse_number(n);
    return n == reversed;
}

fn count_digits(mut n: i32) -> i32 {
    if (n == 0) {
        return 1;
    }
    
    let mut count: i32 = 0;
    while (n > 0) {
        count += 1;
        n /= 10;
    }
    
    return count;
}

fn main() {
    let n: i32 = getInt();
    
    if (is_palindrome(n)) {
        let digit_count: i32 = count_digits(n);
        printInt(1); // Is palindrome
        printInt(digit_count);
    } else {
        printInt(0); // Not palindrome
        printInt(-1);
    }
    exit(0);
}
