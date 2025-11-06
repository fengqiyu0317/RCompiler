#!/bin/bash

# Script to organize the parser results into subdirectories based on their categories

RESULT_DIR="result"

# Create subdirectories for different categories
mkdir -p "$RESULT_DIR/semantic-1"
mkdir -p "$RESULT_DIR/semantic-2"

echo "Organizing results into subdirectories..."

# Move semantic-1 results
for file in "$RESULT_DIR"/semantic-1_*.txt; do
    if [ -f "$file" ]; then
        # Extract the original path structure
        filename=$(basename "$file")
        # Replace the first underscore with a slash to restore directory structure
        new_name="${filename/semantic-1_/semantic-1/}"
        # Replace remaining underscores with slashes
        new_name="${new_name//_/\/}"
        # Remove the .rx.txt extension and add .txt
        new_name="${new_name/.rx.txt/.txt}"
        
        # Create the subdirectory if needed
        subdir="$RESULT_DIR/$(dirname "$new_name")"
        mkdir -p "$subdir"
        
        # Move the file
        mv "$file" "$RESULT_DIR/$new_name"
        echo "Moved: $filename -> $new_name"
    fi
done

# Move semantic-2 results
for file in "$RESULT_DIR"/semantic-2_*.txt; do
    if [ -f "$file" ]; then
        # Extract the original path structure
        filename=$(basename "$file")
        # Replace the first underscore with a slash to restore directory structure
        new_name="${filename/semantic-2_/semantic-2/}"
        # Replace remaining underscores with slashes
        new_name="${new_name//_/\/}"
        # Remove the .rx.txt extension and add .txt
        new_name="${new_name/.rx.txt/.txt}"
        
        # Create the subdirectory if needed
        subdir="$RESULT_DIR/$(dirname "$new_name")"
        mkdir -p "$subdir"
        
        # Move the file
        mv "$file" "$RESULT_DIR/$new_name"
        echo "Moved: $filename -> $new_name"
    fi
done

echo ""
echo "Organization complete!"
echo "Results are now organized in subdirectories mirroring the original test structure."