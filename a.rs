struct S {
    a: i32,
    b: i32,
}

trait T {
    fn foo() -> i32 {
        42
    }
}

impl T for S {
}

fn main() {
    let s = S { a: 1, b: 2 };
    s.a = 3;
    // if (2 + 2 == 4) {
    //     1
    // } else {
    //     2
    // }
    // let x: i32 = 4;
    // let x: i32 = loop {
    //     if (2 + 2 == 4) {
    //         break 1;
    //     } else {
    //         2
    //     }
    //     break 3;
    // };
}