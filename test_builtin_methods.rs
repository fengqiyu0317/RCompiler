// Test file for builtin methods
fn main() {
    // Test String methods
    let mut s = String::new();
    s = s.to_string();  // to_string(&self) -> String
    let str_ref = s.as_str();  // as_str(&self) -> &str
    let mut_str_ref = s.as_mut_str();  // as_mut_str(&mut self) -> &mut str
    let len = s.len();  // len(&self) -> usize
    s.append("world");  // append(&mut self, s: &str) -> ()
    
    // Test str methods
    let hello = "Hello";
    let str_len = hello.len();  // len(&self) -> usize
    let string_from_str = hello.to_string();  // to_string(&self) -> String
    
    // Print results
    println!("String length: {}", len);
    println!("str length: {}", str_len);
    println!("String from str: {}", string_from_str);
}