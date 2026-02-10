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
Comment: KMP string matching algorithm
*/

// KMP (Knuth-Morris-Pratt) string matching algorithm
// Efficient pattern matching with preprocessing
#[derive(Copy, Clone)]
struct KMPMatcher {
    pattern: [i32; 100],
    text: [i32; 1000],
    lps: [i32; 100], // Longest Proper Prefix which is also Suffix
    pattern_length: i32,
    text_length: i32,
}

impl KMPMatcher {
    fn new() -> KMPMatcher {
        KMPMatcher {
            pattern: [0; 100],
            text: [0; 1000],
            lps: [0; 100],
            pattern_length: 0,
            text_length: 0,
        }
    }
    
    fn set_pattern(&mut self, length: i32) {
        self.pattern_length = length;
        let mut i: i32 = 0;
        while (i < length) {
            self.pattern[i as usize] = getInt();
            i += 1;
        }
        self.compute_lps();
    }
    
    fn set_text(&mut self, length: i32) {
        self.text_length = length;
        let mut i: i32 = 0;
        while (i < length) {
            self.text[i as usize] = getInt();
            i += 1;
        }
    }
    
    fn compute_lps(&mut self) {
        let mut length: i32 = 0;
        let mut i: i32 = 1;
        self.lps[0] = 0;
        
        while (i < self.pattern_length) {
            if (self.pattern[i as usize] == self.pattern[length as usize]) {
                length += 1;
                self.lps[i as usize] = length;
                i += 1;
            } else {
                if (length != 0) {
                    length = self.lps[(length - 1) as usize];
                } else {
                    self.lps[i as usize] = 0;
                    i += 1;
                }
            }
        }
    }
    
    fn search(&self) -> i32 {
        let mut i: i32 = 0; // index for text
        let mut j: i32 = 0; // index for pattern
        let mut matches: i32 = 0;
        
        while (i < self.text_length) {
            if (self.pattern[j as usize] == self.text[i as usize]) {
                i += 1;
                j += 1;
            }
            
            if (j == self.pattern_length) {
                matches += 1;
                j = self.lps[(j - 1) as usize];
            } else if (i < self.text_length && self.pattern[j as usize] != self.text[i as usize]) {
                if (j != 0) {
                    j = self.lps[(j - 1) as usize];
                } else {
                    i += 1;
                }
            }
        }
        
        return matches;
    }
    
    fn find_first_occurrence(&self) -> i32 {
        let mut i: i32 = 0; // index for text
        let mut j: i32 = 0; // index for pattern
        
        while (i < self.text_length) {
            if (self.pattern[j as usize] == self.text[i as usize]) {
                i += 1;
                j += 1;
            }
            
            if (j == self.pattern_length) {
                return i - j;
            } else if (i < self.text_length && self.pattern[j as usize] != self.text[i as usize]) {
                if (j != 0) {
                    j = self.lps[(j - 1) as usize];
                } else {
                    i += 1;
                }
            }
        }
        
        return -1; // Pattern not found
    }
}

fn main() {
    let mut matcher: KMPMatcher = KMPMatcher::new();
    
    let pattern_length: i32 = getInt();
    matcher.set_pattern(pattern_length);
    
    let text_length: i32 = getInt();
    matcher.set_text(text_length);
    
    let first_occurrence: i32 = matcher.find_first_occurrence();
    printInt(first_occurrence);
    
    let total_matches: i32 = matcher.search();
    printInt(total_matches);
    exit(0);
}
