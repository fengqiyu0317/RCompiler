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
Comment: Digital root recursive calculation with iterative pattern search
*/

// Digital root calculation: repeatedly sum digits until single digit
// Used in recreational mathematics and numerology
fn digital_root_recursive(n: i32) -> i32 {
    if (n < 10) {
        return n;
    } else {
        let mut sum: i32 = 0;
        let mut temp: i32 = n;
        
        while (temp > 0) {
            sum += temp % 10;
            temp = temp / 10;
        }
        
        return digital_root_recursive(sum);
    }
}

// Find numbers with specific digital root properties
fn find_special_digital_roots() -> i32 {
    let mut count: i32 = 0;
    let mut num: i32 = 10;
    
    while (num <= 100) {
        let root: i32 = digital_root_recursive(num);
        
        // Count numbers where digital root equals specific patterns
        if (root == 3 || root == 6 || root == 9) {
            count += root;
            
            // Nested loop for additional complexity
            let mut factor: i32 = 1;
            while (factor <= root) {
                if (num % factor == 0) {
                    count += 1;
                }
                factor += 1;
            }
        }
        
        num += 7; // Skip by 7 for interesting pattern
    }
    
    return count;
}

fn main() {
    let result: i32 = find_special_digital_roots();
    printInt(result);
    exit(0);
}
