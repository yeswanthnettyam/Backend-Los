#!/bin/bash
# Script to ensure resources are copied to target/classes
# Run this before starting the application in IntelliJ

echo "Ensuring resources are copied to target/classes..."

# Create target/classes directory structure
mkdir -p target/classes/db/migration

# Copy all resources
if [ -d "src/main/resources" ]; then
    echo "Copying resources from src/main/resources to target/classes..."
    cp -rf src/main/resources/* target/classes/ 2>/dev/null || true
    
    # Explicitly copy migration files
    if [ -d "src/main/resources/db/migration" ]; then
        echo "Copying Flyway migration files..."
        cp -f src/main/resources/db/migration/*.sql target/classes/db/migration/ 2>/dev/null || true
    fi
    
    echo "✅ Resources copied successfully"
    MIGRATION_COUNT=$(ls -1 target/classes/db/migration/*.sql 2>/dev/null | wc -l)
    echo "   Migration files copied: $MIGRATION_COUNT"
else
    echo "❌ Error: src/main/resources directory not found"
    exit 1
fi
