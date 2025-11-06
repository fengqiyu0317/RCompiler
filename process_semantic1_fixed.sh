#!/bin/bash

# Process all .rx files in semantic-1 directory with error handling
for dir in RCompiler-Testcases/semantic-1/*/; do
    if [ -d "$dir" ]; then
        # Get the directory name without path
        dirname=$(basename "$dir")
        # Find the .rx file in this directory
        rxfile=$(find "$dir" -name "*.rx" | head -1)
        
        if [ -n "$rxfile" ]; then
            echo "Processing $rxfile..."
            # Run the lexical analyzer with error handling and save output
            if java Main < "$rxfile" > "result/semantic-1/$dirname.txt" 2>&1; then
                echo "Saved result to result/semantic-1/$dirname.txt"
            else
                echo "Error processing $rxfile, but saved partial result to result/semantic-1/$dirname.txt"
            fi
        fi
    fi
done

echo "All semantic-1 files processed."