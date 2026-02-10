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
Comment: misc test, simple hash table simulation
*/

// Simple hash table simulation using arrays
// Basic hash table with linear probing for collision resolution
#[derive(Copy, Clone)]
struct HashEntry {
    key: i32,
    value: i32,
    is_occupied: bool,
}

#[derive(Copy, Clone)]
struct HashTable {
    entries: [HashEntry; 13], // Prime size for better distribution
    size: i32,
}

impl HashEntry {
    fn new() -> HashEntry {
        HashEntry {
            key: 0,
            value: 0,
            is_occupied: false,
        }
    }
}

impl HashTable {
    fn new() -> HashTable {
        HashTable {
            entries: [HashEntry::new(); 13],
            size: 13,
        }
    }
    
    fn hash_function(&self, key: i32) -> i32 {
        let mut hash_key: i32 = key;
        if (hash_key < 0) {
            hash_key = -hash_key;
        }
        return hash_key % self.size;
    }
    
    fn insert(&mut self, key: i32, value: i32) -> bool {
        let mut index: i32 = self.hash_function(key);
        let original_index: i32 = index;
        
        loop {
            if (!self.entries[index as usize].is_occupied) {
                self.entries[index as usize].key = key;
                self.entries[index as usize].value = value;
                self.entries[index as usize].is_occupied = true;
                return true;
            }
            
            if (self.entries[index as usize].key == key) {
                // Update existing key
                self.entries[index as usize].value = value;
                return true;
            }
            
            index = (index + 1) % self.size;
            if (index == original_index) {
                return false; // Table full
            }
        }
    }
    
    fn get(&self, key: i32) -> i32 {
        let mut index: i32 = self.hash_function(key);
        let original_index: i32 = index;
        
        loop {
            if (!self.entries[index as usize].is_occupied) {
                return -1; // Not found
            }
            
            if (self.entries[index as usize].key == key) {
                return self.entries[index as usize].value;
            }
            
            index = (index + 1) % self.size;
            if (index == original_index) {
                return -1; // Not found
            }
        }
    }
}

fn main() {
    let mut hash_table: HashTable = HashTable::new();
    let n: i32 = getInt();
    
    let mut i: i32 = 0;
    while (i < n) {
        let operation: i32 = getInt();
        
        if (operation == 1) {
            // Insert operation
            let key: i32 = getInt();
            let value: i32 = getInt();
            hash_table.insert(key, value);
        } else if (operation == 2) {
            // Get operation
            let key: i32 = getInt();
            let result: i32 = hash_table.get(key);
            printInt(result);
        }
        
        i += 1;
    }
    exit(0);
}
