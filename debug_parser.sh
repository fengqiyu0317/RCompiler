#!/bin/bash

echo "=== Testing the native parser executable ==="

# Test 1: Simple function
echo "Test 1: Simple function"
echo 'fn main() { exit(0); }' > test1.rs
./a < test1.rs > result1.txt 2>&1
echo "Exit code: $?"
echo "Output:"
cat result1.txt
echo ""

# Test 2: Function with variable
echo "Test 2: Function with variable"
echo 'fn main() { let x: i32 = 42; exit(0); }' > test2.rs
./a < test2.rs > result2.txt 2>&1
echo "Exit code: $?"
echo "Output:"
cat result2.txt
echo ""

# Test 3: Function with array
echo "Test 3: Function with array"
echo 'fn main() { let arr: [i32; 3] = [1, 2, 3]; exit(0); }' > test3.rs
./a < test3.rs > result3.txt 2>&1
echo "Exit code: $?"
echo "Output:"
cat result3.txt
echo ""

# Compare results
echo "=== Comparing results ==="
if diff result1.txt result2.txt > /dev/null; then
    echo "WARNING: Test 1 and Test 2 produced the same output!"
else
    echo "Test 1 and Test 2 produced different outputs (good)"
fi

if diff result2.txt result3.txt > /dev/null; then
    echo "WARNING: Test 2 and Test 3 produced the same output!"
else
    echo "Test 2 and Test 3 produced different outputs (good)"
fi

# Clean up
rm -f test1.rs test2.rs test3.rs result1.txt result2.txt result3.txt