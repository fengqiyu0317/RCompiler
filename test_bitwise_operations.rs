fn main() {
    // 测试位操作符与布尔类型
    let a = true;
    let b = false;
    
    // 布尔类型的位操作
    let c = a & b;  // 应该是 false
    let d = a | b;  // 应该是 true
    let e = a ^ b;  // 应该是 true
    
    // 测试位操作符与整数类型
    let x = 5i32;  // 二进制: 0101
    let y = 3i32;  // 二进制: 0011
    
    // 整数类型的位操作
    let z = x & y;  // 应该是 1 (0001)
    let w = x | y;  // 应该是 7 (0111)
    let v = x ^ y;  // 应该是 6 (0110)
    
    // 测试位复合赋值与布尔类型
    let mut bool_val = true;
    bool_val &= false;  // bool_val 应该是 false
    
    // 测试位复合赋值与整数类型
    let mut int_val = 5i32;
    int_val &= 3i32;  // int_val 应该是 1
    
    println!("All bitwise operations completed successfully!");
}