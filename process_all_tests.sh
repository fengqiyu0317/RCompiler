#!/bin/bash

# Script to run the parser on all .rx files and save results in the result folder

RESULT_DIR="result"
TESTCASES_DIR="RCompiler-Testcases"

# Create result directory if it doesn't exist
mkdir -p "$RESULT_DIR"

# Counter for processed files
total_files=0
processed_files=0

echo "Starting to process all .rx files..."
echo "Results will be saved in the $RESULT_DIR directory"
echo ""

# Find all .rx files and process them
find "$TESTCASES_DIR" -name "*.rx" | sort | while read rx_file; do
    total_files=$((total_files + 1))
    
    # Get the relative path from TESTCASES_DIR to create a unique filename
    rel_path=${rx_file#$TESTCASES_DIR/}
    # Replace / with _ to create a valid filename
    output_file="${RESULT_DIR}/${rel_path//\//_}.txt"
    
    echo "Processing: $rel_path"
    
    # Copy the .rx file to a.rs (as expected by the parser)
    cp "$rx_file" a.rs
    
    # Run the parser
    ./a < a.rs > /dev/null 2>&1
    
    # Check if result.txt was created and copy it to our output file
    if [ -f "result.txt" ]; then
        cp result.txt "$output_file"
        echo "  -> Saved to: $output_file"
        processed_files=$((processed_files + 1))
    else
        echo "  -> ERROR: No output generated"
    fi
    
    echo ""
done

echo "Processing complete!"
echo "Total files found: $total_files"
echo "Files processed: $processed_files"
echo "Results saved in: $RESULT_DIR/"