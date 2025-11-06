#!/bin/bash

TESTCASES_DIR="RCompiler-Testcases/semantic-1"
A_RS_PATH="a.rs"
RESULT_TXT_PATH="result.txt"

total_tests=0
passed_tests=0
failed_tests=0

# Create a directory for test results if it doesn't exist
mkdir -p test_results

# Get all test directories and sort them
test_dirs=$(find "$TESTCASES_DIR" -maxdepth 1 -type d | sort -V)

for test_dir in $test_dirs; do
    # Skip the parent directory
    if [ "$test_dir" == "$TESTCASES_DIR" ]; then
        continue
    fi
    
    test_name=$(basename "$test_dir")
    echo "Running test: $test_name"
    
    # Find the .rx file in the test directory
    rx_file=$(find "$test_dir" -name "*.rx" | head -n 1)
    
    if [ -z "$rx_file" ]; then
        echo "✗ $test_name FAILED: No .rx file found"
        failed_tests=$((failed_tests + 1))
        total_tests=$((total_tests + 1))
        echo "----------------------------------------"
        continue
    fi
    
    # Copy the .rx file to a.rs
    cp "$rx_file" "$A_RS_PATH"
    
    # Run the parser and capture output
    java Main > "test_results/${test_name}.out" 2>&1
    exit_code=$?
    
    if [ $exit_code -ne 0 ]; then
        echo "✗ $test_name FAILED: Parser failed with exit code $exit_code"
        failed_tests=$((failed_tests + 1))
    else
        # Check if result.txt was created and is not empty
        if [ -f "$RESULT_TXT_PATH" ] && [ -s "$RESULT_TXT_PATH" ]; then
            # Copy the result to test results
            cp "$RESULT_TXT_PATH" "test_results/${test_name}.result"
            echo "✓ $test_name PASSED"
            passed_tests=$((passed_tests + 1))
        else
            echo "✗ $test_name FAILED: Parser produced no output"
            failed_tests=$((failed_tests + 1))
        fi
    fi
    
    total_tests=$((total_tests + 1))
    echo "----------------------------------------"
done

echo ""
echo "Test Summary:"
echo "Total tests: $total_tests"
echo "Passed: $passed_tests"
echo "Failed: $failed_tests"
if [ $total_tests -gt 0 ]; then
    success_rate=$(echo "scale=2; $passed_tests * 100 / $total_tests" | bc)
    echo "Success rate: $success_rate%"
fi