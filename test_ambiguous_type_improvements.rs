// Test file for AmbiguousBlockType improvements
// This file tests various scenarios where AmbiguousBlockType is used
// and how it interacts with type merging and conversion

fn test_ambiguous_block_in_if() {
    // Test: AmbiguousBlockType in if expression
    // The block { if true { 42 } } should be able to resolve to i32
    let result = if true { 
        if true { 42 } 
    } else { 
        0 
    };
    
    // This should work without type errors
    let x: i32 = result;
}

fn test_ambiguous_block_in_loop() {
    // Test: AmbiguousBlockType in loop expression
    // The loop { break 42; } should be able to resolve to i32
    let result = loop {
        if true { break 42; }
    };
    
    // This should work without type errors
    let x: i32 = result;
}

fn test_ambiguous_block_type_merging() {
    // Test: Type merging with AmbiguousBlockType
    // Both branches return AmbiguousBlockType that should merge to i32
    let result = if true {
        if true { 42 }
    } else {
        if false { 24 }
    };
    
    // This should work without type errors
    let x: i32 = result;
}

fn test_ambiguous_block_with_unit() {
    // Test: AmbiguousBlockType resolving to unit type
    // The block { if true { () } } should resolve to unit type
    let result = if true {
        if true { () }
    };
    
    // This should work without type errors
    let x: () = result;
}

fn test_ambiguous_block_in_function_return() {
    // Test: AmbiguousBlockType in function return
    // The function should return AmbiguousBlockType that resolves to i32
    fn get_value() -> i32 {
        if true { 42 }
    }
    
    // This should work without type errors
    let x: i32 = get_value();
}

fn test_ambiguous_block_in_struct_field() {
    // Test: AmbiguousBlockType in struct field initialization
    struct TestStruct {
        value: i32,
    }
    
    // The block { if true { 42 } } should resolve to i32
    let instance = TestStruct {
        value: if true { 42 }
    };
    
    // This should work without type errors
    let x: i32 = instance.value;
}

fn test_nested_ambiguous_blocks() {
    // Test: Nested AmbiguousBlockType
    // Multiple levels of ambiguous blocks should resolve correctly
    let result = if true {
        if true {
            if false { 42 } else { 24 }
        }
    } else {
        0
    };
    
    // This should work without type errors
    let x: i32 = result;
}

fn main() {
    test_ambiguous_block_in_if();
    test_ambiguous_block_in_loop();
    test_ambiguous_block_type_merging();
    test_ambiguous_block_with_unit();
    test_ambiguous_block_in_function_return();
    test_ambiguous_block_in_struct_field();
    test_nested_ambiguous_blocks();
    
    println!("All tests passed!");
}