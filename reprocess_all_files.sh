#!/bin/bash

echo "Reprocessing all files to ensure correct outputs..."

# Process semantic-1 files
echo "Processing semantic-1 files..."
for dir in RCompiler-Testcases/semantic-1/*/; do
    if [ -d "$dir" ]; then
        dirname=$(basename "$dir")
        rxfile=$(find "$dir" -name "*.rx" | head -1)
        
        if [ -n "$rxfile" ]; then
            echo "Processing $rxfile..."
            java Main < "$rxfile" > "result/semantic-1/$dirname.txt" 2>&1
            if [ $? -eq 0 ]; then
                echo "  Success: result/semantic-1/$dirname.txt"
            else
                echo "  Error processing $rxfile"
            fi
        fi
    fi
done

# Process semantic-2 files
echo "Processing semantic-2 files..."
for dir in RCompiler-Testcases/semantic-2/*/; do
    if [ -d "$dir" ]; then
        dirname=$(basename "$dir")
        rxfile=$(find "$dir" -name "*.rx" | head -1)
        
        if [ -n "$rxfile" ]; then
            echo "Processing $rxfile..."
            java Main < "$rxfile" > "result/semantic-2/$dirname.txt" 2>&1
            if [ $? -eq 0 ]; then
                echo "  Success: result/semantic-2/$dirname.txt"
            else
                echo "  Error processing $rxfile"
            fi
        fi
    fi
done

echo "All files reprocessed."