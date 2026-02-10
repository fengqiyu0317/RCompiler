use std::fs;
use std::path::Path;

fn main() {
    let out_dir = Path::new("/mnt/d/Tomato_Fish/RCompiler/tests/test_loop");
    fs::create_dir_all(out_dir).unwrap();

    // test1: sum of odd numbers < 20
    let mut total: u32 = 0;
    let mut n: u32 = 0;
    while n < 20 {
        if n % 2 == 0 {
            n += 1;
            continue;
        }
        total += n;
        n += 1;
    }
    fs::write(out_dir.join("test1.ans"), format!("{}\n", total)).unwrap();

    // test2: count triples x,y,z in [0,3] with x+y+z==3
    let mut combos: u32 = 0;
    let mut x: u32 = 0;
    while x < 4 {
        let mut y: u32 = 0;
        while y < 4 {
            let mut z: u32 = 0;
            while z < 4 {
                if x + y + z == 3 {
                    combos += 1;
                }
                z += 1;
            }
            y += 1;
        }
        x += 1;
    }
    fs::write(out_dir.join("test2.ans"), format!("{}\n", combos)).unwrap();

    // test3: sieve of Eratosthenes, count primes < 30
    const N: usize = 30;
    let mut is_prime = [true; N];
    is_prime[0] = false;
    is_prime[1] = false;
    let mut p: usize = 2;
    while p < N {
        if is_prime[p] {
            let mut multiple: usize = p * p;
            while multiple < N {
                is_prime[multiple] = false;
                multiple += p;
            }
        }
        p += 1;
    }
    let mut prime_count: i32 = 0;
    let mut idx: usize = 0;
    while idx < N {
        if is_prime[idx] {
            prime_count += 1;
        }
        idx += 1;
    }
    fs::write(out_dir.join("test3.ans"), format!("{}\n", prime_count)).unwrap();

    // test4: nested loop count with break/continue
    let mut count: i32 = 0;
    let mut i: u32 = 0;
    let mut should_break: bool = false;
    loop {
        if i >= 5 { break; }
        let mut j: u32 = 0;
        loop {
            if j >= 5 { break; }
            if i == j {
                j += 1;
                continue;
            }
            if i + j > 6 {
                should_break = true;
                break;
            }
            count += 1;
            j += 1;
        }
        if should_break { break; }
        i += 1;
    }
    fs::write(out_dir.join("test4.ans"), format!("{}\n", count)).unwrap();

    // test5: fib[9]
    let mut fib = [0u32; 10];
    fib[1] = 1;
    let mut i: usize = 2;
    while i < 10 {
        fib[i] = fib[i - 1] + fib[i - 2];
        i += 1;
    }
    fs::write(out_dir.join("test5.ans"), format!("{}\n", fib[9])).unwrap();
}
