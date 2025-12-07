fn main() {
    // Test case 1: Block with semicolon at the end
    let x = {
        let y = 5;
        y + 1
    };
    
    // Test case 2: Block without semicolon at the end
    let z = {
        let a = 10;
        a * 2
    };  // This semicolon is for the let statement, not the block
    
    // Test case 3: Empty block
    let empty = {};
    
    // Test case 4: Block with expression and semicolon
    let with_semicolon = {
        42
    };
    
    // Test case 5: Block with expression without semicolon
    let without_semicolon = {
        42
    };
}