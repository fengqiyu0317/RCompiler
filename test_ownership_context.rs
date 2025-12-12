fn main() {
    let x = String::from("hello");
    
    // 测试基本作用域
    {
        let y = x; // x 被移动到 y
        println!("{}", y); // 应该可以正常使用 y
    } // y 离开作用域
    
    // 这里 x 应该已经被移动，不能再使用
    // println!("{}", x); // 这行应该报错：use of moved value
    
    // 测试 if 表达式的作用域
    let a = String::from("world");
    if true {
        let b = a; // a 被移动到 b
        println!("{}", b); // 应该可以正常使用 b
    } else {
        // 在 else 分支中，a 仍然可用
        println!("{}", a); // 应该可以正常使用 a
    }
    
    // 在 if 表达式外，a 可能已被移动
    // println!("{}", a); // 这行可能报错，取决于哪个分支被执行
    
    // 测试循环作用域
    let mut c = String::from("loop");
    for _ in 0..1 {
        let d = c; // c 被移动到 d
        println!("{}", d); // 应该可以正常使用 d
    } // d 离开作用域
    
    // 循环结束后，c 应该已经被移动
    // println!("{}", c); // 这行应该报错：use of moved value
}