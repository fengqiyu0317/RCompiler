#!/bin/bash

# Script to run SymbolDebugTest with all .rx files in RCompiler-Testcases/semantic-2

echo "Compiling the project..."
make compile

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Directory containing test cases
TEST_DIR="RCompiler-Testcases/semantic-1"

# Create results directory if it doesn't exist
RESULTS_DIR="results/semantic-1-typechecker"
mkdir -p "$RESULTS_DIR"

# Counter for processed files
count=0
success=0
failed=0

echo "Starting batch testing of all .rx files in $TEST_DIR..."
echo "=================================================="

# Find all .rx files in the test directory and its subdirectories
while IFS= read -r -d '' file; do
    count=$((count + 1))
    filename=$(basename "$file" .rx)
    result_file="$RESULTS_DIR/${filename}_result.txt"
    
    echo "Processing test $count: $filename"
    echo "File: $file"
    
    # Run the test and capture output
    java -cp target/classes SymbolDebugTest < "$file" > "$result_file" 2>&1
    exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo "Status: SUCCESS"
        success=$((success + 1))
    else
        echo "Status: FAILED (exit code: $exit_code)"
        failed=$((failed + 1))
    fi
    
    echo "Output saved to: $result_file"
    echo "----------------------------------------"
done < <(find "$TEST_DIR" -name "*.rx" -print0 | sort -z)

echo ""
echo "Batch testing completed!"
echo "Total files processed: $count"
echo "Successful tests: $success"
echo "Failed tests: $failed"
echo "Results saved in: $RESULTS_DIR"