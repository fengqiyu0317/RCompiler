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
Comment: misc test, matrix transpose operation
*/

// Matrix transpose operation
// Read a matrix and output its transpose
#[derive(Copy, Clone)]
struct Matrix {
    data: [[i32; 5]; 5],
    rows: i32,
    cols: i32,
}

impl Matrix {
    fn new(r: i32, c: i32) -> Matrix {
        Matrix {
            data: [[0; 5]; 5],
            rows: r,
            cols: c,
        }
    }
    
    fn set(&mut self, row: i32, col: i32, value: i32) {
        self.data[row as usize][col as usize] = value;
    }
    
    fn get(&self, row: i32, col: i32) -> i32 {
        return self.data[row as usize][col as usize];
    }
    
    fn transpose(&self) -> Matrix {
        let mut result: Matrix = Matrix::new(self.cols, self.rows);
        let mut i: i32 = 0;
        while (i < self.rows) {
            let mut j: i32 = 0;
            while (j < self.cols) {
                result.set(j, i, self.get(i, j));
                j += 1;
            }
            i += 1;
        }
        return result;
    }
}

fn main() {
    let rows: i32 = getInt();
    let cols: i32 = getInt();
    let mut matrix: Matrix = Matrix::new(rows, cols);
    
    let mut i: i32 = 0;
    while (i < rows) {
        let mut j: i32 = 0;
        while (j < cols) {
            let value: i32 = getInt();
            matrix.set(i, j, value);
            j += 1;
        }
        i += 1;
    }
    
    // Transpose and output
    let transposed: Matrix = matrix.transpose();
    i = 0;
    while (i < cols) {
        let mut j: i32 = 0;
        while (j < rows) {
            printInt(transposed.get(i, j));
            j += 1;
        }
        i += 1;
    }
    exit(0);
}
