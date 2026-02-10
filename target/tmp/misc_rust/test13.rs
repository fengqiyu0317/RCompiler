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
Comment: misc test, simple linked list node operations
*/

// Simple linked list node operations
// Basic linked list implementation with array simulation
#[derive(Copy, Clone)]
struct ListNode {
    value: i32,
    next_index: i32,
}

#[derive(Copy, Clone)]
struct SimpleList {
    nodes: [ListNode; 20],
    head: i32,
    size: i32,
    capacity: i32,
}

impl ListNode {
    fn new(val: i32) -> ListNode {
        ListNode {
            value: val,
            next_index: -1,
        }
    }
}

impl SimpleList {
    fn new() -> SimpleList {
        SimpleList {
            nodes: [ListNode::new(0); 20],
            head: -1,
            size: 0,
            capacity: 20,
        }
    }
    
    fn push_front(&mut self, value: i32) -> bool {
        if (self.size >= self.capacity) {
            return false;
        }
        
        let new_index: i32 = self.size;
        self.nodes[new_index as usize] = ListNode::new(value);
        self.nodes[new_index as usize].next_index = self.head;
        self.head = new_index;
        self.size += 1;
        return true;
    }
    
    fn pop_front(&mut self) -> i32 {
        if (self.head == -1) {
            return -1;
        }
        
        let value: i32 = self.nodes[self.head as usize].value;
        self.head = self.nodes[self.head as usize].next_index;
        self.size -= 1;
        return value;
    }
    
    fn get_size(&self) -> i32 {
        return self.size;
    }
}

fn main() {
    let mut list: SimpleList = SimpleList::new();
    let n: i32 = getInt();
    
    let mut i: i32 = 0;
    while (i < n) {
        let operation: i32 = getInt();
        
        if (operation == 1) {
            // Push operation
            let value: i32 = getInt();
            list.push_front(value);
        } else if (operation == 2) {
            // Pop operation
            let value: i32 = list.pop_front();
            printInt(value);
        } else if (operation == 3) {
            // Get size
            let size: i32 = list.get_size();
            printInt(size);
        }
        
        i += 1;
    }
    exit(0);
}
