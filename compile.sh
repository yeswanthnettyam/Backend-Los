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

