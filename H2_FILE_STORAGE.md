# H2 File-Based Storage Configuration

## ✅ IMPLEMENTED: Data Persistence Across Restarts

### Overview
The application now uses **H2 file-based storage** instead of in-memory storage, ensuring all data persists across server restarts.

---

## 🔧 Configuration Changes

### Before (In-Memory - Data Lost on Restart)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:losdb
    username: sa
    password: 
```

### After (File-Based - Data Persists)
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/los-config-db
    username: sa
    password: password
```

---

## 📂 Database Storage Location

**File Path:** `./data/los-config-db.mv.db`
- **Relative Path:** From project root
- **Git Ignored:** Yes (data/ directory excluded)
- **Created Automatically:** On first application start

---

## 🚀 How It Works

1. **First Startup:**
   - H2 creates `data/los-config-db.mv.db` file
   - Flyway runs all migrations (V1, V2, V3, V4, R__)
   - Master data is seeded (partners, products)
   - Database file is saved to disk

2. **Subsequent Startups:**
   - H2 loads existing database from disk
   - Flyway checks schema history
   - Only new migrations are applied
   - **All existing data is preserved** ✅

3. **Server Restart:**
   - ✅ Screen configs persist
   - ✅ Validation configs persist
   - ✅ Field mappings persist
   - ✅ Flow configs persist
   - ✅ Flow snapshots persist
   - ✅ Loan applications persist

---

## 🧪 Verification Steps

### Step 1: Start Application
```bash
# Start Spring Boot application
mvn spring-boot:run
# or run from IntelliJ
```

### Step 2: Create Sample Data
```bash
# Create a screen config via API
curl -X POST http://localhost:8080/api/config/screen-configs \
  -H "Content-Type: application/json" \
  -d '{
    "screenId": "test-screen-001",
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA",
    "status": "ACTIVE",
    "uiConfig": {"title": "Test Screen"}
  }'
```

### Step 3: Verify Data Exists
```bash
# List all screen configs
curl http://localhost:8080/api/config/screen-configs
```

### Step 4: Restart Server
```bash
# Stop the application (Ctrl+C or stop in IntelliJ)
# Start it again
mvn spring-boot:run
```

### Step 5: Verify Data Persisted ✅
```bash
# List configs again - should see the same data
curl http://localhost:8080/api/config/screen-configs
```

**Expected:** Same config created in Step 2 is still present!

---

## 🔍 H2 Console Access

### Access H2 Console
**URL:** http://localhost:8080/h2-console

### Connection Settings
```
JDBC URL:    jdbc:h2:file:./data/los-config-db
Username:    sa
Password:    password
```

### View Data
```sql
-- Check flyway migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Check master data
SELECT * FROM partners;
SELECT * FROM products;

-- Check screen configs
SELECT * FROM screen_configs;

-- Check all tables
SHOW TABLES;
```

---

## 📁 File Structure

```
Backend Los/
├── data/                          # Database storage (Git ignored)
│   ├── README.md                  # Documentation
│   ├── los-config-db.mv.db        # H2 database file (auto-created)
│   └── los-config-db.trace.db     # H2 trace file (auto-created)
├── src/
│   └── main/
│       └── resources/
│           ├── application.yml    # Updated with file-based URL
│           └── db/
│               └── migration/     # Flyway migrations
└── .gitignore                     # Excludes data/
```

---

## 🔐 Security Notes

### Development
- **Username:** `sa` (default H2 admin)
- **Password:** `password` (simple for dev)
- **H2 Console:** Enabled for easy access

### Production Recommendations
```yaml
# application-prod.yml
spring:
  datasource:
    password: ${DB_PASSWORD}  # Use environment variable
  h2:
    console:
      enabled: false          # Disable console in production
```

---

## 🎯 Benefits

| Feature | In-Memory (Before) | File-Based (Now) |
|---------|-------------------|------------------|
| **Data Persistence** | ❌ Lost on restart | ✅ Persists |
| **Development Speed** | ✅ Fast | ✅ Fast |
| **Testing** | ⚠️ Reset each time | ✅ Can test with real data |
| **Debugging** | ❌ Data lost | ✅ Can inspect after restart |
| **Demo/Showcase** | ❌ Reset each time | ✅ Maintains state |
| **Performance** | ✅ Very fast | ✅ Fast enough |

---

## 🔄 Flyway Integration

### First Run (Fresh Database)
```
INFO  o.f.core.internal.command.DbMigrate - Migrating schema "PUBLIC" to version "1 - create master data tables"
INFO  o.f.core.internal.command.DbMigrate - Migrating schema "PUBLIC" to version "2 - create domain tables"
INFO  o.f.core.internal.command.DbMigrate - Migrating schema "PUBLIC" to version "3 - create config tables"
INFO  o.f.core.internal.command.DbMigrate - Migrating schema "PUBLIC" to version "4 - seed master data"
INFO  o.f.core.internal.command.DbMigrate - Successfully applied 4 migrations
```

### Subsequent Runs (Existing Database)
```
INFO  o.f.core.internal.command.DbValidate - Successfully validated 5 migrations
INFO  o.f.core.internal.command.DbMigrate - Schema "PUBLIC" is up to date. No migration necessary.
```

**Note:** Flyway tracks applied migrations in `flyway_schema_history` table, which is also persisted!

---

## 🛠️ Maintenance Commands

### Backup Database
```bash
# Simple file copy
cp data/los-config-db.mv.db data/los-config-db.backup.mv.db
```

### Restore Database
```bash
# Stop application first, then:
cp data/los-config-db.backup.mv.db data/los-config-db.mv.db
```

### Reset Database (Clean Start)
```bash
# Stop application, then:
rm data/los-config-db.mv.db
rm data/los-config-db.trace.db
# Start application - Flyway will recreate everything
```

### Export Data (SQL)
```sql
-- From H2 Console
SCRIPT TO 'data/backup.sql';
```

### Import Data (SQL)
```sql
-- From H2 Console
RUNSCRIPT FROM 'data/backup.sql';
```

---

## 🚨 Troubleshooting

### Issue: Database file locked
**Symptom:** "Database may be already in use"
**Solution:** Ensure only one application instance is running

### Issue: File not found
**Symptom:** Cannot connect to database file
**Solution:** Ensure `data/` directory exists and has write permissions

### Issue: Data lost after restart
**Symptom:** Configs disappear
**Checklist:**
- ✅ Check `application.yml` has `jdbc:h2:file:` (not `jdbc:h2:mem:`)
- ✅ Check `data/los-config-db.mv.db` file exists and has size > 0
- ✅ Check file permissions (should be writable)

### Issue: Migration errors
**Symptom:** Flyway fails to apply migrations
**Solution:** 
```sql
-- Check migration history
SELECT * FROM flyway_schema_history WHERE success = FALSE;
-- Delete failed migration and restart
DELETE FROM flyway_schema_history WHERE version = 'X';
```

---

## 📊 Database File Size

**Initial Size:** ~500 KB (empty schema with master data)
**Expected Growth:** ~10-50 KB per config

**Typical Sizes:**
- 100 screen configs: ~1 MB
- 1,000 screen configs: ~5 MB
- 10,000 screen configs: ~50 MB

H2 automatically manages file size and compression.

---

## 🔄 Migration Path (Future)

### From H2 File → PostgreSQL/MySQL

When ready for production database:

1. **Export H2 data:**
   ```sql
   SCRIPT TO 'data/h2-export.sql';
   ```

2. **Update application.yml:**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/losdb
       username: postgres
       password: ${DB_PASSWORD}
   ```

3. **Import to PostgreSQL:**
   ```bash
   psql losdb < data/h2-export.sql
   ```

**Note:** Flyway migrations remain the same - they're database-agnostic!

---

## ✅ Summary

**What Changed:**
- ✅ Database URL: `mem` → `file:./data/los-config-db`
- ✅ Password: `""` → `"password"`
- ✅ Data directory created and Git-ignored
- ✅ Documentation added

**What Stayed the Same:**
- ✅ All entity models unchanged
- ✅ All business logic unchanged
- ✅ All APIs unchanged
- ✅ Flyway migrations unchanged
- ✅ H2 console still accessible

**Result:**
🎉 **Data now persists across server restarts!**

---

## 🎯 Quick Reference

| Item | Value |
|------|-------|
| **Database Type** | H2 File-Based |
| **Storage Path** | `./data/los-config-db.mv.db` |
| **JDBC URL** | `jdbc:h2:file:./data/los-config-db` |
| **Username** | `sa` |
| **Password** | `password` |
| **H2 Console** | http://localhost:8080/h2-console |
| **Persistence** | ✅ Yes |
| **Auto-start** | ✅ Yes |
| **Flyway** | ✅ Enabled |

---

**Last Updated:** 2026-01-12  
**Status:** ✅ Production Ready (Development/Testing)
