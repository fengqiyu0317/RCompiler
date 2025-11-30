// Test case for function return type checking

// This should pass - return type matches body type
fn test_valid_function() -> i32 {
    42
}

// This should fail - return type i32 but body returns bool
fn test_invalid_function() -> i32 {
    true
}

// This should pass - no explicit return type, body returns unit
fn test_unit_function() {
    let x = 5;
}

// This should fail - no explicit return type but body returns i32
fn test_implicit_return_mismatch() {
    42
}

fn main() {
    let valid_result = test_valid_function();
    // let invalid_result = test_invalid_function(); // This should cause a type error
    test_unit_function();
    // test_implicit_return_mismatch(); // This should cause a type error
}