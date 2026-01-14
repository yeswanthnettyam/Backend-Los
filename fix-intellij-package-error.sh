#!/bin/bash
# Script to fix IntelliJ "package does not exist" error

echo "=== Fixing IntelliJ Package Error ==="
echo ""

# Step 1: Ensure Java 21 is used
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "1. Setting JAVA_HOME to Java 21..."
echo "   JAVA_HOME: $JAVA_HOME"
java -version
echo ""

# Step 2: Clean and compile with correct Java version
echo "2. Cleaning project..."
mvn clean

echo ""
echo "3. Compiling with Java 21..."
mvn compile -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilation successful!"
    echo ""
    echo "4. Verifying entity classes exist..."
    if [ -d "target/classes/com/los/config/entity" ]; then
        echo "   ✅ Entity classes found in target/classes"
        ls -1 target/classes/com/los/config/entity/*.class 2>/dev/null | wc -l | xargs echo "   Total .class files:"
    else
        echo "   ❌ Entity classes not found in target/classes"
    fi
    echo ""
    echo "=== Next Steps ==="
    echo "1. In IntelliJ: File → Invalidate Caches → Invalidate and Restart"
    echo "2. In IntelliJ: Build → Rebuild Project"
    echo "3. Verify: File → Project Structure → Project SDK is set to Java 21"
else
    echo ""
    echo "❌ Compilation failed. Please check Java version configuration."
    echo ""
    echo "Current Java version:"
    java -version
    echo ""
    echo "Expected: Java 21"
fi
