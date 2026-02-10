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
Comment: Continued fraction expansion for square roots and Pell equation solver
*/

// Continued fraction expansion for square roots
// Recursive computation of convergents and periodic patterns
struct Convergent {
    numerator: i32,
    denominator: i32,
}

fn gcd(mut a: i32, mut b: i32) -> i32 {
    while (b != 0) {
        let temp: i32 = b;
        b = a % b;
        a = temp;
    }
    return a;
}

fn continued_fraction_sqrt_recursive(n: i32, m: i32, d: i32, a0: i32, 
                                   convergents: &mut [Convergent; 50], 
                                   depth: usize) -> i32 {
    if (depth >= 48) {
        return depth as i32;
    }
    
    let m_new: i32 = d * convergents[depth as usize].denominator - m;
    let d_new: i32 = (n - m_new * m_new) / d;
    
    if (d_new == 0) {
        return depth as i32;
    }
    
    let a_new: i32 = (a0 + m_new) / d_new;
    
    // Check for period
    let mut period_start: usize = 0;
    while (period_start < depth) {
        if (convergents[period_start].numerator == a_new && 
           convergents[period_start].denominator == d_new) {
            return (depth - period_start) as i32;
        }
        period_start = period_start + 1;
    }
    
    // Calculate new convergent
    if (depth == 0) {
        convergents[depth + 1].numerator = a_new * a0 + 1;
        convergents[depth + 1].denominator = a_new;
    } else {
        convergents[depth + 1].numerator = a_new * convergents[depth].numerator + 
                                         convergents[depth - 1].numerator;
        convergents[depth + 1].denominator = a_new * convergents[depth].denominator + 
                                           convergents[depth - 1].denominator;
    }
    
    return continued_fraction_sqrt_recursive(n, m_new, d_new, a0, convergents, depth + 1);
}

fn sqrt_continued_fraction(n: i32) -> i32 {
    // Check if n is perfect square
    let mut i: i32 = 1;
    while (i * i <= n) {
        if (i * i == n) {
            return 0; // Period is 0 for perfect squares
        }
        i = i + 1;
    }
    
    let a0: i32 = i - 1; // floor(sqrt(n))
    let mut convergents: [Convergent; 50] = [Convergent { numerator: 0, denominator: 1 }; 50];
    
    convergents[0].numerator = a0;
    convergents[0].denominator = 1;
    
    let period: i32 = continued_fraction_sqrt_recursive(n, 0, 1, a0, &mut convergents, 0);
    
    return period;
}

fn pell_equation_solver(n: i32) -> i32 {
    let period: i32 = sqrt_continued_fraction(n);
    
    if (period == 0) {
        return -1; // No solution for perfect squares
    }
    
    // Use continued fraction to find fundamental solution
    let mut p_prev: i32 = 1;
    let mut p_curr: i32 = 1;
    let mut q_prev: i32 = 0;
    let mut q_curr: i32 = 1;
    
    let mut step: i32 = 0;
    while (step < period) {
        let temp_p: i32 = p_curr;
        let temp_q: i32 = q_curr;
        
        p_curr = p_curr + p_prev;
        q_curr = q_curr + q_prev;
        
        p_prev = temp_p;
        q_prev = temp_q;
        
        step = step + 1;
    }
    
    return p_curr;
}

fn main() {
    let n: i32 = getInt();
    let result: i32 = pell_equation_solver(n);
    printInt(result);
    exit(0);
}
