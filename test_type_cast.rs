fn main() {
    // Valid integer casts
    let a: i32 = 42;
    let b: u32 = a as u32;
    let c: isize = b as isize;
    let d: usize = c as usize;
    
    // Invalid cast - should fail
    let e: bool = true;
    let f: i32 = e as i32; // This should fail type checking
    
    // Valid cast - same type
    let g: bool = e as bool;
    
    // Invalid cast - different non-integer types
    let h: char = 'x';
    let i: bool = h as bool; // This should fail type checking
}