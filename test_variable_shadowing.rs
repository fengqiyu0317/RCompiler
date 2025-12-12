fn main() {
    let x = String::from("outer");
    {
        let x = String::from("inner");
        // 在这里，x 是内部作用域的变量，与外部作用域的 x 是不同的变量
        println!("Inner x: {}", x);
    }
    // 在这里，x 是外部作用域的变量，内部作用域的 x 已经被销毁
    println!("Outer x: {}", x);
    
    // 测试移动
    let y = String::from("outer y");
    {
        let y = y; // 这里移动了外部作用域的 y 到内部作用域的新变量 y
        println!("Inner y: {}", y);
    }
    // 这里外部作用域的 y 已经被移动，不能再使用
    // println!("Outer y: {}", y); // 这行会导致编译错误
}