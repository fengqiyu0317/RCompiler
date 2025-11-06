#!/bin/bash

echo "Reliably reprocessing all files..."

# Process semantic-1 files
echo "Processing semantic-1 files..."
for dir in RCompiler-Testcases/semantic-1/*/; do
    if [ -d "$dir" ]; then
        dirname=$(basename "$dir")
        rxfile=$(find "$dir" -name "*.rx" | head -1)
        
        if [ -n "$rxfile" ]; then
            echo "Processing $rxfile..."
            # Clear any previous state
            java -Xms32m -Xmx128m Main < "$rxfile" > "result/semantic-1/$dirname.txt" 2>&1
            echo "  Saved to result/semantic-1/$dirname.txt"
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
            # Clear any previous state
            java -Xms32m -Xmx128m Main < "$rxfile" > "result/semantic-2/$dirname.txt" 2>&1
            echo "  Saved to result/semantic-2/$dirname.txt"
        fi
    fi
done

echo "All files reprocessed reliably."