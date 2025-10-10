
fn main() {
    Point { x: 10.0, y: 20.0 };
    NothingInMe {};
    let u = User { name: "Joe".to_string(), age: 35, score: 100_000 };
    Enum::Variant {};
}

struct Point { x: f64, y: f64 }
struct NothingInMe { }
struct User { name: String, age: u32, score: usize }
enum Enum { Variant {} }
