// Test cases for integer overflow detection

fn main() {
    // Test 1: i32 overflow
    let a: i32 = 2147483647; // Max i32 value
    let b: i32 = 1;
    let c = a + b; // Should detect overflow
    
    // Test 2: u32 overflow
    let d: u32 = 4294967295; // Max u32 value
    let e: u32 = 1;
    let f = d + e; // Should detect overflow
    
    // Test 3: i32 underflow
    let g: i32 = -2147483648; // Min i32 value
    let h: i32 = 1;
    let i = g - h; // Should detect underflow
    
    // Test 4: Multiplication overflow
    let j: i32 = 100000;
    let k: i32 = 100000;
    let l = j * k; // Should detect overflow
    
    // Test 5: Division by zero
    let m: i32 = 10;
    let n: i32 = 0;
    let o = m / n; // Should detect division by zero
    
    // Test 6: Modulo by zero
    let p: i32 = 10;
    let q: i32 = 0;
    let r = p % q; // Should detect modulo by zero
    
    // Test 7: Edge case - Long.MIN_VALUE / -1
    let s: i64 = -9223372036854775808; // Min i64 value
    let t: i64 = -1;
    let u = s / t; // Should detect overflow
    
    // Test 8: Valid operations (should not report errors)
    let v: i32 = 100;
    let w: i32 = 200;
    let x = v + w; // Valid operation
    
    // Test 9: Mixed types with int (undetermined)
    let y = 2147483647; // int type
    let z: i32 = 1;
    let aa = y + z; // Should detect overflow after type inference
    
    // Test 10: Large literal that doesn't fit in i32
    let bb = 2147483648; // int type with value > i32 max
    let cc: i32 = bb; // Should detect overflow during assignment
}