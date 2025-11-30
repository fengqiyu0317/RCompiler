// Test cases for constant expression evaluation

// Test 1: Basic literals
const I: i32 = 42;
const U: u32 = 42u32;
const USIZE: usize = 42usize;
const ISIZE: isize = 42isize;
const B: bool = true;
const C: char = 'a';
const S: &str = "hello";

// Test 2: Arithmetic expressions
const ADD: i32 = 2 + 3;
const SUB: i32 = 10 - 3;
const MUL: i32 = 5 * 6;
const DIV: i32 = 20 / 4;
const MOD: i32 = 15 % 4;
const BIT_AND: i32 = 0b1100 & 0b1010;
const BIT_OR: i32 = 0b1100 | 0b1010;
const BIT_XOR: i32 = 0b1100 ^ 0b1010;
const SHL: i32 = 1 << 4;
const SHR: i32 = 32 >> 2;

// Test 3: Comparison expressions
const EQ: bool = 5 == 5;
const NEQ: bool = 5 != 3;
const GT: bool = 10 > 5;
const LT: bool = 5 < 10;
const GE: bool = 10 >= 5;
const LE: bool = 5 <= 10;

// Test 4: Logical expressions
const AND: bool = true && false;
const OR: bool = true || false;
const NOT: bool = !true;

// Test 5: Type casting
const CAST_U32: u32 = 42 as u32;
const CAST_USIZE: usize = 42 as usize;
const CAST_ISIZE: isize = 42 as isize;

// Test 6: Grouped expressions
const GROUP1: i32 = (2 + 3) * 4;
const GROUP2: bool = (5 > 3) && (2 < 10);

// Test 7: Array sizes
const ARR1: [i32; 5];
const ARR2: [i32; 2 + 3];
const ARR3: [i32; 4 * 2];
const ARR4: [i32; (1 + 2) * 3];
const ARR5: [u8; 256];
const ARR6: [char; 5];

// Test 8: Complex expressions
const COMPLEX1: i32 = (1 << 8) + 0xFF;
const COMPLEX2: u32 = 0xFF00 | (1 << 8);
const COMPLEX3: i32 = ((BASE + OFFSET) * SCALE) & MASK;

// Test 9: Constant references
const BASE: i32 = 100;
const OFFSET: i32 = 20;
const SCALE: i32 = 2;
const MASK: i32 = 0xFF;
const DERIVED: i32 = (BASE + OFFSET) * SCALE;

// Test 10: Array with constant size
const SIZED_ARRAY: [i32; 1024];
const LARGE_ARRAY: [u8; 65536];

fn main() {
    // Test array access
    let arr1: [i32; 5] = [1, 2, 3, 4, 5];
    let arr2: [i32; 2 + 3] = [1, 2, 3, 4, 5];
    
    // Test constant usage
    println!("I = {}", I);
    println!("U = {}", U);
    println!("USIZE = {}", USIZE);
    println!("ISIZE = {}", ISIZE);
    println!("B = {}", B);
    println!("C = {}", C);
    println!("S = {}", S);
    
    println!("ADD = {}", ADD);
    println!("SUB = {}", SUB);
    println!("MUL = {}", MUL);
    println!("DIV = {}", DIV);
    println!("MOD = {}", MOD);
    println!("BIT_AND = {}", BIT_AND);
    println!("BIT_OR = {}", BIT_OR);
    println!("BIT_XOR = {}", BIT_XOR);
    println!("SHL = {}", SHL);
    println!("SHR = {}", SHR);
    
    println!("EQ = {}", EQ);
    println!("NEQ = {}", NEQ);
    println!("GT = {}", GT);
    println!("LT = {}", LT);
    println!("GE = {}", GE);
    println!("LE = {}", LE);
    
    println!("AND = {}", AND);
    println!("OR = {}", OR);
    println!("NOT = {}", NOT);
    
    println!("CAST_U32 = {}", CAST_U32);
    println!("CAST_USIZE = {}", CAST_USIZE);
    println!("CAST_ISIZE = {}", CAST_ISIZE);
    
    println!("GROUP1 = {}", GROUP1);
    println!("GROUP2 = {}", GROUP2);
    
    println!("ARR1[0] = {}", arr1[0]);
    println!("ARR2[0] = {}", arr2[0]);
    
    println!("COMPLEX1 = {}", COMPLEX1);
    println!("COMPLEX2 = {}", COMPLEX2);
    println!("COMPLEX3 = {}", COMPLEX3);
    
    println!("BASE = {}", BASE);
    println!("OFFSET = {}", OFFSET);
    println!("SCALE = {}", SCALE);
    println!("MASK = {}", MASK);
    println!("DERIVED = {}", DERIVED);
}