#!/bin/bash

# First, let's try to compile the Java files using a different approach
echo "Attempting to compile Java files..."

# Try to find the Java files and compile them
java_files=$(ls *.java 2>/dev/null | grep -v TestRunner.java)

if [ -z "$java_files" ]; then
    echo "No Java files found in current directory"
    exit 1
fi

echo "Found Java files: $java_files"

# Try to compile each file individually
for file in $java_files; do
    echo "Compiling $file..."
    javac "$file" 2>&1
    if [ $? -ne 0 ]; then
        echo "Failed to compile $file"
    else
        echo "Successfully compiled $file"
    fi
done

# Check if Main.class was created
if [ -f "Main.class" ]; then
    echo "Main.class found, trying to run a test..."
    
    # Copy a test file and run the parser
    cp RCompiler-Testcases/semantic-1/basic1/basic1.rx a.rs
    
    echo "Running parser on basic1.rx..."
    java Main > test_output.txt 2>&1
    exit_code=$?
    
    echo "Parser exit code: $exit_code"
    
    if [ -f "result.txt" ]; then
        echo "result.txt created:"
        cat result.txt
    else
        echo "result.txt not created"
    fi
    
    if [ -f "test_output.txt" ]; then
        echo "Parser output:"
        cat test_output.txt
    fi
else
    echo "Main.class not found, compilation failed"
fi