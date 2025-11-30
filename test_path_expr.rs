/*
Test Package: PathExpr
Test Target: Two-segment path expression
Author: Test
Time: 2025-11-28
Verdict: Success
Comment: Test two-segment path expression handling
*/

struct MyStruct {
    field1: i32,
    field2: i32,
}

impl MyStruct {
    fn new() -> MyStruct {
        MyStruct { field1: 0, field2: 0 }
    }
}

fn main() {
    // Test single segment path (should work as before)
    let s = MyStruct::new();
    
    // Test two-segment path (should now use LSeg's symbol only)
    // This is what we're testing - the PathExprNode with two segments
    let _ = MyStruct::new; // This should resolve to MyStruct's symbol, not the new method's symbol
    
    exit(0);
}