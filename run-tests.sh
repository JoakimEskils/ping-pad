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
if [ -f "target/surefire-reports/TEST-*.xml" ]; then
    echo "✅ Tests completed successfully"
    echo "📁 Test reports available in: target/surefire-reports/"
else
    echo "❌ Test reports not found"
    exit 1
fi

echo ""
echo "🎉 All tests passed! Ready for CI/CD pipeline."
