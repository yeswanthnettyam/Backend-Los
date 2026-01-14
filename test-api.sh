#!/bin/bash

echo "=========================================="
echo "Testing Master Data API"
echo "=========================================="
echo ""

# Test Master Data API
echo "Making request to: http://localhost:8080/api/v1/master-data"
echo ""

response=$(curl -s http://localhost:8080/api/v1/master-data)

echo "Response:"
echo "$response" | jq . 2>/dev/null || echo "$response"

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="

if command -v jq &> /dev/null; then
    partners_count=$(echo "$response" | jq '.partners | length' 2>/dev/null)
    products_count=$(echo "$response" | jq '.products | length' 2>/dev/null)
    branches_count=$(echo "$response" | jq '.branches | length' 2>/dev/null)
    
    echo "✅ Partners: $partners_count"
    echo "✅ Products: $products_count"
    echo "✅ Branches: $branches_count"
else
    echo "Install 'jq' for better output formatting"
fi
