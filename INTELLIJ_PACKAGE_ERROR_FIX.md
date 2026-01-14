# Fix: "package com.los.config.entity does not exist" in IntelliJ

## ✅ Problem Solved

The code compiles successfully with Java 21. The issue is IntelliJ's indexing/configuration.

## Root Cause

1. **Java Version Mismatch**: IntelliJ may be using a different Java version than Maven
2. **Stale Index**: IntelliJ's internal index is out of sync with the actual compiled classes
3. **Project Structure**: IntelliJ may not recognize the source folders correctly

## ✅ Verification

The project compiles successfully:
- ✅ All entity classes exist: `src/main/java/com/los/config/entity/*.java`
- ✅ All classes compile: `target/classes/com/los/config/entity/*.class`
- ✅ Package declarations are correct: `package com.los.config.entity;`

## 🔧 Fix Steps (Do These in Order)

### Step 1: Configure IntelliJ to Use Java 21

1. **File** → **Project Structure** (⌘; on Mac, Ctrl+Alt+Shift+S on Windows)
2. **Project** tab:
   - **Project SDK**: Select **21** (or add Java 21 if not listed)
   - **Project language level**: **21 - Preview**
3. **Modules** → Your module → **Sources** tab:
   - Ensure `src/main/java` is marked as **Sources** (blue folder icon)
   - Ensure `src/main/resources` is marked as **Resources** (green folder icon)
4. Click **OK**

### Step 2: Invalidate Caches and Restart

1. **File** → **Invalidate Caches...**
2. Check all boxes:
   - ✅ Clear file system cache and Local History
   - ✅ Clear downloaded shared indexes
   - ✅ Clear VCS file caches
3. Click **Invalidate and Restart**
4. Wait for IntelliJ to restart and re-index (may take 2-3 minutes)

### Step 3: Reload Maven Project

1. Right-click on `pom.xml` in Project view
2. Select **Maven** → **Reload Project**
3. Wait for Maven to download dependencies and index

### Step 4: Rebuild Project

1. **Build** → **Rebuild Project**
2. Wait for compilation to complete
3. Check the **Build** tool window for any errors

### Step 5: Verify Fix

1. Open any file that imports `com.los.config.entity.*` (e.g., `FlowSnapshotRepository.java`)
2. The imports should resolve (no red underlines)
3. Hover over the import - it should show the correct class

## 🚀 Quick Script

I've created a script that compiles the project correctly:

```bash
./fix-intellij-package-error.sh
```

This ensures:
- ✅ Java 21 is used for compilation
- ✅ All classes are compiled to `target/classes`
- ✅ Resources are copied correctly

After running the script, follow Steps 1-5 above in IntelliJ.

## 🔍 If Problem Persists

### Option 1: Check IntelliJ's Java Version

1. **Help** → **About**
2. Verify IntelliJ is using Java 21
3. If not, configure IntelliJ's JDK:
   - **File** → **Project Structure** → **SDKs**
   - Add Java 21 if missing
   - Set it as the project SDK

### Option 2: Manual Rebuild

1. Close IntelliJ
2. Run from terminal:
   ```bash
   ./fix-intellij-package-error.sh
   ```
3. Reopen IntelliJ
4. **File** → **Invalidate Caches** → **Invalidate and Restart**

### Option 3: Delete .idea and Reimport

⚠️ **Warning**: This will delete IntelliJ project settings

1. Close IntelliJ
2. Delete `.idea` folder:
   ```bash
   rm -rf .idea/
   ```
3. Reopen project in IntelliJ
4. Let IntelliJ re-index (may take a few minutes)

## ✅ Expected Result

After following these steps:
- ✅ No red underlines on imports
- ✅ All `com.los.config.entity.*` imports resolve correctly
- ✅ Project compiles without errors
- ✅ IntelliJ recognizes all entity classes

## 📝 Summary

The code is **100% correct**. This is purely an IntelliJ IDE configuration/indexing issue. The compilation from command line proves the code works. Follow the steps above to sync IntelliJ with the actual project state.
