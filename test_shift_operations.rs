// Test file for shift operations
fn main() {
    // Valid shift operations
    let a: i32 = 5;
    let b: u32 = 2;
    let c = a << b;  // Valid: left shift with i32 and u32
    let d = a >> b;  // Valid: right shift with i32 and u32
    
    // Valid shift compound assignments
    let mut e: i32 = 10;
    e <<= b;  // Valid: left shift compound assignment
    e >>= b;  // Valid: right shift compound assignment
    
    // Test with different integer types
    let f: isize = 20;
    let g: usize = 3;
    let h = f << g;  // Valid: left shift with isize and usize
    let i = f >> g;  // Valid: right shift with isize and usize
    
    // Test with unsigned left operand
    let j: u32 = 40;
    let k: u32 = 1;
    let l = j << k;  // Valid: left shift with u32 and u32
    let m = j >> k;  // Valid: right shift with u32 and u32
    
    // Test with signed right operand (now allowed)
    let r: i32 = 1;
    let s = a << r;  // Valid: left shift with i32 and i32
    let t = a >> r;  // Valid: right shift with i32 and i32
    
    // These should cause type errors if uncommented:
    // let p = 3.14 << b;  // Error: left operand must be integer
    // let q = a << 2.5;  // Error: right operand must be integer
    
    println!("Shift operations test completed");
}