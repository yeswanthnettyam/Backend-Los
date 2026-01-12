# Database Migrations Guide

## Overview
This project uses **Flyway** for database migrations, similar to Liquibase. All migrations run automatically when the server starts.

## Migration Types

### 1. Versioned Migrations (V__*.sql)
- **Purpose**: One-time schema changes (create tables, alter columns, etc.)
- **Naming**: `V{version}__{description}.sql` (e.g., `V1__create_master_data_tables.sql`)
- **Execution**: Runs **once** and never again
- **Location**: `src/main/resources/db/migration/`

**Existing Versioned Migrations:**
- `V1__create_master_data_tables.sql` - Creates partners and products tables
- `V2__create_domain_tables.sql` - Creates loan_applications, applicants, businesses, verification_records
- `V3__create_config_tables.sql` - Creates screen_configs, validation_configs, field_mapping_configs, flow_configs, flow_snapshots
- `V4__seed_master_data.sql` - Initial seed data (uses MERGE for idempotency)

### 2. Repeatable Migrations (R__*.sql)
- **Purpose**: Master/reference data that may change over time
- **Naming**: `R__{description}.sql` (e.g., `R__master_data.sql`)
- **Execution**: Runs **every time the file changes** (checksum-based)
- **Location**: `src/main/resources/db/migration/`

**Existing Repeatable Migrations:**
- `R__master_data.sql` - Maintains partners and products master data

## How It Works (Like Liquibase)

### On Server Startup:
1. Flyway checks `flyway_schema_history` table
2. Runs any **new versioned migrations** (V__) that haven't been applied
3. Runs **all repeatable migrations** (R__) if their checksum changed
4. All scripts are **idempotent** - safe to run multiple times

### Idempotency Pattern:
All seed data scripts use **MERGE** statements:
```sql
MERGE INTO "table_name" ("column1", "column2")
KEY ("primary_key_column")
VALUES
('value1', 'value2');
```

This ensures:
- ✅ Insert if record doesn't exist
- ✅ Update if record already exists
- ✅ No duplicate errors
- ✅ Safe to run multiple times

## Adding New Master Data

### Option 1: Add to Repeatable Migration (Recommended)
Edit `R__master_data.sql`:
```sql
MERGE INTO "partners" ("partner_code", "partner_name", "is_active", "created_at")
KEY ("partner_code")
VALUES
('NEW_CODE', 'New Partner Name', TRUE, CURRENT_TIMESTAMP);
```

### Option 2: Create New Versioned Migration
Create `V5__add_new_partner.sql`:
```sql
MERGE INTO "partners" ("partner_code", "partner_name", "is_active", "created_at")
KEY ("partner_code")
VALUES
('NEW_CODE', 'New Partner Name', TRUE, CURRENT_TIMESTAMP);
```

## Configuration

### Flyway Settings (application.yml):
```yaml
spring:
  flyway:
    enabled: true                    # Auto-run on startup
    baseline-on-migrate: true        # Handle existing databases
    locations: classpath:db/migration # Where to find SQL files
    validate-on-migrate: false       # Skip validation for flexibility
```

## Flyway vs Liquibase Comparison

| Feature | Flyway | Liquibase |
|---------|--------|-----------|
| **Versioned Changes** | V__*.sql | numbered changesets |
| **Repeatable Changes** | R__*.sql | runAlways="true" |
| **Auto-run on startup** | ✅ Yes | ✅ Yes |
| **Idempotency** | Manual (MERGE) | Built-in (changeSet id) |
| **Rollback** | ❌ Manual | ✅ Automatic |
| **SQL-based** | ✅ Yes | ✅ Yes (also XML/YAML) |

## Best Practices

1. **Never modify existing versioned migrations** after they've been deployed
2. **Always use MERGE** for master data to ensure idempotency
3. **Use repeatable migrations** (R__) for reference data that changes
4. **Test migrations** against a copy of production data
5. **Use quoted identifiers** ("table_name", "column_name") for case sensitivity

## Troubleshooting

### Clear History (Development Only):
```sql
-- Connect to H2 console: http://localhost:8080/h2-console
DROP TABLE flyway_schema_history;
```
Then restart - all migrations will re-run.

### Check Migration Status:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Failed Migration:
1. Fix the SQL file
2. Delete the failed entry from `flyway_schema_history`
3. Restart the application

## Examples

### Add New Product:
```sql
-- Edit R__master_data.sql or create V6__add_new_product.sql
MERGE INTO "products" ("product_code", "product_name", "is_active", "created_at")
KEY ("product_code")
VALUES ('HOUSING', 'Housing Loan', TRUE, CURRENT_TIMESTAMP);
```

### Add New Table:
```sql
-- Create V5__create_audit_table.sql
CREATE TABLE "audit_log" (
    "audit_id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "entity_type" VARCHAR(100) NOT NULL,
    "entity_id" BIGINT NOT NULL,
    "action" VARCHAR(50) NOT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```
