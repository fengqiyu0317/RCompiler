struct Point {
    x: i32,
    y: i32,
}

fn test_parent_moved_child_cannot_borrow() {
    let mut p = Point { x: 1, y: 2 };
    
    // 移动父结构体
    let p2 = p;
    
    // 尝试借用子字段 - 应该报错
    let _x_ref = &p2.x; // 这是合法的，因为p2拥有所有权
    
    // 下面这行应该报错：Borrow of moved value: 'p'
    // let _x_ref = &p.x;
}

fn test_parent_immutable_borrow_child_mutable_borrow() {
    let mut p = Point { x: 1, y: 2 };
    
    // 不可变借用父结构体
    let p_ref = &p;
    
    // 尝试可变借用子字段 - 应该报错
    // 下面这行应该报错：Cannot mutably borrow field 'x' of 'p' while it is immutably borrowed
    // let _x_mut = &mut p.x;
    
    // 不可变借用子字段是合法的
    let _x_ref = &p.x;
}

fn test_parent_mutable_borrow_child_any_borrow() {
    let mut p = Point { x: 1, y: 2 };
    
    // 可变借用父结构体
    let p_mut = &mut p;
    
    // 尝试不可变借用子字段 - 应该报错
    // 下面这行应该报错：Cannot borrow field 'x' of 'p' while it is mutably borrowed
    // let _x_ref = &p.x;
    
    // 尝试可变借用子字段 - 也应该报错
    // 下面这行应该报错：Cannot borrow field 'x' of 'p' while it is mutably borrowed
    // let _x_mut = &mut p.x;
}

fn test_index_access_parent_borrow() {
    let mut arr = [1, 2, 3];
    
    // 可变借用父结构体（数组）
    let arr_mut = &mut arr;
    
    // 尝试索引访问 - 应该报错
    // 下面这行应该报错：Cannot index 'arr' while it is mutably borrowed
    // let _first = &arr[0];
}

fn test_deref_access_parent_borrow() {
    let mut x = 5;
    let mut ptr = &mut x;
    
    // 可变借用父指针
    let ptr_mut = &mut ptr;
    
    // 尝试解引用访问 - 应该报错
    // 下面这行应该报错：Cannot dereference 'ptr' while it is mutably borrowed
    // let _x_ref = &*ptr;
    
    // 尝试可变解引用访问 - 也应该报错
    // 下面这行应该报错：Cannot mutably dereference 'ptr' while it is mutably borrowed
    // let _x_mut = &mut *ptr;
}

fn test_index_access_immutable_parent_mutable_child() {
    let mut arr = [1, 2, 3];
    
    // 不可变借用父数组
    let arr_ref = &arr;
    
    // 尝试可变索引访问 - 应该报错
    // 下面这行应该报错：Cannot mutably index 'arr' while it is immutably borrowed
    // let _first_mut = &mut arr[0];
    
    // 不可变索引访问是合法的
    let _first_ref = &arr[0];
}

fn main() {
    test_parent_moved_child_cannot_borrow();
    test_parent_immutable_borrow_child_mutable_borrow();
    test_parent_mutable_borrow_child_any_borrow();
    test_index_access_parent_borrow();
    test_deref_access_parent_borrow();
    test_index_access_immutable_parent_mutable_child();
}