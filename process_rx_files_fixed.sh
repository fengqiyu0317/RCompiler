#!/bin/bash

# Script to process all .rx files in RCompiler-Testcases/semantic-1 and RCompiler-Testcases/semantic-2
# and save the output to corresponding .txt files in result directory

echo "Processing .rx files..."

# Process semantic-1 directory
for dir in RCompiler-Testcases/semantic-1/*/; do
    if [ -d "$dir" ]; then
        dir_name=$(basename "$dir")
        rx_file="$dir$dir_name.rx"
        output_file="result/semantic-1/$dir_name.txt"
        
        if [ -f "$rx_file" ]; then
            echo "Processing $rx_file -> $output_file"
            # Copy the .rx file to a.rs (as expected by the parser)
            cp "$rx_file" a.rs
            # Run the parser
            ./a < a.rs > /dev/null 2>&1
            # Check if result.txt was created and copy it to our output file
            if [ -f "result.txt" ]; then
                cp result.txt "$output_file"
            else
                echo "  -> ERROR: No output generated"
                touch "$output_file"  # Create empty file
            fi
        else
            echo "Warning: $rx_file not found"
        fi
    fi
done

# Process semantic-2 directory
for dir in RCompiler-Testcases/semantic-2/*/; do
    if [ -d "$dir" ]; then
        dir_name=$(basename "$dir")
        rx_file="$dir$dir_name.rx"
        output_file="result/semantic-2/$dir_name.txt"
        
        if [ -f "$rx_file" ]; then
            echo "Processing $rx_file -> $output_file"
            # Copy the .rx file to a.rs (as expected by the parser)
            cp "$rx_file" a.rs
            # Run the parser
            ./a < a.rs > /dev/null 2>&1
            # Check if result.txt was created and copy it to our output file
            if [ -f "result.txt" ]; then
                cp result.txt "$output_file"
            else
                echo "  -> ERROR: No output generated"
                touch "$output_file"  # Create empty file
            fi
        else
            echo "Warning: $rx_file not found"
        fi
    fi
done

echo "Processing complete!"