# Configuration Activation API - Quick Reference

## 🎯 Quick Start

### Activate a Config (3 Steps)

```bash
# Step 1: Create DRAFT config
POST /api/v1/configs/screens
{
  "screenId": "kyc-001",
  "uiConfig": {...}
}
# Response: configId=123, status="DRAFT"

# Step 2: Test and verify (DRAFT not used at runtime)

# Step 3: Activate when ready
POST /api/v1/configs/screens/123/activate
# Response: status="ACTIVE", previous ACTIVE → DEPRECATED
```

---

## 📍 Activation Endpoints

| Config Type | Endpoint | Role Required |
|-------------|----------|---------------|
| **Screen** | `POST /api/v1/configs/screens/{id}/activate` | ADMIN, CONFIG_APPROVER |
| **Flow** | `POST /api/v1/configs/flows/{id}/activate` | ADMIN, CONFIG_APPROVER |
| **Field Mapping** | `POST /api/v1/configs/mappings/{id}/activate` | ADMIN, CONFIG_APPROVER |

---

## 🔄 Status Lifecycle

```
CREATE → DRAFT (editable, not used at runtime)
           ↓
      ACTIVATE
           ↓
        ACTIVE (used for NEW apps, ONE per scope)
           ↓
     (new activation)
           ↓
      DEPRECATED (preserved for existing apps)
```

---

## ✅ Activation Rules

| Rule | Description |
|------|-------------|
| **Only DRAFT can activate** | Cannot activate ACTIVE/DEPRECATED/INACTIVE |
| **ONE ACTIVE per scope** | Scope = id + product + partner + branch |
| **Atomic transition** | Old ACTIVE → DEPRECATED + New → ACTIVE (or rollback all) |
| **Validation required** | Config must be complete (non-empty fields) |
| **Idempotent** | Activating ACTIVE config returns same (no error) |

---

## 🎯 Scope Definition

**Scope Components:**
- Screen/Flow/Mapping ID
- Product Code
- Partner Code  
- Branch Code

**Example Scopes:**
- `screenId=KYC + product=LOAN` → All partners/branches
- `screenId=KYC + product=LOAN + partner=SAMASTA` → All SAMASTA branches
- `screenId=KYC + product=LOAN + partner=SAMASTA + branch=MH001` → Specific branch

**Only ONE ACTIVE allowed per exact scope combination**

---

## 🚀 Common Use Cases

### Use Case 1: Deploy New Screen Config

```bash
# 1. Create new version
POST /api/v1/configs/screens
{"screenId": "kyc-001", "uiConfig": {...}}
# → configId=101, status=DRAFT

# 2. Activate
POST /api/v1/configs/screens/101/activate
# → configId=101, status=ACTIVE
# → Previous ACTIVE (if any) → DEPRECATED
```

### Use Case 2: Rollback to Previous Version

```bash
# Assume current ACTIVE is configId=102
# Want to rollback to configId=101 (now DEPRECATED)

# 1. Clone old version
POST /api/v1/configs/screens/101/clone
# → configId=103, status=DRAFT (copy of 101)

# 2. Activate clone
POST /api/v1/configs/screens/103/activate
# → configId=103, status=ACTIVE
# → configId=102, status=DEPRECATED (rollback!)
```

### Use Case 3: Branch-Specific Override

```bash
# 1. Product-level config (all branches)
POST /api/v1/configs/screens
{
  "screenId": "kyc-001",
  "productCode": "LOAN",
  "uiConfig": {...}
}
# → configId=10, status=DRAFT

POST /api/v1/configs/screens/10/activate
# → Applies to ALL branches

# 2. Branch-specific override
POST /api/v1/configs/screens
{
  "screenId": "kyc-001",
  "productCode": "LOAN",
  "partnerCode": "SAMASTA",
  "branchCode": "MH001",
  "uiConfig": {...}  // Different config
}
# → configId=11, status=DRAFT

POST /api/v1/configs/screens/11/activate
# → MH001 branch uses configId=11
# → All other branches use configId=10
```

---

## ⚠️ Important Constraints

### ✅ DO

- ✅ Create DRAFT configs for testing
- ✅ Activate only after validation
- ✅ Use activation API (not manual status change)
- ✅ Clone ACTIVE configs to create new versions
- ✅ Monitor activation logs

### ❌ DON'T

- ❌ Edit ACTIVE configs directly (clone instead)
- ❌ Manually set status to ACTIVE (use API)
- ❌ Activate incomplete configs
- ❌ Delete ACTIVE/DEPRECATED configs (audit trail)
- ❌ Expect config changes to affect in-progress apps

---

## 🔍 Verification

### Check Config Status

```bash
GET /api/v1/configs/screens/{configId}

# Response includes:
{
  "configId": 123,
  "status": "ACTIVE",  ← Current status
  ...
}
```

### Find ACTIVE Config for Scope

```bash
GET /api/v1/configs/screens?status=ACTIVE&screenId=kyc-001&productCode=LOAN

# Returns only ACTIVE configs
```

### Check What's Running in Production

```bash
# All ACTIVE screen configs
GET /api/v1/configs/screens?status=ACTIVE

# All ACTIVE flow configs
GET /api/v1/configs/flows?status=ACTIVE

# All ACTIVE field mapping configs
GET /api/v1/configs/mappings?status=ACTIVE
```

---

## 🐛 Troubleshooting

### Error: "Cannot activate config with status ACTIVE"

**Cause:** Config is already ACTIVE  
**Solution:** This is OK (idempotent). If you want a new version, clone first then activate the clone.

### Error: "Cannot activate config with status DEPRECATED"

**Cause:** Trying to activate an old version  
**Solution:** Clone it first (becomes DRAFT), then activate.

```bash
POST /api/v1/configs/screens/{deprecatedId}/clone  # → new DRAFT
POST /api/v1/configs/screens/{newDraftId}/activate  # → ACTIVE
```

### Error: "UI config cannot be empty for activation"

**Cause:** Config incomplete  
**Solution:** Update config with required data before activating.

```bash
PUT /api/v1/configs/screens/{configId}
{"uiConfig": {...}}  # Add required data

POST /api/v1/configs/screens/{configId}/activate  # Now works
```

### Error: "No ACTIVE config found for screenId=X"

**Cause:** No config activated for that scope  
**Solution:** Activate at least one config.

```bash
# Create and activate
POST /api/v1/configs/screens
{...}
# Get configId from response, then:
POST /api/v1/configs/screens/{configId}/activate
```

---

## 📊 Status Summary

| Status | Can Activate? | Used at Runtime? | Editable? | Can Delete? |
|--------|--------------|------------------|-----------|-------------|
| **DRAFT** | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes |
| **ACTIVE** | ⚠️ Idempotent | ✅ Yes (new apps) | ❌ No* | ❌ No |
| **DEPRECATED** | ❌ No | ⚠️ Existing apps only | ❌ No | ❌ No** |
| **INACTIVE** | ❌ No | ❌ No | ❌ No | ⚠️ Maybe |

*Clone to create new version instead  
**Keep for audit trail

---

## 🎯 Key Takeaways

1. **Always activate via API** - Don't manually change status
2. **DRAFT = safe playground** - Test without affecting production
3. **ONE ACTIVE per scope** - Previous automatically deprecated
4. **Atomic = safe** - Either all succeeds or all fails
5. **DEPRECATED = preserved** - Audit trail + existing apps
6. **Immutable snapshots** - In-progress apps unaffected by changes

---

## 📞 Support

**Error Messages:** Check logs for correlation ID  
**API Docs:** Swagger UI at `/swagger-ui.html`  
**Full Documentation:** See `CONFIG_ACTIVATION_SYSTEM.md`

---

**Version:** 1.0.0  
**Last Updated:** 2026-01-12
