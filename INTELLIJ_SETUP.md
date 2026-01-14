# IntelliJ IDEA Setup Guide

## Permanent Fix for Resource Copying Issues

This guide ensures that Flyway migration files and other resources are always available when running the application from IntelliJ IDEA.

## Problem

IntelliJ IDEA may not copy resources from `src/main/resources` to `target/classes` when running the application directly, causing:
- Flyway to report "No migrations found"
- Tables not being created
- Application errors when accessing resources

## Solution

### Option 1: Run Script Before Starting Application (Recommended)

Before starting the application in IntelliJ, run:

```bash
./ensure-resources.sh
```

This script ensures all resources (including Flyway migrations) are copied to `target/classes`.

### Option 2: Configure IntelliJ Run Configuration

1. Go to **Run** → **Edit Configurations...**
2. Select your Spring Boot run configuration
3. Click **Modify options** → **Add before launch task**
4. Select **Run External Tool**
5. Click the **+** button to add a new external tool
6. Configure:
   - **Name**: `Copy Resources`
   - **Program**: `/bin/bash`
   - **Arguments**: `-c "cd '$ProjectFileDir$' && ./ensure-resources.sh"`
   - **Working directory**: `$ProjectFileDir$`
7. Click **OK** to save

Now every time you run the application, resources will be copied automatically.

### Option 3: Use Maven to Run Application

Instead of running the application directly, use Maven:

1. Go to **Run** → **Edit Configurations...**
2. Click **+** → **Maven**
3. Configure:
   - **Name**: `Spring Boot Run`
   - **Working directory**: `$ProjectFileDir$`
   - **Command line**: `spring-boot:run`
4. Click **OK**

Maven will ensure resources are copied before running.

### Option 4: Mark Resources Directory in IntelliJ

1. Right-click `src/main/resources` in Project view
2. Select **Mark Directory as** → **Resources Root**
3. Go to **File** → **Project Structure** → **Modules**
4. Select your module → **Sources** tab
5. Ensure `src/main/resources` is marked as **Resources**

## Verification

After setup, verify resources are copied:

```bash
ls -la target/classes/db/migration/
```

You should see all migration files (V1 through V7).

## Build Script

The `compile.sh` script has been updated to automatically copy resources after compilation. Use it for building:

```bash
./compile.sh
```

## Troubleshooting

If migrations still aren't found:

1. **Delete database and restart**: `rm -rf data/`
2. **Run ensure-resources.sh**: `./ensure-resources.sh`
3. **Rebuild project**: `Build` → `Rebuild Project` in IntelliJ
4. **Restart IntelliJ**: Sometimes IntelliJ needs a restart to pick up changes

## Permanent Fix Applied

The following changes have been made to ensure resources are always available:

1. ✅ **pom.xml**: Added explicit `maven-resources-plugin` configuration
2. ✅ **compile.sh**: Updated to copy resources after compilation
3. ✅ **ensure-resources.sh**: New script to manually copy resources
4. ✅ **Migration files**: All 7 migration files are in `src/main/resources/db/migration/`

These changes ensure resources are copied whether you use Maven, IntelliJ, or manual scripts.
