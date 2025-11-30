// Test file for AmbiguousBlockType
fn main() {
    // Test case 1: Block with if expression without semicolon
    let x = {
        if true {
            42
        } else {
            24
        }
    }; // This should have AmbiguousBlockType(i32 | ())
    
    // Test case 2: Block with loop expression without semicolon
    let y = {
        loop {
            break 10;
        }
    }; // This should have AmbiguousBlockType(i32 | ())
    
    // Test case 3: Block with block expression without semicolon
    let z = {
        { 5 }
    }; // This should have AmbiguousBlockType(i32 | ())
    
    // Test case 4: Block with if expression with semicolon
    let a = {
        if true {
            42
        } else {
            24
        };
    }; // This should have UnitType
    
    println!("Test completed");
}