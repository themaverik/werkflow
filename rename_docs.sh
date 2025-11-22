#!/bin/bash

# Function to convert filename to Title-Case with hyphens
convert_filename() {
    local file="$1"
    local dir=$(dirname "$file")
    local filename=$(basename "$file")
    
    # Skip if already correct format or is README.md
    if [[ "$filename" == "README.md" ]] || [[ "$filename" == *.md && ! "$filename" =~ [_A-Z] ]]; then
        return
    fi
    
    # Convert underscores to hyphens, keep Title Case
    local newname=$(echo "$filename" | sed 's/_/-/g')
    
    if [[ "$newname" != "$filename" ]]; then
        mv "$file" "$dir/$newname"
        echo "Renamed: $filename → $newname"
    fi
}

# Find all markdown files and rename them
find /Users/lamteiwahlang/Projects/werkflow/docs -name "*.md" -type f | while read file; do
    convert_filename "$file"
done

echo "Documentation file renaming complete!"
