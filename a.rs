fn main() {
    let i: u32 = 10;
    let result: u32 = loop {
        if (i <= 2) {
            break 42;        // i32类型
        } else {
            break 100;       // 也是i32类型，兼容
        }
    };
}