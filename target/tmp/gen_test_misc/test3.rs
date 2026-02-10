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
Comment: misc test, Fibonacci sequence calculation using struct
*/

// Fibonacci sequence calculation using struct
// Calculate the nth Fibonacci number
#[derive(Copy, Clone)]
struct FibState {
    prev: i32,
    curr: i32,
}

impl FibState {
    fn new() -> FibState {
        FibState { prev: 0, curr: 1 }
    }
    
    fn next(&mut self) {
        let temp: i32 = self.curr;
        self.curr = self.prev + self.curr;
        self.prev = temp;
    }
}

fn main() {
    let n: i32 = getInt();
    
    if (n <= 0) {
        printInt(0);
        return;
    }
    
    if (n == 1) {
        printInt(1);
        return;
    }
    
    let mut fib: FibState = FibState::new();
    let mut i: i32 = 2;
    
    while (i <= n) {
        fib.next();
        i += 1;
    }
    
    printInt(fib.curr);
    exit(0);
}
