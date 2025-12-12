struct S {
    a: String,
    b: String,
}

fn main() {
    let mut s1 = String::from("hello");
    let mut s2 = String::from("world");
    let n = S {a: s1, b: s2};
    
    // 测试部分移动：移动字段a后，字段b仍然可用
    let g = n.a;  // 移动字段a
    let h = n.b;  // 应该仍然可以移动字段b
    
    // 测试错误情况：尝试再次使用已移动的字段a
    // let i = n.a;  // 这应该报错：Use of moved value: 'n.a'
    
    // 测试嵌套字段访问
    struct T {
        s: S,
    }
    
    let t = T {
        s: S {
            a: String::from("nested_a"),
            b: String::from("nested_b"),
        }
    };
    
    // 移动嵌套字段
    let nested_a = t.s.a;  // 移动t.s.a
    let nested_b = t.s.b;  // 应该仍然可以移动t.s.b
    
    // 测试字段借用
    let n2 = S {
        a: String::from("borrow_a"),
        b: String::from("borrow_b"),
    };
    
    let borrow_a = &n2.a;  // 借用字段a
    let borrow_b = &n2.b;  // 应该仍然可以借用字段b
    
    // 测试字段移动后的借用
    let n3 = S {
        a: String::from("move_then_borrow_a"),
        b: String::from("move_then_borrow_b"),
    };
    
    let moved_a = n3.a;  // 移动字段a
    // let borrow_after_move = &n3.a;  // 这应该报错：Borrow of moved value: 'n3.a'
    let borrow_b_after_move = &n3.b;  // 应该仍然可以借用字段b
}