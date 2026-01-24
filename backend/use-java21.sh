#!/bin/bash
# Helper script to set Java 21 for Linkwave backend development
# Usage: source use-java21.sh

export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "✓ Java switched to version:"
java -version
