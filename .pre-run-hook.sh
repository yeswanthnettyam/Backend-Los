#!/bin/bash
# Pre-run hook for IntelliJ IDEA
# This script ensures resources are copied before application starts

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure resources are copied
if [ -d "src/main/resources" ]; then
    mkdir -p target/classes/db/migration
    cp -rf src/main/resources/* target/classes/ 2>/dev/null || true
fi
