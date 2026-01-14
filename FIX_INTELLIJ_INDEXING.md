# Fix IntelliJ "package does not exist" Error

## Problem
IntelliJ reports: `java: package com.los.config.entity does not exist`

## Solution

This is an IntelliJ indexing issue. The files exist and are correct, but IntelliJ needs to refresh its index.

### Quick Fix (Try These in Order):

1. **Invalidate Caches and Restart** (Most Effective):
   - Go to **File** → **Invalidate Caches...**
   - Check all boxes:
     - ✅ Clear file system cache and Local History
     - ✅ Clear downloaded shared indexes
     - ✅ Clear VCS file caches
   - Click **Invalidate and Restart**
   - Wait for IntelliJ to re-index

2. **Reload Maven Project**:
   - Right-click on `pom.xml` in Project view
   - Select **Maven** → **Reload Project**
   - Wait for Maven to download dependencies and index

3. **Rebuild Project**:
   - Go to **Build** → **Rebuild Project**
   - Wait for compilation to complete

4. **Check Source Folders**:
   - Go to **File** → **Project Structure** (⌘; on Mac, Ctrl+Alt+Shift+S on Windows)
   - Select **Modules** → Your module
   - Go to **Sources** tab
   - Ensure `src/main/java` is marked as **Sources** (blue folder icon)
   - Ensure `src/main/resources` is marked as **Resources** (green folder icon)
   - Click **OK**

5. **Sync Project with Gradle/Maven Files**:
   - Go to **File** → **Sync Project with Gradle Files** (if using Gradle)
   - Or **File** → **Reload Project from Disk**

6. **Check Java SDK**:
   - Go to **File** → **Project Structure** → **Project**
   - Ensure **Project SDK** is set to Java 21
   - Ensure **Project language level** is set to **21 - Preview**

7. **Manual Rebuild**:
   ```bash
   ./compile.sh
   ```
   Then restart IntelliJ

## Verification

After applying fixes, verify:

1. **Check if files are recognized**:
   - Open `src/main/java/com/los/config/entity/FlowConfig.java`
   - IntelliJ should show no red underlines
   - Hover over imports - they should resolve correctly

2. **Check compilation**:
   - Go to **Build** → **Build Project**
   - Should complete without errors

3. **Check imports**:
   - Open any file that imports `com.los.config.entity.*`
   - Imports should be resolved (no red underlines)

## If Problem Persists

1. **Close IntelliJ completely**
2. **Delete IntelliJ cache**:
   ```bash
   rm -rf .idea/
   ```
   ⚠️ **Warning**: This will delete your IntelliJ project settings. You may need to reconfigure run configurations.

3. **Reopen project in IntelliJ**
4. **Let IntelliJ re-index** (may take a few minutes)

## Alternative: Use Maven from Command Line

If IntelliJ continues to have issues, you can compile and run from command line:

```bash
# Compile
./compile.sh

# Run
mvn spring-boot:run
```

The code is correct - this is purely an IntelliJ IDE issue.
