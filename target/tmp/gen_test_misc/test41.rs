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
Comment: Baby-step Giant-step algorithm for discrete logarithm computation
*/

// Baby-step Giant-step algorithm for discrete logarithm
// Finds x such that g^x ≡ h (mod p) using time-space tradeoff
#[derive(Copy, Clone)]
struct HashEntry {
    key: i32,
    value: i32,
    used: bool,
}

fn power_mod(mut base: i32, mut exp: i32, modulus: i32) -> i32 {
    let mut result: i32 = 1;
    base = base % modulus;
    
    while (exp > 0) {
        if (exp % 2 == 1) {
            result = (result * base) % modulus;
        }
        exp = exp / 2;
        base = (base * base) % modulus;
    }
    
    return result;
}

fn hash_insert(table: &mut [HashEntry; 100], key: i32, value: i32, size: i32) {
    let mut index: i32 = key % size;
    
    while (table[index as usize].used) {
        if (table[index as usize].key == key) {
            return; // Already exists
        }
        index = (index + 1) % size;
    }
    
    table[index as usize].key = key;
    table[index as usize].value = value;
    table[index as usize].used = true;
}

fn hash_lookup(table: &[HashEntry; 100], key: i32, size: i32) -> i32 {
    let mut index: i32 = key % size;
    
    while (table[index as usize].used) {
        if (table[index as usize].key == key) {
            return table[index as usize].value;
        }
        index = (index + 1) % size;
    }
    
    return -1; // Not found
}

fn isqrt(n: i32) -> i32 {
    if (n < 2) {
        return n;
    }
    
    let mut x: i32 = n;
    let mut y: i32 = (x + 1) / 2;
    
    while (y < x) {
        x = y;
        y = (x + n / x) / 2;
    }
    
    return x;
}

fn baby_step_giant_step(g: i32, h: i32, p: i32) -> i32 {
    let m: i32 = isqrt(p) + 1;
    let mut baby_steps: [HashEntry; 100] = [HashEntry { key: 0, value: 0, used: false }; 100];
    
    // Baby steps: compute g^j mod p for j = 0, 1, ..., m-1
    let mut gamma: i32 = 1;
    let mut j: i32 = 0;
    
    while (j < m && j < 100) {
        hash_insert(&mut baby_steps, gamma, j, 100);
        gamma = (gamma * g) % p;
        j = j + 1;
    }
    
    // Giant steps: compute h * (g^(-m))^i mod p for i = 0, 1, ..., m-1
    let g_inv_m: i32 = power_mod(g, p - 1 - m, p); // g^(-m) mod p using Fermat's little theorem
    let mut y: i32 = h;
    let mut i: i32 = 0;
    
    while (i < m && i < 100) {
        let lookup_result: i32 = hash_lookup(&baby_steps, y, 100);
        if (lookup_result != -1) {
            return i * m + lookup_result;
        }
        y = (y * g_inv_m) % p;
        i = i + 1;
    }
    
    return -1; // No solution found
}

fn discrete_log_recursive(g: i32, h: i32, p: i32, depth: i32) -> i32 {
    if (depth > 3) {
        return baby_step_giant_step(g, h, p);
    }
    
    // Try small values first
    let mut x: i32 = 1;
    let mut g_power: i32 = g;
    
    while (x < 10) {
        if (g_power == h) {
            return x;
        }
        g_power = (g_power * g) % p;
        x = x + 1;
    }
    
    return discrete_log_recursive(g, h, p, depth + 1);
}

fn main() {
    let g: i32 = getInt();
    let h: i32 = getInt();
    let p: i32 = getInt();
    
    let result: i32 = discrete_log_recursive(g, h, p, 0);
    printInt(result);
    exit(0);
}
