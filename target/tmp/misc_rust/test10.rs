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
Comment: misc test, stack implementation with array
*/

// Stack implementation using array
// Basic stack operations: push, pop, peek, is_empty
#[derive(Copy, Clone)]
struct Stack {
    data: [i32; 50],
    top: i32,
}

impl Stack {
    fn new() -> Stack {
        Stack {
            data: [0; 50],
            top: -1,
        }
    }
    
    fn is_empty(&self) -> bool {
        return self.top == -1;
    }
    
    fn push(&mut self, value: i32) -> bool {
        if (self.top >= 49) {
            return false; // Stack overflow
        }
        self.top += 1;
        self.data[self.top as usize] = value;
        return true;
    }
    
    fn pop(&mut self) -> i32 {
        if (self.is_empty()) {
            return -1; // Stack underflow
        }
        let value: i32 = self.data[self.top as usize];
        self.top -= 1;
        return value;
    }
    
    fn peek(&self) -> i32 {
        if (self.is_empty()) {
            return -1;
        }
        return self.data[self.top as usize];
    }
}

fn main() {
    let mut stack: Stack = Stack::new();
    let n: i32 = getInt();
    
    let mut i: i32 = 0;
    while (i < n) {
        let operation: i32 = getInt();
        
        if (operation == 1) {
            // Push operation
            let value: i32 = getInt();
            stack.push(value);
        } else if (operation == 2) {
            // Pop operation
            let value: i32 = stack.pop();
            printInt(value);
        } else if (operation == 3) {
            // Peek operation
            let value: i32 = stack.peek();
            printInt(value);
        }
        
        i += 1;
    }
    exit(0);
}
