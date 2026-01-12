# Configuration Activation & Runtime System

## ✅ IMPLEMENTED: Enterprise-Grade Config Lifecycle Management

### Overview
Complete configuration activation system with atomic transitions, runtime safety, and immutable flow snapshots.

---

## 🎯 Core Principles

### 1. **Config Lifecycle**
```
DRAFT → ACTIVE → DEPRECATED
  ↑       |          |
  |       |          |
  └───────┴──────────┘
     (clone resets to DRAFT)
```

### 2. **Status Definitions**

| Status | Description | Editable | Runtime Use | Auto-transition |
|--------|-------------|----------|-------------|-----------------|
| **DRAFT** | Under development | ✅ Yes | ❌ Never | Default for new configs |
| **ACTIVE** | Production use | ❌ No | ✅ Yes | Via activation API |
| **DEPRECATED** | Superseded | ❌ No | ⚠️ Existing snapshots only | Auto when new activated |
| **INACTIVE** | Manually disabled | ❌ No | ❌ Never | Manual change |

### 3. **Key Rules**

✅ Only **ONE ACTIVE** config per scope  
✅ Only **DRAFT** configs can be activated  
✅ Activation is **atomic** (deprecate old → activate new)  
✅ **DRAFT configs NEVER used** at runtime  
✅ **FlowSnapshots are immutable**  
✅ Config changes only affect **NEW applications**  

---

## 🚀 Common Activation API

### Endpoints

```bash
# Activate Screen Config
POST /api/v1/configs/screens/{configId}/activate

# Activate Flow Config
POST /api/v1/configs/flows/{configId}/activate

# Activate Field Mapping Config
POST /api/v1/configs/mappings/{configId}/activate
```

### Example: Activate Screen Config

```bash
curl -X POST http://localhost:8080/api/v1/configs/screens/123/activate \
  -H "Authorization: Bearer {token}"
```

**Response:**
```json
{
  "configId": 123,
  "screenId": "kyc-001",
  "status": "ACTIVE",  ← Changed from DRAFT
  "version": 1,
  "productCode": "ENTREPRENEURIAL",
  "partnerCode": "SAMASTA",
  "branchCode": "MH001"
}
```

---

## 🔐 Activation Rules & Validation

### Pre-Activation Validation

**1. Status Check:**
- ✅ Must be DRAFT  
- ❌ Cannot activate ACTIVE (idempotent - returns existing)  
- ❌ Cannot activate DEPRECATED or INACTIVE  

**2. Completeness Check:**
- ✅ ScreenConfig: screenId + uiConfig must be non-empty  
- ✅ FlowConfig: flowId + flowDefinition must be non-empty  
- ✅ FieldMappingConfig: screenId + mappings must be non-empty  

**3. Scope Uniqueness:**
- Only ONE ACTIVE per scope combination  
- Scope = identifier + productCode + partnerCode + branchCode  

### Atomic Activation Process

```
Transaction Start
    ├─ 1. Load config to activate
    ├─ 2. Validate status (must be DRAFT)
    ├─ 3. Validate completeness
    ├─ 4. Find existing ACTIVE in same scope
    ├─ 5. Set existing ACTIVE → DEPRECATED
    ├─ 6. Set new config → ACTIVE
    └─ 7. Save both (atomic commit)
Transaction End
```

**If any step fails → entire transaction rolls back**

---

## 🎯 Scope Resolution

### What is Scope?

Scope defines the context for config selection:
- **Product Level**: All partners, all branches
- **Partner Level**: Specific partner, all branches  
- **Branch Level**: Specific branch (most specific)

### Resolution Priority

```
Branch Config     (highest priority)
  ↓
Partner Config    (if no branch match)
  ↓
Product Config    (fallback)
```

### Example

**Scenario:** Get screen config for KYC screen

**Available Configs:**
1. ConfigID=1: screenId=KYC, product=LOAN, partner=NULL, branch=NULL, status=ACTIVE
2. ConfigID=2: screenId=KYC, product=LOAN, partner=SAMASTA, branch=NULL, status=ACTIVE
3. ConfigID=3: screenId=KYC, product=LOAN, partner=SAMASTA, branch=MH001, status=ACTIVE

**Request:** `product=LOAN, partner=SAMASTA, branch=MH001`  
**Result:** ConfigID=3 (exact match - highest priority)

**Request:** `product=LOAN, partner=SAMASTA, branch=MH002`  
**Result:** ConfigID=2 (partner match - branch not found)

**Request:** `product=LOAN, partner=SONATA, branch=NULL`  
**Result:** ConfigID=1 (product match - partner not found)

---

## 📦 Flow Snapshot System

### Purpose
Ensures **immutability** for in-progress loan applications.

### When Created
- Application start
- First screen render
- Created ONCE per application

### What's Captured
```json
{
  "snapshotId": 456,
  "applicationId": 789,
  "flowConfigId": 10,
  "snapshotData": {
    "flowDefinition": {...},      // FlowConfig at creation time
    "screenConfigs": {
      "kyc-001": {
        "configId": 1,
        "version": 1,
        "uiConfig": {...}          // ScreenConfig at creation time
      }
    },
    "fieldMappings": {...},        // FieldMappingConfig at creation time
    "validationRules": {...}       // ValidationConfig at creation time
  },
  "createdAt": "2026-01-12T10:00:00Z"
}
```

### Immutability Rules

✅ **Snapshot is FROZEN**  
- Contains config versions at creation time  
- Never updated during application lifecycle  

✅ **Config Updates → New Apps Only**  
- Activate new config → affects NEW applications  
- Existing applications continue with their snapshot  

✅ **Back Button Safe**  
- User navigates back → uses SAME snapshot  
- No config drift  
- Predictable behavior  

---

## 🔄 Runtime Behavior

### Scenario 1: New Application

```
User starts loan application
    ↓
System resolves ACTIVE configs (current state)
    ├─ FlowConfig (ACTIVE)
    ├─ ScreenConfigs (ACTIVE)
    ├─ FieldMappingConfigs (ACTIVE)
    └─ ValidationConfigs (ACTIVE)
    ↓
Create FlowSnapshot (immutable)
    ↓
Application uses THIS snapshot forever
```

### Scenario 2: Config Update During In-Progress Application

```
Time T1: Application A starts
    ↓ Creates snapshot with ConfigV1
    
Time T2: Admin activates ConfigV2
    ↓ ConfigV1 → DEPRECATED
    ↓ ConfigV2 → ACTIVE
    
Time T3: Application A continues
    ✅ Still uses ConfigV1 from snapshot
    
Time T4: New Application B starts
    ✅ Uses ConfigV2 (latest ACTIVE)
```

**Result:**  
- Application A: Unaffected by config change  
- Application B: Gets latest config  
- No runtime surprises  
- Audit trail preserved (ConfigV1 still exists as DEPRECATED)  

### Scenario 3: Back Button Navigation

```
User on Screen 3
    ↓ Clicks Back
    ↓ Returns to Screen 2
    ↓ Edits data
    ↓ Clicks Next
    ↓ Flow re-evaluates using SAME snapshot
    
✅ Uses original FlowSnapshot
❌ Does NOT fetch new ACTIVE configs
```

---

## 🛠️ Implementation Details

### Files Created

1. **`ConfigStatus.java`** - Enhanced enum with DEPRECATED
2. **`ConfigActivationService.java`** - Activation logic
3. **`ConfigActivationController.java`** - REST API
4. **`ConfigResolutionService.java`** - Enhanced with safety checks

### Files Updated

1. **Repositories** - Added findByExactScope methods
2. **ConfigStatusValidator.java`** - Updated for DEPRECATED
3. **DTOs** - No changes (backward compatible)

### Database Impact

**No schema changes required!**  
- Uses existing `status` column  
- `DEPRECATED` is just a new status value  
- Existing data unaffected  

---

## 📊 API Examples

### 1. Create DRAFT Config

```bash
POST /api/v1/configs/screens
{
  "screenId": "kyc-001",
  "productCode": "ENTREPRENEURIAL",
  "partnerCode": "SAMASTA",
  "uiConfig": {"title": "KYC Screen"}
  // status omitted → defaults to DRAFT
}

# Response: status = "DRAFT"
```

### 2. Activate Config

```bash
POST /api/v1/configs/screens/123/activate

# Response:
{
  "configId": 123,
  "status": "ACTIVE",  ← Changed
  ...
}

# Previous ACTIVE config (if any) is now DEPRECATED
```

### 3. Attempt Invalid Activation

```bash
POST /api/v1/configs/screens/456/activate
# Where configId=456 has status=DEPRECATED

# Error Response:
{
  "message": "Cannot activate config with status DEPRECATED. Only DRAFT configs can be activated.",
  "code": "VALIDATION_ERROR"
}
```

### 4. Idempotent Activation

```bash
POST /api/v1/configs/screens/123/activate
# Where configId=123 is already ACTIVE

# Response: Returns same config (no error)
{
  "configId": 123,
  "status": "ACTIVE",  ← Already ACTIVE
  ...
}
```

---

## 🔍 Verification & Testing

### Test Activation Flow

```bash
# 1. Create DRAFT config
curl -X POST /api/v1/configs/screens \
  -d '{"screenId":"test","uiConfig":{}}'
# Response: configId=100, status=DRAFT

# 2. Activate it
curl -X POST /api/v1/configs/screens/100/activate
# Response: configId=100, status=ACTIVE

# 3. Create another DRAFT in same scope
curl -X POST /api/v1/configs/screens \
  -d '{"screenId":"test","uiConfig":{}}'
# Response: configId=101, status=DRAFT

# 4. Activate new one
curl -X POST /api/v1/configs/screens/101/activate
# Response: configId=101, status=ACTIVE
# Side effect: configId=100 → DEPRECATED

# 5. Verify old config is deprecated
curl GET /api/v1/configs/screens/100
# Response: configId=100, status=DEPRECATED
```

### Verify Runtime Resolution

```bash
# Get ACTIVE screen config for runtime
GET /api/v1/runtime/screen-config?screenId=test&product=LOAN

# Should return configId=101 (latest ACTIVE)
# Should NEVER return configId=100 (DEPRECATED)
```

---

## ⚠️ Critical Constraints

### What DRAFT Means
- ❌ **NEVER** used at runtime  
- ✅ Can be edited freely  
- ✅ Can be deleted  
- ✅ Can be cloned  
- ✅ Can be activated  

### What ACTIVE Means
- ✅ Used for **NEW** applications  
- ❌ Should NOT be edited (create new DRAFT instead)  
- ❌ Should NOT be deleted (deprecate via activation)  
- ✅ Can be cloned (clone becomes DRAFT)  
- ❌ Cannot be activated again (idempotent)  

### What DEPRECATED Means
- ✅ Preserved for audit  
- ✅ Used by existing FlowSnapshots  
- ❌ NOT used for new applications  
- ❌ Cannot be edited  
- ❌ Cannot be activated  
- ✅ Can be viewed for audit  

### What INACTIVE Means
- ❌ Manually disabled  
- ❌ NOT used anywhere  
- ❌ Cannot be activated (must change to DRAFT first)  

---

## 🎯 Benefits

| Benefit | Description |
|---------|-------------|
| **Safety** | No config drift for in-progress applications |
| **Auditability** | Full history via DEPRECATED configs |
| **Atomic** | Activation is all-or-nothing |
| **Predictable** | Same snapshot throughout application lifecycle |
| **Scalable** | Supports multi-tenant (product/partner/branch) |
| **Testable** | DRAFT configs for safe testing |
| **Rollback-friendly** | Activate old config to rollback |

---

## 📝 Best Practices

### For Config Editors

1. **Never edit ACTIVE configs directly**  
   - Clone → Edit → Activate new version  

2. **Test in DRAFT before activating**  
   - DRAFT configs won't affect production  

3. **Activate during low-traffic windows**  
   - Though safe, minimizes confusion  

### For Developers

1. **Always use ConfigResolutionService**  
   - Never query repositories directly for runtime configs  

2. **Create FlowSnapshot early**  
   - At application start, not mid-flow  

3. **Never modify FlowSnapshot**  
   - Immutable by design  

### For DevOps

1. **Monitor activation events**  
   - Log: `Activated {type} config {id}, deprecated {count} previous`  

2. **Periodic cleanup of old DEPRECATED**  
   - After audit retention period  

3. **Backup before activation**  
   - Though atomic, good practice  

---

## 🚨 Error Scenarios

### Error 1: No ACTIVE Config Found

**Cause:** No ACTIVE config exists for scope  
**Solution:** Activate at least one config for that scope  

### Error 2: Activation of Non-DRAFT

**Cause:** Trying to activate DEPRECATED/INACTIVE config  
**Solution:** Clone it first (creates DRAFT), then activate  

### Error 3: Incomplete Config

**Cause:** Required fields missing  
**Solution:** Edit config to add required data before activation  

---

## ✅ Summary

**Implemented:**
- ✅ DEPRECATED status in lifecycle  
- ✅ Common activation API (/configs/{module}/{id}/activate)  
- ✅ Atomic activation (deprecate old → activate new)  
- ✅ Only ONE ACTIVE per scope enforcement  
- ✅ Runtime resolution (ACTIVE only)  
- ✅ FlowSnapshot immutability  
- ✅ Config isolation (no drift)  

**Constraints:**
- ✅ DRAFT configs NEVER used at runtime  
- ✅ ACTIVE config changes don't affect in-progress apps  
- ✅ FlowSnapshots are immutable  
- ✅ Backward compatible (no API breaking changes)  

**Status:** ✅ **PRODUCTION READY**

---

**Last Updated:** 2026-01-12  
**Version:** 1.0.0  
**Implemented By:** AI Assistant
