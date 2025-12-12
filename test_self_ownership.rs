// Test file for self parameter ownership handling

struct Point {
    x: i32,
    y: i32,
}

impl Point {
    // Test &self parameter
    fn get_x(&self) -> i32 {
        self.x
    }
    
    // Test &mut self parameter
    fn set_x(&mut self, x: i32) {
        self.x = x;
    }
    
    // Test self: Self parameter (by value)
    fn consume(self) -> i32 {
        self.x + self.y
    }
    
    // Test method with self parameter and borrowing
    fn borrow_self(&self) -> &i32 {
        &self.x
    }
    
    // Test method with mutable self parameter and borrowing
    fn borrow_mut_self(&mut self) -> &mut i32 {
        &mut self.x
    }
    
    // Test method returning self
    fn clone_self(&self) -> Self {
        Point { x: self.x, y: self.y }
    }
}

fn main() {
    let mut p = Point { x: 1, y: 2 };
    
    // Test &self usage
    let x = p.get_x();
    println!("x = {}", x);
    
    // Test &mut self usage
    p.set_x(10);
    println!("p.x = {}", p.x);
    
    // Test borrowing self
    let x_ref = p.borrow_self();
    println!("x_ref = {}", x_ref);
    
    // Test mutable borrowing self
    let x_mut_ref = p.borrow_mut_self();
    *x_mut_ref = 20;
    println!("p.x = {}", p.x);
    
    // Test cloning self
    let p2 = p.clone_self();
    println!("p2.x = {}, p2.y = {}", p2.x, p2.y);
    
    // Test consuming self
    let sum = p.consume();
    println!("sum = {}", sum);
    
    // This should cause an error because p is consumed
    // let x = p.get_x(); // This would be an error
}