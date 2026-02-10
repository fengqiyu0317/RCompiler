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
Comment: misc test, simple sorting and processing with structs
*/

// Simple sorting and number processing
// Bubble sort implementation with struct methods
#[derive(Copy, Clone)]
struct NumberProcessor {
    value: i32,
}

impl NumberProcessor {
    fn new(val: i32) -> NumberProcessor {
        NumberProcessor { value: val }
    }
    
    fn get_value(&self) -> i32 {
        return self.value;
    }
    
    fn is_greater(&self, other: &NumberProcessor) -> bool {
        return self.value > other.value;
    }
}

fn sort_array(arr: &mut [NumberProcessor; 10], n: usize) {
    let mut i: usize = 0;
    while (i < n) {
        let mut j: usize = 0;
        while (j < n - 1 - i) {
            if (arr[j].is_greater(&arr[j + 1])) {
                // Swap elements
                let temp_val: i32 = arr[j].value;
                arr[j].value = arr[j + 1].value;
                arr[j + 1].value = temp_val;
            }
            j += 1;
        }
        i += 1;
    }
}

fn main() {
    // Read input numbers
    let n: i32 = getInt();
    let mut processors: [NumberProcessor; 10] = [
        NumberProcessor::new(0), NumberProcessor::new(0), NumberProcessor::new(0), 
        NumberProcessor::new(0), NumberProcessor::new(0), NumberProcessor::new(0),
        NumberProcessor::new(0), NumberProcessor::new(0), NumberProcessor::new(0), 
        NumberProcessor::new(0)
    ];
    
    let mut i: i32 = 0;
    while (i < n) {
        let val: i32 = getInt();
        processors[i as usize] = NumberProcessor::new(val);
        i += 1;
    }
    
    // Sort the array
    sort_array(&mut processors, n as usize);
    
    // Output sorted numbers
    i = 0;
    while (i < n) {
        printInt(processors[i as usize].get_value());
        i += 1;
    }
    exit(0);
}
