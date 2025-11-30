// Test file for return and break expressions

// Test 1: Function with return in if expression
fn test_if_return() -> i32 {
    let x: i32 = if (2 + 2 == 4) {
        return 1;
    } else {
        return 2;
    };
    3  // This line should be unreachable
}

// Test 2: Function with return value
fn test_return_value() -> i32 {
    return 42;
}

// Test 3: Function with unit return
fn test_return_unit() {
    return;
}

// Test 4: Loop with break value
fn test_loop_break() -> i32 {
    let result = loop {
        if (true) {
            break 42;
        }
    };
    result
}

// Test 5: Loop with unit break
fn test_loop_break_unit() {
    loop {
        if (true) {
            break;
        }
    }
}

// Test 6: Nested if with return
fn test_nested_if_return() -> i32 {
    if (true) {
        if (false) {
            return 1;
        } else {
            return 2;
        }
    } else {
        return 3;
    }
}

// Test 7: If expression without return in all branches
fn test_if_no_return() -> i32 {
    let x: i32 = if (true) {
        1
    } else {
        2
    };
    x
}