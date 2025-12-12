// Test program to verify mutability and assignability checks

fn main() {
    // Test 1: Immutable variable assignment (should fail)
    let x = 5;
    x = 10; // Should fail: cannot assign to immutable variable
    
    // Test 2: Mutable variable assignment (should succeed)
    let mut y = 5;
    y = 10; // Should succeed: can assign to mutable variable
    
    // Test 3: Immutable parameter assignment (should fail)
    fn test_immutable_param(a: i32) {
        a = 10; // Should fail: cannot assign to immutable parameter
    }
    
    // Test 4: Mutable parameter assignment (should succeed)
    fn test_mutable_param(mut a: i32) {
        a = 10; // Should succeed: can assign to mutable parameter
    }
    
    // Test 5: Field access on immutable struct (should fail)
    struct Point {
        x: i32,
        y: i32,
    }
    
    let p = Point { x: 1, y: 2 };
    p.x = 5; // Should fail: cannot assign to field of immutable struct
    
    // Test 6: Field access on mutable struct (should succeed)
    let mut p2 = Point { x: 1, y: 2 };
    p2.x = 5; // Should succeed: can assign to field of mutable struct
    
    // Test 7: Index access on immutable array (should fail)
    let arr = [1, 2, 3];
    arr[0] = 5; // Should fail: cannot assign to element of immutable array
    
    // Test 8: Index access on mutable array (should succeed)
    let mut arr2 = [1, 2, 3];
    arr2[0] = 5; // Should succeed: can assign to element of mutable array
    
    // Test 9: Dereference of immutable reference (should fail)
    let a = 5;
    let ref_a = &a;
    *ref_a = 10; // Should fail: cannot assign through immutable reference
    
    // Test 10: Dereference of mutable reference (should succeed)
    let mut b = 5;
    let ref_b = &mut b;
    *ref_b = 10; // Should succeed: can assign through mutable reference
}