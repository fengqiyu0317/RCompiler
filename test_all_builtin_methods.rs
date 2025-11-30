// Test file for all builtin methods

fn main() {
    // Test String methods
    let mut s: String = String::from("Hello");
    s.append(", world!");
    let s_ref: &str = s.as_str();
    let s_mut: &mut str = s.as_mut_str();
    let s_len: usize = s.len();
    printlnInt(s_len as i32);
    
    // Test str methods
    let str_slice: &str = "Hello, Rust!";
    let str_string: String = str_slice.to_string();
    let str_len: usize = str_slice.len();
    printlnInt(str_len as i32);
    
    // Test array methods
    let arr: [i32; 5] = [1, 2, 3, 4, 5];
    let arr_len: usize = arr.len();
    printlnInt(arr_len as i32);
    
    let arr_ref: &[i32; 5] = &arr;
    let arr_ref_len: usize = arr_ref.len();
    printlnInt(arr_ref_len as i32);
    
    let mut arr_mut: [i32; 5] = [1, 2, 3, 4, 5];
    let arr_mut_ref: &mut [i32; 5] = &mut arr_mut;
    let arr_mut_ref_len: usize = arr_mut_ref.len();
    printlnInt(arr_mut_ref_len as i32);
    
    // Test u32 to_string method
    let num_u32: u32 = 42;
    let u32_string: String = num_u32.to_string();
    println(u32_string.as_str());
    
    // Test usize to_string method
    let num_usize: usize = 100;
    let usize_string: String = num_usize.to_string();
    println(usize_string.as_str());
}