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
Comment: misc test, simple calculator with enums
*/

// Simple calculator with basic operations
// Perform arithmetic operations on two numbers
#[derive(PartialEq, Copy, Clone)]
enum Operation {
    Add,
    Subtract,
    Multiply,
    Divide,
    Modulo,
}

fn perform_operation(a: i32, b: i32, op: Operation) -> i32 {
    if (op == Operation::Add) {
        return a + b;
    } else if (op == Operation::Subtract) {
        return a - b;
    } else if (op == Operation::Multiply) {
        return a * b;
    } else if (op == Operation::Divide) {
        if (b != 0) {
            return a / b;
        }
        return 0; // Division by zero
    } else {
        if (b != 0) {
            return a % b;
        }
        return 0; // Modulo by zero
    }
}

fn get_operation(op_code: i32) -> Operation {
    if (op_code == 1) {
        return Operation::Add;
    } else if (op_code == 2) {
        return Operation::Subtract;
    } else if (op_code == 3) {
        return Operation::Multiply;
    } else if (op_code == 4) {
        return Operation::Divide;
    } else {
        return Operation::Modulo;
    }
}

fn main() {
    let a: i32 = getInt();
    let b: i32 = getInt();
    let op_code: i32 = getInt();
    
    let operation: Operation = get_operation(op_code);
    let result: i32 = perform_operation(a, b, operation);
    
    printInt(result);
    exit(0);
}
