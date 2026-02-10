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
Comment: Min Heap implementation using array
*/

// Min Heap implementation using array
// Complete binary tree stored in array format
#[derive(Copy, Clone)]
struct MinHeap {
    data: [i32; 1000],
    size: i32,
    capacity: i32,
}

impl MinHeap {
    fn new() -> MinHeap {
        MinHeap {
            data: [0; 1000],
            size: 0,
            capacity: 1000,
        }
    }
    
    fn parent_index(&self, index: i32) -> i32 {
        return (index - 1) / 2;
    }
    
    fn left_child_index(&self, index: i32) -> i32 {
        return 2 * index + 1;
    }
    
    fn right_child_index(&self, index: i32) -> i32 {
        return 2 * index + 2;
    }
    
    fn swap(&mut self, i: i32, j: i32) {
        let temp: i32 = self.data[i as usize];
        self.data[i as usize] = self.data[j as usize];
        self.data[j as usize] = temp;
    }
    
    fn heapify_up(&mut self, index: i32) {
        let mut current: i32 = index;
        while (current > 0) {
            let parent: i32 = self.parent_index(current);
            if (self.data[current as usize] >= self.data[parent as usize]) {
                break;
            }
            self.swap(current, parent);
            current = parent;
        }
    }
    
    fn heapify_down(&mut self, index: i32) {
        let mut current: i32 = index;
        loop {
            let left: i32 = self.left_child_index(current);
            let right: i32 = self.right_child_index(current);
            let mut smallest: i32 = current;
            
            if (left < self.size && self.data[left as usize] < self.data[smallest as usize]) {
                smallest = left;
            }
            
            if (right < self.size && self.data[right as usize] < self.data[smallest as usize]) {
                smallest = right;
            }
            
            if (smallest == current) {
                break;
            }
            
            self.swap(current, smallest);
            current = smallest;
        }
    }
    
    fn insert(&mut self, value: i32) -> bool {
        if (self.size >= self.capacity) {
            return false;
        }
        
        self.data[self.size as usize] = value;
        self.heapify_up(self.size);
        self.size += 1;
        return true;
    }
    
    fn extract_min(&mut self) -> i32 {
        if (self.size == 0) {
            return -1;
        }
        
        let min_value: i32 = self.data[0];
        self.size -= 1;
        self.data[0] = self.data[self.size as usize];
        self.heapify_down(0);
        return min_value;
    }
    
    fn peek(&self) -> i32 {
        if (self.size == 0) {
            return -1;
        }
        return self.data[0];
    }
    
    fn is_empty(&self) -> bool {
        return self.size == 0;
    }
}

fn main() {
    let mut heap: MinHeap = MinHeap::new();
    let n: i32 = getInt();
    
    let mut i: i32 = 0;
    while (i < n) {
        let operation: i32 = getInt();
        
        if (operation == 1) {
            let value: i32 = getInt();
            heap.insert(value);
        } else if (operation == 2) {
            let min_val: i32 = heap.extract_min();
            printInt(min_val);
        } else if (operation == 3) {
            let min_val: i32 = heap.peek();
            printInt(min_val);
        }
        
        i += 1;
    }
    exit(0);
}
