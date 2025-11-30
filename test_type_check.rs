fn main() {
    let x: i32 = 5;
    let y: i32 = 10;
    let z = x + y;
    
    if z > 10 {
        println!("z is greater than 10");
    } else {
        println!("z is less than or equal to 10");
    }
    
    let arr = [1, 2, 3, 4, 5];
    let first = arr[0];
    
    struct Point {
        x: i32,
        y: i32,
    }
    
    let p = Point { x: 1, y: 2 };
    let px = p.x;
    let py = p.y;
}