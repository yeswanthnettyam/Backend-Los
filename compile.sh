#!/bin/bash

# Set JAVA_HOME to Java 21
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# Verify Java version
echo "Using Java:"
java -version

echo ""
echo "Compiling project..."
mvn clean compile

# Ensure resources are copied (for IntelliJ compatibility)
echo ""
echo "Ensuring resources are copied..."
mkdir -p target/classes/db/migration
cp -f src/main/resources/db/migration/*.sql target/classes/db/migration/ 2>/dev/null || true
cp -f src/main/resources/*.yml target/classes/ 2>/dev/null || true
cp -f src/main/resources/*.yaml target/classes/ 2>/dev/null || true
cp -rf src/main/resources/* target/classes/ 2>/dev/null || true

echo "✅ Build complete. Resources copied to target/classes/"

