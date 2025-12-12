// Test file for Rust borrowing rules implementation

// Test 1: Basic immutable borrowing
fn test_immutable_borrow() {
    let x = 5;
    let y = &x; // OK: immutable borrow
    let z = &x; // OK: multiple immutable borrows allowed
    println!("{} {} {}", x, y, z);
}

// Test 2: Basic mutable borrowing
fn test_mutable_borrow() {
    let mut x = 5;
    let y = &mut x; // OK: mutable borrow
    *y = 10;
    // let z = &x; // ERROR: cannot create immutable borrow while mutable borrow exists
    // let z = &mut x; // ERROR: cannot create another mutable borrow while one exists
    println!("{}", x);
}

// Test 3: Borrowing after move
fn test_borrow_after_move() {
    let x = String::from("hello");
    let y = x; // x is moved
    // let z = &x; // ERROR: cannot borrow moved value
    println!("{}", y);
}

// Test 4: Field borrowing
fn test_field_borrow() {
    struct Point {
        x: i32,
        y: i32,
    }
    
    let mut p = Point { x: 1, y: 2 };
    let x_ref = &p.x; // OK: immutable field borrow
    // let y_mut = &mut p.y; // ERROR: cannot create mutable field borrow while immutable field borrow exists
    println!("{} {}", x_ref, p.y);
}

// Test 5: Mutable field borrowing
fn test_mutable_field_borrow() {
    struct Point {
        x: i32,
        y: i32,
    }
    
    let mut p = Point { x: 1, y: 2 };
    let y_mut = &mut p.y; // OK: mutable field borrow
    *y_mut = 3;
    // let x_ref = &p.x; // ERROR: cannot create immutable field borrow while mutable field borrow exists
    println!("{} {}", p.x, p.y);
}

// Test 6: Index borrowing
fn test_index_borrow() {
    let mut arr = [1, 2, 3];
    let first = &arr[0]; // OK: immutable index borrow
    // let second_mut = &mut arr[1]; // ERROR: cannot create mutable index borrow while immutable index borrow exists
    println!("{} {}", first, arr[1]);
}

// Test 7: Mutable index borrowing
fn test_mutable_index_borrow() {
    let mut arr = [1, 2, 3];
    let first_mut = &mut arr[0]; // OK: mutable index borrow
    *first_mut = 10;
    // let second = &arr[1]; // ERROR: cannot create immutable index borrow while mutable index borrow exists
    println!("{} {}", arr[0], arr[1]);
}

// Test 8: Deref borrowing
fn test_deref_borrow() {
    let x = 5;
    let r = &x;
    let dr = &*r; // OK: deref borrow
    println!("{} {}", r, dr);
}

// Test 9: Mutable variable requirement
fn test_mutable_variable_requirement() {
    let x = 5;
    // let y = &mut x; // ERROR: cannot create mutable borrow of immutable variable
    
    let mut x = 5;
    let y = &mut x; // OK: mutable borrow of mutable variable
    *y = 10;
    println!("{}", x);
}

fn main() {
    test_immutable_borrow();
    test_mutable_borrow();
    test_borrow_after_move();
    test_field_borrow();
    test_mutable_field_borrow();
    test_index_borrow();
    test_mutable_index_borrow();
    test_deref_borrow();
    test_mutable_variable_requirement();
}