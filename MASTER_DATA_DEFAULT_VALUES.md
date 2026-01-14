# Master Data - Default Values

## Overview

This document lists all default master data that is automatically seeded when the application starts.

The data is seeded via Flyway migrations and is **idempotent** - running migrations multiple times will not create duplicates.

---

## Partners (6 Total)

All partners are marked as **ACTIVE** by default.

| Code | Name |
|------|------|
| `SAMASTA` | Samasta Microfinance |
| `SONATA` | Sonata Finance |
| `UJJIVAN` | Ujjivan Small Finance Bank |
| `BANDHAN` | Bandhan Bank |
| `EQUITAS` | Equitas Small Finance Bank |
| `FINCARE` | Fincare Small Finance Bank |

**Migration:** V4 (SAMASTA, SONATA), V7 (rest)

---

## Products (8 Total)

All products are marked as **ACTIVE** by default.

| Code | Name |
|------|------|
| `ENTREPRENEURIAL` | Entrepreneurial Loan |
| `JLG` | Joint Liability Group Loan |
| `HOME_LOAN` | Home Loan |
| `PERSONAL_LOAN` | Personal Loan |
| `VEHICLE_LOAN` | Vehicle Loan |
| `GOLD_LOAN` | Gold Loan |
| `MSME_LOAN` | MSME Loan |
| `AGRICULTURE_LOAN` | Agriculture Loan |

**Migration:** V4 (ENTREPRENEURIAL, JLG), V7 (rest)

---

## Branches (18 Total)

All branches are marked as **ACTIVE** by default.

### SAMASTA Branches (4)

| Code | Name | Partner Code |
|------|------|--------------|
| `MIYAPUR` | Miyapur Branch | SAMASTA |
| `KUKATPALLY` | Kukatpally Branch | SAMASTA |
| `AMEERPET` | Ameerpet Branch | SAMASTA |
| `KONDAPUR` | Kondapur Branch | SAMASTA |

### SONATA Branches (4)

| Code | Name | Partner Code |
|------|------|--------------|
| `MADHAPUR` | Madhapur Branch | SONATA |
| `GACHIBOWLI` | Gachibowli Branch | SONATA |
| `HITECH_CITY` | Hitech City Branch | SONATA |
| `BANJARA_HILLS` | Banjara Hills Branch | SONATA |

### UJJIVAN Branches (3)

| Code | Name | Partner Code |
|------|------|--------------|
| `SECUNDERABAD` | Secunderabad Branch | UJJIVAN |
| `JUBILEE_HILLS` | Jubilee Hills Branch | UJJIVAN |
| `BEGUMPET` | Begumpet Branch | UJJIVAN |

### BANDHAN Branches (2)

| Code | Name | Partner Code |
|------|------|--------------|
| `LB_NAGAR` | LB Nagar Branch | BANDHAN |
| `DILSUKHNAGAR` | Dilsukhnagar Branch | BANDHAN |

### EQUITAS Branches (2)

| Code | Name | Partner Code |
|------|------|--------------|
| `ATTAPUR` | Attapur Branch | EQUITAS |
| `MEHDIPATNAM` | Mehdipatnam Branch | EQUITAS |

### FINCARE Branches (2)

| Code | Name | Partner Code |
|------|------|--------------|
| `UPPAL` | Uppal Branch | FINCARE |
| `KOMPALLY` | Kompally Branch | FINCARE |

**Migration:** V6 (first 4), V7 (rest)

---

## API Response Example

### Full Master Data Response

```json
{
  "partners": [
    {"code": "BANDHAN", "name": "Bandhan Bank"},
    {"code": "EQUITAS", "name": "Equitas Small Finance Bank"},
    {"code": "FINCARE", "name": "Fincare Small Finance Bank"},
    {"code": "SAMASTA", "name": "Samasta Microfinance"},
    {"code": "SONATA", "name": "Sonata Finance"},
    {"code": "UJJIVAN", "name": "Ujjivan Small Finance Bank"}
  ],
  "products": [
    {"code": "AGRICULTURE_LOAN", "name": "Agriculture Loan"},
    {"code": "ENTREPRENEURIAL", "name": "Entrepreneurial Loan"},
    {"code": "GOLD_LOAN", "name": "Gold Loan"},
    {"code": "HOME_LOAN", "name": "Home Loan"},
    {"code": "JLG", "name": "Joint Liability Group Loan"},
    {"code": "MSME_LOAN", "name": "MSME Loan"},
    {"code": "PERSONAL_LOAN", "name": "Personal Loan"},
    {"code": "VEHICLE_LOAN", "name": "Vehicle Loan"}
  ],
  "branches": [
    {"code": "AMEERPET", "name": "Ameerpet Branch", "partnerCode": "SAMASTA"},
    {"code": "ATTAPUR", "name": "Attapur Branch", "partnerCode": "EQUITAS"},
    {"code": "BANJARA_HILLS", "name": "Banjara Hills Branch", "partnerCode": "SONATA"},
    {"code": "BEGUMPET", "name": "Begumpet Branch", "partnerCode": "UJJIVAN"},
    {"code": "DILSUKHNAGAR", "name": "Dilsukhnagar Branch", "partnerCode": "BANDHAN"},
    {"code": "GACHIBOWLI", "name": "Gachibowli Branch", "partnerCode": "SONATA"},
    {"code": "HITECH_CITY", "name": "Hitech City Branch", "partnerCode": "SONATA"},
    {"code": "JUBILEE_HILLS", "name": "Jubilee Hills Branch", "partnerCode": "UJJIVAN"},
    {"code": "KOMPALLY", "name": "Kompally Branch", "partnerCode": "FINCARE"},
    {"code": "KONDAPUR", "name": "Kondapur Branch", "partnerCode": "SAMASTA"},
    {"code": "KUKATPALLY", "name": "Kukatpally Branch", "partnerCode": "SAMASTA"},
    {"code": "LB_NAGAR", "name": "LB Nagar Branch", "partnerCode": "BANDHAN"},
    {"code": "MADHAPUR", "name": "Madhapur Branch", "partnerCode": "SONATA"},
    {"code": "MEHDIPATNAM", "name": "Mehdipatnam Branch", "partnerCode": "EQUITAS"},
    {"code": "MIYAPUR", "name": "Miyapur Branch", "partnerCode": "SAMASTA"},
    {"code": "SECUNDERABAD", "name": "Secunderabad Branch", "partnerCode": "UJJIVAN"},
    {"code": "UPPAL", "name": "Uppal Branch", "partnerCode": "FINCARE"}
  ]
}
```

**Note:** Data is sorted alphabetically by name.

---

## Testing

### Get All Master Data

```bash
curl http://localhost:8080/api/v1/master-data | jq .
```

### Count Records

```bash
# Partners
curl -s http://localhost:8080/api/v1/master-data | jq '.partners | length'
# Expected: 6

# Products
curl -s http://localhost:8080/api/v1/master-data | jq '.products | length'
# Expected: 8

# Branches
curl -s http://localhost:8080/api/v1/master-data | jq '.branches | length'
# Expected: 18
```

### Filter Branches by Partner

```bash
# Get all SAMASTA branches
curl -s http://localhost:8080/api/v1/master-data | \
  jq '.branches[] | select(.partnerCode == "SAMASTA")'
```

---

## Use Cases

### Config Builder (Web)

Use this data to populate dropdowns when creating configurations:

```javascript
// Fetch master data once on app load
const masterData = await fetch('/api/v1/master-data').then(r => r.json());

// Populate partner dropdown
const partnerDropdown = masterData.partners.map(p => ({
  value: p.code,
  label: p.name
}));

// Populate product dropdown
const productDropdown = masterData.products.map(p => ({
  value: p.code,
  label: p.name
}));

// Populate branch dropdown (filtered by selected partner)
const branchDropdown = masterData.branches
  .filter(b => b.partnerCode === selectedPartner)
  .map(b => ({
    value: b.code,
    label: b.name
  }));
```

### Android App (Runtime)

Cache master data locally and use for scope resolution:

```kotlin
// Fetch and cache on app startup
val masterData = apiService.getMasterData()
preferences.saveMasterData(masterData)

// Use for validation
fun isValidPartner(code: String): Boolean {
  return masterData.partners.any { it.code == code }
}

// Get branch name for display
fun getBranchName(code: String): String {
  return masterData.branches.find { it.code == code }?.name ?: code
}
```

### Dashboard (Web/Android)

Display available options:

```kotlin
// Show all partners
masterData.partners.forEach { partner ->
  addPartnerCard(partner.code, partner.name)
}

// Filter flows by partner
val samastaBranches = masterData.branches.filter { 
  it.partnerCode == "SAMASTA" 
}
```

---

## Data Maintenance

### Adding New Records (Future)

While CRUD APIs are not implemented in MVP, you can add records via SQL:

```sql
-- Add new partner
INSERT INTO PARTNERS (PARTNER_CODE, PARTNER_NAME, IS_ACTIVE, CREATED_AT)
VALUES ('NEW_PARTNER', 'New Partner Name', TRUE, CURRENT_TIMESTAMP);

-- Add new product
INSERT INTO PRODUCTS (PRODUCT_CODE, PRODUCT_NAME, IS_ACTIVE, CREATED_AT)
VALUES ('NEW_PRODUCT', 'New Product Name', TRUE, CURRENT_TIMESTAMP);

-- Add new branch
INSERT INTO BRANCHES (BRANCH_CODE, BRANCH_NAME, PARTNER_CODE, IS_ACTIVE, CREATED_AT, UPDATED_AT)
VALUES ('NEW_BRANCH', 'New Branch Name', 'SAMASTA', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

After adding records, restart the application to clear the cache.

### Deactivating Records

```sql
-- Deactivate a partner (won't appear in API response)
UPDATE PARTNERS SET IS_ACTIVE = FALSE WHERE PARTNER_CODE = 'SAMASTA';

-- Deactivate a product
UPDATE PRODUCTS SET IS_ACTIVE = FALSE WHERE PRODUCT_CODE = 'JLG';

-- Deactivate a branch
UPDATE BRANCHES SET IS_ACTIVE = FALSE WHERE BRANCH_CODE = 'MIYAPUR';
```

After updates, restart the application to clear the cache.

---

## Summary

- **6 Partners** across different financial institutions
- **8 Products** covering various loan types
- **18 Branches** distributed across 6 partners
- All records are **ACTIVE** by default
- Data is **sorted alphabetically** by name
- Data is **cached** for performance
- **Ready for testing** scope resolution and filtering

This comprehensive default data provides a realistic testing environment for all client applications! 🎯
