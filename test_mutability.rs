// Test cases for mutability checking

// Test 1: Basic immutable variable assignment (should fail)
fn test_immutable_assignment() {
    let x = 5;
    x = 10;  // Error: Cannot assign to immutable variable 'x'
}

// Test 2: Basic mutable variable assignment (should pass)
fn test_mutable_assignment() {
    let mut x = 5;
    x = 10;  // OK: x is mutable
}

// Test 3: Mutable borrow of immutable variable (should fail)
fn test_mutable_borrow_of_immutable() {
    let x = 5;
    let y = &mut x;  // Error: Cannot create mutable borrow of immutable variable 'x'
}

// Test 4: Immutable borrow of immutable variable (should pass)
fn test_immutable_borrow_of_immutable() {
    let x = 5;
    let y = &x;  // OK: immutable borrow of immutable variable
}

// Test 5: Mutable borrow of mutable variable (should pass)
fn test_mutable_borrow_of_mutable() {
    let mut x = 5;
    let y = &mut x;  // OK: mutable borrow of mutable variable
}

// Test 6: Multiple mutable borrows (should fail)
fn test_multiple_mutable_borrows() {
    let mut x = 5;
    let y = &mut x;  // OK: first mutable borrow
    let z = &mut x;  // Error: Cannot create multiple mutable borrows of variable 'x'
}

// Test 7: Immutable borrow with existing mutable borrow (should fail)
fn test_immutable_borrow_with_mutable_borrow() {
    let mut x = 5;
    let y = &mut x;  // OK: mutable borrow
    let z = &x;      // Error: Cannot create immutable borrow of variable 'x' when it already has a mutable borrow
}

// Test 8: Mutable borrow with existing immutable borrow (should fail)
fn test_mutable_borrow_with_immutable_borrow() {
    let x = 5;
    let y = &x;      // OK: immutable borrow
    let z = &mut x;  // Error: Cannot create mutable borrow of variable 'x' when it already has active borrows
}

// Test 9: Mutable field access on immutable variable (should fail)
struct Point {
    x: i32,
    y: i32,
}

fn test_mutable_field_access_on_immutable() {
    let p = Point { x: 1, y: 2 };
    p.x = 10;  // Error: Cannot assign to immutable field of immutable variable 'p'
}

// Test 10: Mutable field access on mutable variable (should pass)
fn test_mutable_field_access_on_mutable() {
    let mut p = Point { x: 1, y: 2 };
    p.x = 10;  // OK: p is mutable
}

// Test 11: Mutable parameter mutation (should fail)
fn test_mutable_parameter(x: i32) {
    x = 10;  // Error: Cannot assign to immutable parameter 'x'
}

// Test 12: Mutable parameter mutation (should pass)
fn test_mutable_parameter(mut x: i32) {
    x = 10;  // OK: x is mutable parameter
}

// Test 13: Complex nested mutability
fn test_nested_mutability() {
    let mut x = 5;
    {
        let y = &x;      // OK: immutable borrow
        let z = &mut x;  // Error: Cannot create mutable borrow of variable 'x' when it already has active borrows
    }
}

// Test 14: Array indexing with immutable array (should fail)
fn test_immutable_array_index() {
    let arr = [1, 2, 3, 4, 5];
    arr[0] = 10;  // Error: Cannot assign to immutable array 'arr'
}

// Test 15: Array indexing with mutable array (should pass)
fn test_mutable_array_index() {
    let mut arr = [1, 2, 3, 4, 5];
    arr[0] = 10;  // OK: arr is mutable
}

// Test 16: Dereference of immutable reference (should fail)
fn test_immutable_dereference() {
    let x = 5;
    let y = &x;
    let z = *y;  // OK: dereferencing immutable reference
    // Note: This should actually pass, as dereferencing doesn't change the original value
}

// Test 17: Dereference of mutable reference (should pass)
fn test_mutable_dereference() {
    let mut x = 5;
    let y = &mut x;
    *y = 10;  // OK: dereferencing mutable reference and assigning
}

// Test 18: Pattern matching with mutability
fn test_pattern_matching() {
    let (x, mut y) = (5, 10);
    x = 15;  // Error: Cannot assign to immutable variable 'x'
    y = 20;  // OK: y is mutable
}

// Test 19: Reference pattern matching
fn test_reference_pattern_matching() {
    let mut x = 5;
    let &(ref y, ref mut z) = &(x, x);
    // y is immutable reference to x
    // z is mutable reference to x
    // Both should be OK as they don't change the mutability of x
}

// Test 20: Function with mutable self parameter
struct Counter {
    value: i32,
}

impl Counter {
    fn new() -> Counter {
        Counter { value: 0 }
    }
    
    fn increment(&mut self) {
        self.value += 1;  // OK: self is mutable
    }
    
    fn get_value(&self) -> i32 {
        self.value  // OK: just reading from immutable self
    }
    
    fn reset(&mut self) {
        self.value = 0;  // OK: self is mutable
    }
}

// Test 21: Function with immutable self parameter
impl Counter {
    fn double_value(&self) -> i32 {
        self.value * 2  // OK: just reading from immutable self
    }
    
    // This would fail if added:
    // fn set_value(&self, new_value: i32) {
    //     self.value = new_value;  // Error: Cannot assign to immutable field
    // }
}

fn main() {
    // Test all the functions
    test_immutable_assignment();
    test_mutable_assignment();
    test_mutable_borrow_of_immutable();
    test_immutable_borrow_of_immutable();
    test_mutable_borrow_of_mutable();
    test_multiple_mutable_borrows();
    test_immutable_borrow_with_mutable_borrow();
    test_mutable_borrow_with_immutable_borrow();
    test_mutable_field_access_on_immutable();
    test_mutable_field_access_on_mutable();
    test_mutable_parameter();
    test_mutable_parameter();
    test_nested_mutability();
    test_immutable_array_index();
    test_mutable_array_index();
    test_immutable_dereference();
    test_mutable_dereference();
    test_pattern_matching();
    test_reference_pattern_matching();
    
    let mut counter = Counter::new();
    counter.increment();
    counter.increment();
    let value = counter.get_value();
    counter.reset();
}