# Installation Guide: Homebrew & Maven on macOS

Complete step-by-step guide to install Homebrew and Maven on macOS.

## 📋 Prerequisites

- macOS (any recent version)
- Administrator access (for sudo commands)
- Terminal access
- Internet connection

---

## 🍺 Step 1: Install Homebrew

Homebrew is a package manager for macOS that makes installing software easy.

### Method 1: Using the Official Installer (Recommended)

1. **Open Terminal**
   - Press `Cmd + Space` to open Spotlight
   - Type "Terminal" and press Enter
   - Or go to Applications → Utilities → Terminal

2. **Run the Homebrew Installation Script**
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

3. **Follow the Prompts**
   - It will ask for your **administrator password** (you won't see characters as you type)
   - Press Enter after typing your password
   - It will ask you to press Enter again to confirm
   - The installation will take a few minutes

4. **Add Homebrew to Your PATH** (if prompted)
   
   After installation, you'll see a message like:
   ```
   Next steps:
   - Run these two commands in your terminal to add Homebrew to your PATH:
     echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
     eval "$(/opt/homebrew/bin/brew shellenv)"
   ```
   
   **Run these commands:**
   ```bash
   echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
   eval "$(/opt/homebrew/bin/brew shellenv)"
   ```

   **For older Macs (Intel):**
   ```bash
   echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
   eval "$(/usr/local/bin/brew shellenv)"
   ```

5. **Verify Homebrew Installation**
   ```bash
   brew --version
   ```
   
   You should see something like: `Homebrew 4.x.x`

### Troubleshooting Homebrew Installation

- **If you get "command not found" after installation:**
  - Close and reopen your Terminal window
  - Or run: `source ~/.zprofile` (or `source ~/.bash_profile` for older systems)

- **If installation fails:**
  - Check your internet connection
  - Make sure you have administrator privileges
  - Try again after a few minutes (servers might be busy)

---

## ☕ Step 2: Install Java 21 (Required for Maven)

Maven requires Java. Since our project uses Java 21, we need to install it.

### Install Java 21 using Homebrew

```bash
# Install OpenJDK 21
brew install openjdk@21

# Link it to make it available
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

### Set JAVA_HOME Environment Variable

```bash
# Add to your shell profile (for zsh)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zprofile
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zprofile

# Reload your profile
source ~/.zprofile
```

**For bash (older systems):**
```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.bash_profile
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bash_profile
source ~/.bash_profile
```

### Verify Java Installation

```bash
java -version
```

You should see:
```
openjdk version "21.0.x" ...
```

---

## 🏗️ Step 3: Install Maven

Now that Homebrew is installed, installing Maven is easy!

### Install Maven

```bash
brew install maven
```

This will:
- Download Maven
- Install it to `/opt/homebrew/bin/maven` (or `/usr/local/bin/maven` for Intel Macs)
- Set up all necessary paths

### Verify Maven Installation

```bash
mvn -version
```

You should see output like:
```
Apache Maven 3.9.x (...
Maven home: /opt/homebrew/Cellar/maven/3.9.x/libexec
Java version: 21.0.x, ...
Java home: /Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home
...
```

---

## ✅ Step 4: Verify Everything Works

### Check All Installations

Run these commands to verify:

```bash
# Check Homebrew
brew --version

# Check Java
java -version

# Check Maven
mvn -version
```

All commands should work without errors!

---

## 🚀 Step 5: Test with Your Project

Now let's test Maven with your LOS project:

```bash
# Navigate to your project directory
cd "/Users/yeswanthchowdary/Desktop/yesh/Backend Los"

# Clean and compile the project
mvn clean compile

# If compilation succeeds, you're ready!
```

### Run the Application

```bash
mvn spring-boot:run
```

---

## 📝 Quick Reference Commands

### Homebrew Commands
```bash
brew --version              # Check Homebrew version
brew update                 # Update Homebrew itself
brew upgrade                # Upgrade all installed packages
brew install <package>      # Install a package
brew uninstall <package>    # Uninstall a package
brew list                   # List installed packages
brew search <package>       # Search for packages
```

### Maven Commands
```bash
mvn -version                # Check Maven version
mvn clean                   # Clean build artifacts
mvn compile                 # Compile source code
mvn test                    # Run tests
mvn package                 # Package as JAR
mvn install                 # Install to local repository
mvn spring-boot:run         # Run Spring Boot application
```

---

## 🔧 Troubleshooting

### Issue: "Command not found" after installation

**Solution:**
1. Close and reopen Terminal
2. Or run: `source ~/.zprofile` (or `source ~/.bash_profile`)

### Issue: "Permission denied" errors

**Solution:**
```bash
# Fix Homebrew permissions
sudo chown -R $(whoami) /opt/homebrew
# Or for Intel Macs:
sudo chown -R $(whoami) /usr/local
```

### Issue: Java version mismatch

**Solution:**
```bash
# List installed Java versions
/usr/libexec/java_home -V

# Switch to Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Verify
java -version
```

### Issue: Maven can't find Java

**Solution:**
```bash
# Set JAVA_HOME explicitly
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

# Add to your profile permanently
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zprofile
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zprofile
```

### Issue: Port 8080 already in use

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process (replace <PID> with actual process ID)
kill -9 <PID>
```

---

## 📚 Additional Resources

- **Homebrew:** https://brew.sh
- **Maven Documentation:** https://maven.apache.org
- **OpenJDK:** https://openjdk.org

---

## ✨ Next Steps

After installation:

1. ✅ **Verify installations** (all commands work)
2. ✅ **Navigate to project** directory
3. ✅ **Compile the project:** `mvn clean compile`
4. ✅ **Run the application:** `mvn spring-boot:run`
5. ✅ **Access Swagger UI:** http://localhost:8080/swagger-ui.html

---

## 💡 Tips

- **Keep Homebrew updated:** Run `brew update` regularly
- **Keep packages updated:** Run `brew upgrade` periodically
- **Use Homebrew for other tools:** It's great for installing git, node, python, etc.
- **Check Maven settings:** Located at `~/.m2/settings.xml` (optional)

---

**That's it! You're all set! 🎉**

If you encounter any issues, check the troubleshooting section above or consult the official documentation.

