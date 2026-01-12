# Dashboard API Implementation

## Overview
Created a READ-ONLY Dashboard API to list all available flows for a given product/partner/branch combination. This API is used by Android home screen and Web dashboard to display flow tiles.

## Changes Made

### 1. FlowConfig Entity (`config/entity/FlowConfig.java`)

**Added Field:**
- `dashboardMeta`: JSON field containing UI metadata (title, description, icon)

```java
@Column(name = "dashboard_meta", columnDefinition = "TEXT")
@Convert(converter = JsonConverter.class)
private Map<String, Object> dashboardMeta;
```

**Dashboard Metadata Structure:**
```json
{
  "title": "Applicant Onboarding",
  "description": "Capture applicant personal and business details",
  "icon": "APPLICANT_ONBOARDING"
}
```

### 2. Dashboard DTOs (`dto/dashboard/`)

#### DashboardFlowResponse
Contains UI-ready metadata for a single flow:
- `flowId` - Flow identifier (used to start the flow)
- `title` - Display title
- `description` - Brief description
- `icon` - Icon identifier
- `productCode`, `partnerCode`, `branchCode` - Scope info
- `status` - Current status (ACTIVE)
- `startable` - Whether the flow can be started (always `true` for active flows)

#### DashboardFlowsResponse
Wrapper containing list of flows:
- `flows` - List of `DashboardFlowResponse`

### 3. FlowConfigRepository (`repository/FlowConfigRepository.java`)

**New Method: `findAllActiveByScope()`**
- Fetches all ACTIVE flows for a given product/partner/branch
- Uses scope resolution: BRANCH > PARTNER > PRODUCT
- Orders by flowId and scope specificity

```java
@Query("""
    SELECT fc FROM FlowConfig fc 
    WHERE fc.status = 'ACTIVE'
    AND (
        (fc.branchCode = :branchCode AND fc.partnerCode = :partnerCode AND fc.productCode = :productCode)
        OR (fc.branchCode IS NULL AND fc.partnerCode = :partnerCode AND fc.productCode = :productCode)
        OR (fc.branchCode IS NULL AND fc.partnerCode IS NULL AND fc.productCode = :productCode)
    )
    ORDER BY fc.flowId, fc.branchCode DESC NULLS LAST, fc.partnerCode DESC NULLS LAST
    """)
List<FlowConfig> findAllActiveByScope(
    @Param("productCode") String productCode,
    @Param("partnerCode") String partnerCode,
    @Param("branchCode") String branchCode
);
```

### 4. DashboardService (`service/DashboardService.java`)

**Main Method: `getAvailableFlows()`**
1. Fetches all ACTIVE flows from repository
2. Deduplicates by flowId (keeps most specific scope)
3. Maps to DTOs with dashboard metadata
4. Returns UI-ready response

**Scope Resolution Logic:**
- For each unique `flowId`, keeps only the most specific config
- Precedence: BRANCH > PARTNER > PRODUCT
- Ensures no duplicate flows in the dashboard

**Default Values:**
- Title: Falls back to `flowId` if not in metadata
- Description: "Flow description not available"
- Icon: "DEFAULT"

### 5. DashboardController (`controller/DashboardController.java`)

**Endpoint:**
```
GET /api/v1/dashboard/flows
```

**Query Parameters:**
- `productCode` (optional) - Product code. If not provided, returns ALL flows (testing mode)
- `partnerCode` (optional) - Partner code. If not provided, returns ALL flows (testing mode)
- `branchCode` (optional) - Branch code

**Response:**
```json
{
  "flows": [
    {
      "flowId": "APPLICANT_FLOW",
      "title": "Applicant & Co-Applicant Onboarding",
      "description": "Capture applicant personal and business details",
      "icon": "APPLICANT_ONBOARDING",
      "productCode": "ENTREPRENEURIAL",
      "partnerCode": "SAMASTA",
      "branchCode": null,
      "status": "ACTIVE",
      "startable": true
    }
  ]
}
```

### 6. Database Migration (`db/migration/V5__add_dashboard_meta.sql`)

Adds `dashboard_meta` column to `flow_configs` table:
```sql
ALTER TABLE FLOW_CONFIGS ADD COLUMN DASHBOARD_META TEXT;
```

## API Usage

### Example 1: Get ALL Flows (Testing Mode)
```bash
GET /api/v1/dashboard/flows
```

**Behavior:**
- No query parameters provided
- Returns ALL ACTIVE flows in the system
- Useful for testing and development

**Response:**
```json
{
  "flows": [
    {
      "flowId": "loan_application",
      "title": "Home Loan Application",
      "description": "Complete home loan application process",
      "icon": "HOME_LOAN",
      "productCode": "HOME_LOAN",
      "partnerCode": "PARTNER_001",
      "branchCode": null,
      "status": "ACTIVE",
      "startable": true
    },
    {
      "flowId": "kyc_flow",
      "title": "KYC Verification",
      "description": "Complete KYC and document upload",
      "icon": "KYC",
      "productCode": "PERSONAL_LOAN",
      "partnerCode": "PARTNER_002",
      "branchCode": null,
      "status": "ACTIVE",
      "startable": true
    }
  ]
}
```

### Example 2: Get Flows for Product and Partner
```bash
GET /api/v1/dashboard/flows?productCode=HOME_LOAN&partnerCode=PARTNER_001
```

**Response:**
```json
{
  "flows": [
    {
      "flowId": "loan_application",
      "title": "Home Loan Application",
      "description": "Complete home loan application process",
      "icon": "HOME_LOAN",
      "productCode": "HOME_LOAN",
      "partnerCode": "PARTNER_001",
      "branchCode": null,
      "status": "ACTIVE",
      "startable": true
    },
    {
      "flowId": "kyc_flow",
      "title": "KYC Verification",
      "description": "Complete KYC and document upload",
      "icon": "KYC",
      "productCode": "HOME_LOAN",
      "partnerCode": "PARTNER_001",
      "branchCode": null,
      "status": "ACTIVE",
      "startable": true
    }
  ]
}
```

### Example 3: Get Flows with Branch Specificity
```bash
GET /api/v1/dashboard/flows?productCode=HOME_LOAN&partnerCode=PARTNER_001&branchCode=BRANCH_001
```

**Behavior:**
- If branch-specific flow exists, it will be returned
- Otherwise, falls back to partner-level or product-level flow
- Only one config per flowId is returned (most specific)

## Frontend Integration

### Android/Web Dashboard Flow

```typescript
// 1. Fetch available flows
const response = await fetch(
  '/api/v1/dashboard/flows?productCode=HOME_LOAN&partnerCode=PARTNER_001'
);
const { flows } = await response.json();

// 2. Render flow tiles
flows.forEach(flow => {
  renderFlowTile({
    title: flow.title,
    description: flow.description,
    icon: flow.icon,
    onClick: () => startFlow(flow.flowId)
  });
});

// 3. Start flow when user clicks a tile
async function startFlow(flowId) {
  const startResponse = await fetch('/api/v1/runtime/next-screen', {
    method: 'POST',
    body: JSON.stringify({
      applicationId: null,
      currentScreenId: null,        // Flow start
      formData: {},
      flowId: flowId,
      productCode: 'HOME_LOAN',
      partnerCode: 'PARTNER_001',
      branchCode: 'BRANCH_001'
    })
  });
  
  const { nextScreenId, screenConfig } = await startResponse.json();
  renderScreen(nextScreenId, screenConfig);
}
```

## Configuration Example

### Creating a Flow with Dashboard Metadata

When creating/activating a FlowConfig, include `dashboardMeta`:

```json
{
  "flowId": "loan_application",
  "productCode": "HOME_LOAN",
  "partnerCode": "PARTNER_001",
  "status": "ACTIVE",
  "flowDefinition": {
    "startScreen": "applicant_details",
    "screens": {
      "applicant_details": { ... },
      "employment_details": { ... }
    }
  },
  "dashboardMeta": {
    "title": "Home Loan Application",
    "description": "Complete home loan application in 5 easy steps",
    "icon": "HOME_LOAN"
  }
}
```

### Updating Dashboard Metadata

To update metadata without changing flow logic:
1. Create new version with updated `dashboardMeta`
2. Activate the new version
3. Dashboard will immediately reflect the changes

## Security

- **Authentication:** Currently DISABLED for testing
- **Authorization:** Not implemented yet
- **Future:** JWT-based auth with role checks

## Benefits

### ✅ Backend as Source of Truth
- Frontend doesn't need to know which flows exist
- All flow metadata managed in backend
- Easy to add/remove flows without app updates

### ✅ Scope Resolution
- Different flows for different partners/branches
- Automatic precedence handling
- No duplicate flows in UI

### ✅ Clean Separation
- Dashboard API: READ-ONLY, lists flows
- Runtime API: Executes flows
- Single responsibility for each API

### ✅ UI-Ready Response
- No frontend mapping needed
- Direct binding to UI components
- Includes all necessary metadata

## Testing Checklist

- [x] Compilation successful
- [x] No linter errors
- [x] Migration file created
- [ ] Test with sample flow configs
- [ ] Verify scope resolution works correctly
- [ ] Test with missing dashboardMeta (should use defaults)
- [ ] Test with branch-specific flows
- [ ] Verify only ACTIVE flows are returned
- [ ] Integration test with Runtime API

## Next Steps

1. **Add Dashboard Metadata to Existing Flows:**
   - Update flow configs with title, description, icon
   - Activate updated versions

2. **Test Dashboard API:**
   ```bash
   curl -X GET "http://localhost:8080/api/v1/dashboard/flows?productCode=HOME_LOAN&partnerCode=PARTNER_001"
   ```

3. **Integrate with Frontend:**
   - Android: Display flow tiles on home screen
   - Web: Show flows in dashboard/config builder

4. **Add Icons:**
   - Define icon mappings in frontend
   - Create icon components/assets

## API Contract Summary

### Request
```
GET /api/v1/dashboard/flows
  ?productCode={code}      # Required
  &partnerCode={code}      # Required
  &branchCode={code}       # Optional
```

### Response
```json
{
  "flows": [
    {
      "flowId": "string",
      "title": "string",
      "description": "string",
      "icon": "string",
      "productCode": "string",
      "partnerCode": "string",
      "branchCode": "string|null",
      "status": "ACTIVE",
      "startable": true
    }
  ]
}
```

### Error Responses
- `400 Bad Request` - Missing required parameters
- `500 Internal Server Error` - Server error

## Summary

The Dashboard API provides a clean, read-only interface for listing available flows:
- ✅ Single endpoint for all flow discovery
- ✅ Scope resolution with automatic precedence
- ✅ UI-ready metadata (title, description, icon)
- ✅ No breaking changes to existing APIs
- ✅ Frontend remains a pure renderer
- ✅ Backend maintains complete control

**Result:** Android and Web apps can now dynamically discover and display available flows without hardcoding, with all logic handled by the backend.
