// Test file to verify constant type checking before evaluation

const MAX_SIZE: i32 = 10;
const HALF_SIZE: i32 = 5;

fn main() {
    // This should work with our changes - constants are type-checked before evaluation
    let array = [0; MAX_SIZE];
    
    println!("Array created successfully");
}