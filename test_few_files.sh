#!/bin/bash

echo "Testing a few files individually..."

# Test array1
echo "Processing array1..."
java Main < RCompiler-Testcases/semantic-1/array1/array1.rx > result/semantic-1/array1_test.txt

# Test array2
echo "Processing array2..."
java Main < RCompiler-Testcases/semantic-1/array2/array2.rx > result/semantic-1/array2_test.txt

# Test return1
echo "Processing return1..."
java Main < RCompiler-Testcases/semantic-1/return1/return1.rx > result/semantic-1/return1_test.txt

echo "Done. Check the *_test.txt files."