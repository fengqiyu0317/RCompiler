fn foo() -> i32 {
    // let x: () = {
    //     if (2 + 2 == 4) {
    //         1
    //     } else {
    //         2
    //     }
    // };
    // let y = if (2 + 2 == 4) {
    //     return 1;
    //     const Y: i32 = 5;
    // } else {
    //     2
    // };
    // let x: i32 = 4;
    return 3;
}

trait Bar {
    fn baz(&self) -> i32;
    fn qux(&self) -> i32;
}

struct MyStruct;

impl Bar for MyStruct {
    fn baz(&self) -> i32 {
        42
    }
    
    fn qux(&self) -> i32 {
        24
    }
}

fn main() {
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