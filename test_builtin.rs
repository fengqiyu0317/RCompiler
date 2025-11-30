// Test file for builtin functions and methods

fn main() {
    // Test builtin functions
    print("Hello, ");
    println("world!");
    
    let x: i32 = 42;
    printInt(x);
    println();
    
    let s: String = getString();
    println(s.as_str());
    
    let n: i32 = getInt();
    printlnInt(n);
    
    // Test builtin methods
    let num: u32 = 123;
    let str_num: String = num.to_string();
    println(str_num.as_str());
    
    let arr: [i32; 5] = [1, 2, 3, 4, 5];
    let len: usize = arr.len();
    printlnInt(len as i32);
    
    let mut mutable_str: String = String::from("Hello");
    let str_slice: &mut str = mutable_str.as_mut_str();
    println(str_slice);
    
    mutable_str.append(", world!");
    println(mutable_str.as_str());
    
    // Test exit function (should be last statement)
    exit(0);
}