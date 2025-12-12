struct TestStruct {
    field1: i32,
    field2: i32,
}

fn main() {
    let mut obj = TestStruct { field1: 5, field2: 10 };
    
    // 测试1: 不同字段的独立借用应该被允许
    let borrow1 = &mut obj.field1;  // 可变借用 field1
    let borrow2 = &obj.field2;      // 不可变借用 field2 - 这应该是允许的！
    
    println!("borrow1: {}, borrow2: {}", borrow1, borrow2);
    
    // 测试2: 同一字段的冲突借用应该被拒绝
    // let borrow3 = &obj.field1;     // 这应该报错，因为 field1 已经被可变借用
    // let borrow4 = &mut obj.field1; // 这也应该报错，因为 field1 已经被可变借用
    
    // 测试3: 基础变量的借用应该阻止字段借用
    // let borrow5 = &mut obj;        // 可变借用整个对象
    // let borrow6 = &obj.field1;     // 这应该报错，因为 obj 已经被可变借用
}