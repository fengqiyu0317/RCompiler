struct S {
    a: i32,
    b: String,
}

struct U {
    a: i32
}

trait T {
    fn foo() -> i32 {
        42
    }
}

impl S {
    fn bar(&mut self) {
        self.a = 10;
    }
}

enum A {
    B,
    C
}

fn main() {
    let m: i32 = 1500000000 + 1500000000;
    // println!("g.a: {}, h: {}", g.a, h);
    // {n}.b = String::from("changed");
    // let m = n;
    // let p = &s1;
    // let q = *p;
    // {
    //     let mut s1 = String::from("foo");
    //     let mut g = s1;
    // }
    // n.bar();
    // output n.a and n.b
    // println!("n.a: {}, n.b: {}", n.a, n.b);
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