# Master Data API Documentation

## Overview

The Master Data API provides a **single consolidated endpoint** to fetch all master data required by:
- **Config Builder** (Web Application)
- **Android App** (Runtime & Dashboard)

This API eliminates duplication and provides a clean, stable interface for master data access.

---

## API Endpoint

```
GET /api/v1/master-data
```

### Authentication
- **Current**: No authentication required
- **Future**: JWT authentication can be added at filter level without API changes

---

## Response Format

### Success Response (HTTP 200)

```json
{
  "partners": [
    {
      "code": "SAMASTA",
      "name": "Samasta Microfinance"
    },
    {
      "code": "SONATA",
      "name": "Sonata Finance"
    }
  ],
  "products": [
    {
      "code": "ENTREPRENEURIAL",
      "name": "Entrepreneurial Loan"
    },
    {
      "code": "JLG",
      "name": "Joint Liability Group Loan"
    }
  ],
  "branches": [
    {
      "code": "MIYAPUR",
      "name": "Miyapur Branch",
      "partnerCode": "SAMASTA"
    },
    {
      "code": "KUKATPALLY",
      "name": "Kukatpally Branch",
      "partnerCode": "SAMASTA"
    },
    {
      "code": "MADHAPUR",
      "name": "Madhapur Branch",
      "partnerCode": "SONATA"
    },
    {
      "code": "GACHIBOWLI",
      "name": "Gachibowli Branch",
      "partnerCode": "SONATA"
    }
  ]
}
```

### Empty Data Response (HTTP 200)

If no master data exists (e.g., fresh database), the API returns empty arrays:

```json
{
  "partners": [],
  "products": [],
  "branches": []
}
```

**Note**: This API **never returns 404**. It always returns HTTP 200 with empty arrays if no data is found.

---

## Features

### ✅ Single Consolidated API
- One endpoint for all master data
- No need for multiple API calls
- Reduces network overhead

### ✅ Active Records Only
- Returns only records where `is_active = true`
- Inactive records are filtered out automatically

### ✅ Sorted by Name
- Partners: Sorted by `partner_name` (ascending)
- Products: Sorted by `product_name` (ascending)
- Branches: Sorted by `branch_name` (ascending)

### ✅ Performance Optimized
- Results are **cached in-memory**
- Cache is evicted only on application restart
- Ideal for READ-HEAVY, WRITE-RARE data

### ✅ Backward Compatible
- Does not affect existing APIs
- Can be consumed independently
- No breaking changes

---

## Database Schema

### 1. Partners Table

```sql
CREATE TABLE PARTNERS (
    PARTNER_CODE VARCHAR(50) PRIMARY KEY,
    PARTNER_NAME VARCHAR(255) NOT NULL,
    IS_ACTIVE BOOLEAN NOT NULL DEFAULT TRUE,
    CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 2. Products Table

```sql
CREATE TABLE PRODUCTS (
    PRODUCT_CODE VARCHAR(50) PRIMARY KEY,
    PRODUCT_NAME VARCHAR(255) NOT NULL,
    IS_ACTIVE BOOLEAN NOT NULL DEFAULT TRUE,
    CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3. Branches Table

```sql
CREATE TABLE BRANCHES (
    BRANCH_CODE VARCHAR(50) PRIMARY KEY,
    BRANCH_NAME VARCHAR(255) NOT NULL,
    PARTNER_CODE VARCHAR(50) NOT NULL,
    IS_ACTIVE BOOLEAN NOT NULL DEFAULT TRUE,
    CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## Default Master Data (MVP)

The following data is seeded automatically via Flyway migrations:

### Partners
- **SAMASTA** - Samasta Microfinance
- **SONATA** - Sonata Finance

### Products
- **ENTREPRENEURIAL** - Entrepreneurial Loan
- **JLG** - Joint Liability Group Loan

### Branches
- **MIYAPUR** - Miyapur Branch (SAMASTA)
- **KUKATPALLY** - Kukatpally Branch (SAMASTA)
- **MADHAPUR** - Madhapur Branch (SONATA)
- **GACHIBOWLI** - Gachibowli Branch (SONATA)

---

## Usage Examples

### Using cURL

```bash
curl -X GET http://localhost:8080/api/v1/master-data
```

### Using JavaScript (Fetch API)

```javascript
fetch('http://localhost:8080/api/v1/master-data')
  .then(response => response.json())
  .then(data => {
    console.log('Partners:', data.partners);
    console.log('Products:', data.products);
    console.log('Branches:', data.branches);
  });
```

### Using Android (Retrofit)

```kotlin
@GET("/api/v1/master-data")
suspend fun getMasterData(): MasterDataResponse
```

---

## Implementation Details

### Architecture

```
Controller (MasterDataController)
    ↓
Service (MasterDataService) [Cached]
    ↓
Repositories (PartnerRepository, ProductRepository, BranchRepository)
    ↓
Database (H2 / MySQL)
```

### Key Components

1. **MasterDataController** (`/api/v1/master-data`)
   - Exposes REST endpoint
   - Swagger/OpenAPI documented

2. **MasterDataService**
   - Fetches data from repositories
   - Transforms entities to DTOs
   - Applies caching

3. **Repositories**
   - PartnerRepository
   - ProductRepository
   - BranchRepository

4. **Entities**
   - Partner
   - Product
   - Branch

5. **DTOs**
   - MasterDataResponse
   - PartnerDTO
   - ProductDTO
   - BranchDTO

---

## Caching Strategy

### Configuration
- **Cache Provider**: Spring Boot's default (ConcurrentMap)
- **Cache Name**: `masterData`
- **Cache Key**: `all`
- **Eviction**: Application restart only

### Why Caching?
- Master data is **READ-HEAVY**
- Master data is **WRITE-RARE**
- Improves API response time
- Reduces database load

### Cache Behavior
1. **First request**: Hits database, caches result
2. **Subsequent requests**: Returns cached data
3. **Application restart**: Cache cleared, fresh data loaded

---

## Error Handling

| Scenario | Response Code | Response Body |
|----------|---------------|---------------|
| Success with data | 200 | JSON with all master data |
| Success, no data | 200 | JSON with empty arrays |
| Server error | 500 | Standard error response |

---

## Future Enhancements (Out of MVP Scope)

### Phase 2: CRUD Operations
- POST `/api/v1/master-data/partners` - Create partner
- PUT `/api/v1/master-data/partners/{code}` - Update partner
- DELETE `/api/v1/master-data/partners/{code}` - Deactivate partner
- Similar for Products and Branches

### Phase 3: Filtering
- GET `/api/v1/master-data?type=partners` - Fetch only partners
- GET `/api/v1/master-data?partnerCode=SAMASTA` - Filter branches by partner

### Phase 4: Versioning
- Support for API versioning (v1, v2)
- Maintain backward compatibility

---

## Testing

### Manual Testing (Postman/cURL)

```bash
# Get all master data
curl -X GET http://localhost:8080/api/v1/master-data

# Verify response format
curl -X GET http://localhost:8080/api/v1/master-data | jq .
```

### Swagger UI

Access interactive API documentation:
```
http://localhost:8080/swagger-ui/index.html
```

Navigate to **Master Data** section and test the API.

---

## Migration Scripts

### V1: Create Partners and Products Tables
File: `V1__create_master_data_tables.sql`

### V4: Seed Partners and Products
File: `V4__seed_master_data.sql`

### V6: Create Branches Table and Seed Data
File: `V6__create_branches_table.sql`

All migrations are **idempotent** and use `MERGE INTO` to prevent duplicate inserts.

---

## Important Notes

### ✅ DO's
- Use this API for all master data needs
- Cache results on client side if needed
- Handle empty arrays gracefully

### ❌ DON'Ts
- **Don't** create individual APIs per master type
- **Don't** hardcode master data in client apps
- **Don't** expect 404 when no data exists
- **Don't** bypass this API and query database directly

---

## Summary

The Master Data API provides a **clean, stable, and performant** way to access all master data in the LOS system. It's designed for:

- **Simplicity**: Single endpoint, single response
- **Performance**: Cached results, minimal database hits
- **Stability**: No breaking changes, backward compatible
- **Scalability**: Ready for future enhancements

This API is the **single source of truth** for Partners, Products, and Branches across all client applications.
