#!/bin/bash

# PingPad Test Runner Script
# This script runs the test suite for the PingPad application

set -e  # Exit on any error

echo "🧪 Starting PingPad Test Suite"
echo "================================"

# Change to backend directory
cd backend

echo "📦 Running Maven clean and compile..."
mvn clean compile

echo "🧪 Running tests..."
mvn test

echo "📊 Test Results:"
echo "================"
if ls target/surefire-reports/TEST-*.xml 1> /dev/null 2>&1; then
    echo "✅ Tests completed successfully"
    echo "📁 Test reports available in: target/surefire-reports/"
else
    echo "⚠️  Test reports not found, but tests completed"
    echo "📁 Checking target/surefire-reports/ directory..."
    ls -la target/surefire-reports/ 2>/dev/null || echo "Directory does not exist"
fi

echo ""
echo "🎉 All tests passed! Ready for CI/CD pipeline."
