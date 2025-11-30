// fn foo() -> i32 {
//     if (2 + 2 == 4) {
//         return 1;
//     } else {
//         2
//     } + 2;
//     return 3;
// }

fn main() {
    let x: i32 = {
        if (1 <= 2) {
            2
        } else {
            3
        };
    };
    // let x: i32 = loop {
    //     if (2 + 2 == 4) {
    //         break 1;
    //     } else {
    //         2
    //     }
    //     break 3;
    // };
}