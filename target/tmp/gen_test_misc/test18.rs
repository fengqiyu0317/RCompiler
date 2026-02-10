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
Comment: Quick Sort implementation
*/

// Quick Sort implementation
// Divide and conquer sorting algorithm
#[derive(Copy, Clone)]
struct QuickSort {
    data: [i32; 1000],
    size: i32,
}

impl QuickSort {
    fn new() -> QuickSort {
        QuickSort {
            data: [0; 1000],
            size: 0,
        }
    }
    
    fn swap(&mut self, i: i32, j: i32) {
        let temp: i32 = self.data[i as usize];
        self.data[i as usize] = self.data[j as usize];
        self.data[j as usize] = temp;
    }
    
    fn partition(&mut self, low: i32, high: i32) -> i32 {
        let pivot: i32 = self.data[high as usize];
        let mut i: i32 = low - 1;
        
        let mut j: i32 = low;
        while (j < high) {
            if (self.data[j as usize] <= pivot) {
                i += 1;
                self.swap(i, j);
            }
            j += 1;
        }
        
        self.swap(i + 1, high);
        return i + 1;
    }
    
    fn quick_sort_recursive(&mut self, low: i32, high: i32) {
        if (low < high) {
            let pivot_index: i32 = self.partition(low, high);
            self.quick_sort_recursive(low, pivot_index - 1);
            self.quick_sort_recursive(pivot_index + 1, high);
        }
    }
    
    fn sort(&mut self) {
        if (self.size <= 1) {
            return;
        }
        self.quick_sort_recursive(0, self.size - 1);
    }
    
    fn insert(&mut self, value: i32) {
        if (self.size < 1000) {
            self.data[self.size as usize] = value;
            self.size += 1;
        }
    }
    
    fn print_array(&self) {
        let mut i: i32 = 0;
        while (i < self.size) {
            printInt(self.data[i as usize]);
            i += 1;
        }
    }
}

fn main() {
    let mut sorter: QuickSort = QuickSort::new();
    let n: i32 = getInt();
    
    let mut i: i32 = 0;
    while (i < n) {
        let value: i32 = getInt();
        sorter.insert(value);
        i += 1;
    }
    
    sorter.sort();
    sorter.print_array();
    exit(0);
}
