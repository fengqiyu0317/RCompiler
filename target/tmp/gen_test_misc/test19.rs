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
Comment: Merge Sort implementation
*/

// Merge Sort implementation
// Stable divide-and-conquer sorting algorithm
#[derive(Copy, Clone)]
struct MergeSort {
    data: [i32; 1000],
    temp: [i32; 1000],
    size: i32,
}

impl MergeSort {
    fn new() -> MergeSort {
        MergeSort {
            data: [0; 1000],
            temp: [0; 1000],
            size: 0,
        }
    }
    
    fn merge(&mut self, left: i32, mid: i32, right: i32) {
        let mut i: i32 = left;
        let mut j: i32 = mid + 1;
        let mut k: i32 = left;
        
        // Copy elements to temporary array for merging
        let mut idx: i32 = left;
        while (idx <= right) {
            self.temp[idx as usize] = self.data[idx as usize];
            idx += 1;
        }
        
        // Merge the two halves
        while (i <= mid && j <= right) {
            if (self.temp[i as usize] <= self.temp[j as usize]) {
                self.data[k as usize] = self.temp[i as usize];
                i += 1;
            } else {
                self.data[k as usize] = self.temp[j as usize];
                j += 1;
            }
            k += 1;
        }
        
        // Copy remaining elements from left half
        while (i <= mid) {
            self.data[k as usize] = self.temp[i as usize];
            i += 1;
            k += 1;
        }
        
        // Copy remaining elements from right half
        while (j <= right) {
            self.data[k as usize] = self.temp[j as usize];
            j += 1;
            k += 1;
        }
    }
    
    fn merge_sort_recursive(&mut self, left: i32, right: i32) {
        if (left < right) {
            let mid: i32 = left + (right - left) / 2;
            self.merge_sort_recursive(left, mid);
            self.merge_sort_recursive(mid + 1, right);
            self.merge(left, mid, right);
        }
    }
    
    fn sort(&mut self) {
        if (self.size <= 1) {
            return;
        }
        self.merge_sort_recursive(0, self.size - 1);
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
    let mut sorter: MergeSort = MergeSort::new();
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
