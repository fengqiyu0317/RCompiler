// struct S {
//     a: i32,
//     b: String,
// }

// struct U {
//     a: i32
// }

// trait T {
//     fn foo() -> i32 {
//         42
//     }
// }

// impl S {
//     fn bar(&mut self) {
//         self.a = 10;
//     }
// }

// enum A {
//     B,
//     C
// }

// fn add(x: i32, y: i32) -> i32 {
//     fn sub(x: i32, y: i32) -> i32 {
//         x - y
//     }
//     x + y + sub(x, y)
// }

fn modify(mut a: [i32; 3]) {
    a[0] = 10;
    println!("{:?}", a);
}

fn main() {
    let mut arr: [i32; 3] = [1, 2, 3];
    let mut arr1 = arr; // This creates a copy of the array
    arr1[0] = 10; // This modifies the copy, not the original array
    println!("Original array: {:?}", arr); // Output: [1, 2, 3]
    println!("Modified array: {:?}", arr1); // Output: [10, 2, 3]
}