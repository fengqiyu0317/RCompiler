// Test case for exit function position checking

// This should pass - exit is the last statement in main
fn main() {
    println!("Hello, world!");
    exit(0);
}

// This should fail - exit is not the last statement
fn main_bad() {
    exit(0);
    println!("This should not be printed");
}

// This should pass - exit is used as return value
fn main_return() {
    exit(0)
}

// This should pass - exit is in a different function
fn other_function() {
    exit(1);
}

fn main_ok() {
    other_function();
}