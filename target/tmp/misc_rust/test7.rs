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
Comment: misc test, binary search algorithm
*/

// Binary search algorithm
// Search for a target value in a sorted array
fn binary_search(arr: &[i32; 20], target: i32, size: i32) -> i32 {
    let mut left: i32 = 0;
    let mut right: i32 = size - 1;
    
    while (left <= right) {
        let mid: i32 = left + (right - left) / 2;
        
        if (arr[mid as usize] == target) {
            return mid;
        }
        
        if (arr[mid as usize] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return -1; // Not found
}

fn main() {
    let n: i32 = getInt();
    let target: i32 = getInt();
    
    let mut arr: [i32; 20] = [0; 20];
    let mut i: i32 = 0;
    
    // Read sorted array
    while (i < n) {
        arr[i as usize] = getInt();
        i += 1;
    }
    
    let result: i32 = binary_search(&arr, target, n);
    printInt(result);
    exit(0);
}
