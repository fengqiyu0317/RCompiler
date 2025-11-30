// Test file for operator precedence with ambiguous symbols

// Test cases for * (dereference vs multiplication)
fn test_dereference_and_multiplication() {
    let x = 5;
    let y = &x;
    let z = *y * 2;  // *y is dereference (unary), then * 2 is multiplication (binary)
    println!("{}", z); // Should print 10
    
    let a = *y * *y;  // Both * are dereference (unary), then * is multiplication (binary)
    println!("{}", a); // Should print 25
}

// Test cases for & (borrow vs bitwise AND)
fn test_borrow_and_bitwise_and() {
    let x = 5;
    let y = &x;  // & is borrow (unary)
    let z = x & 3;  // & is bitwise AND (binary)
    println!("{}", z); // Should print 1 (5 & 3 = 1)
    
    let a = *y & 2;  // *y is dereference (unary), then & 2 is bitwise AND (binary)
    println!("{}", a); // Should print 0 (5 & 2 = 0)
}

// Test cases for - (negation vs subtraction)
fn test_negation_and_subtraction() {
    let x = 5;
    let y = -x;  // - is negation (unary)
    let z = x - 2;  // - is subtraction (binary)
    println!("{}", z); // Should print 3
    
    let a = -x - -y;  // First -x is negation (unary), then - is subtraction (binary), then -y is negation (unary)
    println!("{}", a); // Should print -10 (-5 - (-5) = 0, but y is -5, so -5 - 5 = -10)
}

// Complex expression with all ambiguous operators
fn test_complex_expression() {
    let x = 5;
    let y = &x;
    let z = *y * 2 + x & 3 - -x;  // Mix of unary and binary operators
    // Expected evaluation order:
    // 1. *y (dereference) = 5
    // 2. -x (negation) = -5
    // 3. 5 * 2 (multiplication) = 10
    // 4. 10 + 5 (addition) = 15
    // 5. 15 & 3 (bitwise AND) = 3
    // 6. 3 - (-5) (subtraction) = 8
    println!("{}", z); // Should print 8
}

fn main() {
    test_dereference_and_multiplication();
    test_borrow_and_bitwise_and();
    test_negation_and_subtraction();
    test_complex_expression();
}