#!/bin/bash

echo "=========================================="
echo "Testing Master Data API"
echo "=========================================="
echo ""

# Wait for application to start
echo "Waiting for application to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Application is running!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Application did not start in time"
        exit 1
    fi
    sleep 1
done

echo ""
echo "=========================================="
echo "GET /api/v1/master-data"
echo "=========================================="
echo ""

# Test Master Data API
response=$(curl -s -w "\n%{http_code}" http://localhost:8080/api/v1/master-data)
http_code=$(echo "$response" | tail -n 1)
body=$(echo "$response" | sed '$d')

echo "HTTP Status: $http_code"
echo ""
echo "Response Body:"
echo "$body" | jq . 2>/dev/null || echo "$body"

echo ""
echo "=========================================="
echo "Validation"
echo "=========================================="

if [ "$http_code" = "200" ]; then
    echo "✅ Status Code: 200 OK"
else
    echo "❌ Status Code: Expected 200, got $http_code"
fi

# Check if response contains expected fields
if echo "$body" | jq -e '.partners' > /dev/null 2>&1; then
    echo "✅ Response contains 'partners' field"
    partners_count=$(echo "$body" | jq '.partners | length')
    echo "   Partners found: $partners_count"
else
    echo "❌ Response missing 'partners' field"
fi

if echo "$body" | jq -e '.products' > /dev/null 2>&1; then
    echo "✅ Response contains 'products' field"
    products_count=$(echo "$body" | jq '.products | length')
    echo "   Products found: $products_count"
else
    echo "❌ Response missing 'products' field"
fi

if echo "$body" | jq -e '.branches' > /dev/null 2>&1; then
    echo "✅ Response contains 'branches' field"
    branches_count=$(echo "$body" | jq '.branches | length')
    echo "   Branches found: $branches_count"
else
    echo "❌ Response missing 'branches' field"
fi

echo ""
echo "=========================================="
echo "Test Complete"
echo "=========================================="
