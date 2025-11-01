# YourGPT Android SDK - Development Environment Setup

## Prerequisites

### Required Software
- **Android Studio** (latest version) - [Download](https://developer.android.com/studio)
- **Java Development Kit (JDK)** - OpenJDK 11 or 17
- **Android SDK** (API level 21+)
- **Git** for version control

### Optional Tools
- **Android Emulator** or physical Android device
- **ADB (Android Debug Bridge)** - included with Android SDK
- **Gradle** (included with Android Studio)

## Environment Setup

### 1. Install Android Studio
```bash
# Download and install Android Studio from:
# https://developer.android.com/studio

# During installation, ensure these components are selected:
# - Android SDK
# - Android SDK Platform
# - Android Virtual Device (AVD)
```

### 2. Configure SDK and Tools
```bash
# Open Android Studio
# Go to: Tools → SDK Manager
# Install these components:

# SDK Platforms:
# - Android 14 (API level 34) - Latest
# - Android 5.0 (API level 21) - Minimum supported

# SDK Tools:
# - Android SDK Build-Tools
# - Android Emulator
# - Android SDK Platform-Tools
# - Intel x86 Emulator Accelerator (HAXM installer)
```

### 3. Set Environment Variables
```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
# export ANDROID_HOME=$HOME/Android/Sdk        # Linux
# export ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk  # Windows

export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools/bin

# Reload your shell
source ~/.bashrc  # or ~/.zshrc
```

### 4. Verify Installation
```bash
# Check Android SDK
adb version

# Check available emulators
emulator -list-avds

# Check connected devices
adb devices
```

## Quick Start

### 1. Open Project in Android Studio
```bash
# Navigate to Android SDK directory
cd android-sdk

# Open in Android Studio
# File → Open → Select 'android-sdk' folder
# OR
studio .  # If you have Android Studio command line tools
```

### 2. Project Structure Setup
The project is configured with these modules:
```
android-sdk/
├── build.gradle (SDK module)
├── src/main/ (SDK source code)
├── example/
│   ├── build.gradle (Example app)
│   ├── settings.gradle (Project configuration)
│   └── src/main/ (Example app source)
```

### 3. Sync and Build
```bash
# In Android Studio:
# 1. Click "Sync Project with Gradle Files" 
# 2. Wait for sync to complete
# 3. Build → Make Project (Cmd+F9 / Ctrl+F9)
```

### 4. Running the Example App

#### Using Android Studio (Recommended)
1. **Select Configuration**: Choose "app" from the run configuration dropdown
2. **Select Target**: Choose emulator or connected device
3. **Run**: Click the green play button or press Shift+F10

#### Using Command Line
```bash
# Navigate to example directory
cd example

# Build and install on connected device/emulator
./gradlew installDebug

# Run specific tasks
./gradlew assembleDebug      # Build APK
./gradlew bundleRelease      # Build AAB
```

## Development Workflow

### Working with Local SDK

The example app is configured to use the local SDK module:

#### Module Dependencies
```kotlin
// In example/build.gradle
dependencies {
    implementation project(':yourgpt-sdk')
}
```

#### Settings Configuration
```kotlin
// In example/settings.gradle
include ':yourgpt-sdk'
project(':yourgpt-sdk').projectDir = new File(settingsDir, '../')
```

### Live Development
Changes to SDK source files will automatically trigger rebuilds:

1. **Edit SDK Files**: Modify files in `src/main/kotlin/`
2. **Auto-Build**: Android Studio will automatically build changes
3. **Instant Run**: Changes reflected with Instant Run (if supported)

## Testing the SDK

### 1. Basic Functionality Test
```kotlin
// In MainActivity.kt, verify these features work:
1. SDK initialization with valid widgetUid
2. Loading states display correctly
3. Error handling with invalid configuration
4. WebView loads the widget successfully
5. Bidirectional communication (send message, receive events)
```

### 2. Debug Mode Testing
```kotlin
// Enable debug mode in the example app
val configuration = YourGPTConfiguration(
    widgetUid = "widget_123456",
    debug = true // Enable detailed logging
)
```

### 3. Device Testing

#### Android Emulator Testing
```bash
# Create AVD (Android Virtual Device)
# In Android Studio: Tools → AVD Manager → Create Virtual Device

# Or via command line:
avdmanager create avd -n "Pixel_API_34" -k "system-images;android-34;google_apis;x86_64"

# Start emulator
emulator -avd Pixel_API_34

# List running emulators
adb devices
```

#### Physical Device Testing
```bash
# Enable Developer Options on your device:
# Settings → About Phone → Tap "Build Number" 7 times

# Enable USB Debugging:
# Settings → Developer Options → USB Debugging

# Connect device and verify
adb devices
# Should show your device as "device" (not "unauthorized")
```

## Development Commands

### Gradle Commands
```bash
# In android-sdk/ directory

# Build SDK module
./gradlew :yourgpt-sdk:assembleDebug

# Run tests
./gradlew :yourgpt-sdk:testDebugUnitTest

# Generate documentation
./gradlew :yourgpt-sdk:dokkaHtml

# Lint check
./gradlew :yourgpt-sdk:lintDebug
```

### Example App Commands
```bash
# In android-sdk/example/ directory

# Build and install
./gradlew installDebug

# Run tests
./gradlew testDebugUnitTest

# Generate signed APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### ADB Commands
```bash
# Install APK
adb install example/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "(YourGPT|AndroidRuntime)"

# Clear app data
adb shell pm clear com.yourgpt.sdk.example

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

## Debugging

### Android Studio Debugger
1. **Set Breakpoints**: Click in the gutter next to line numbers
2. **Debug Run**: Click debug button or press Shift+F9
3. **Debug Console**: View → Tool Windows → Debug
4. **Variables**: Inspect variables in the debug window

### Logging
```kotlin
// Use Android Log for debugging
import android.util.Log

Log.d("YourGPTSDK", "Debug message")
Log.i("YourGPTSDK", "Info message")
Log.e("YourGPTSDK", "Error message")

// Use SDK's built-in logging
// Enable debug mode in configuration for detailed logs
```

### LogCat
```bash
# View logs in Android Studio
# View → Tool Windows → Logcat

# Filter logs by tag
# In Logcat window, set filter to "YourGPT"

# Command line logging
adb logcat -s YourGPTSDK
adb logcat | grep YourGPT
```

### WebView Debugging
```bash
# Enable WebView debugging in your app
WebView.setWebContentsDebuggingEnabled(true)

# Chrome DevTools for WebView debugging
# 1. Open Chrome browser
# 2. Navigate to chrome://inspect
# 3. Select your app's WebView under "Remote Target"
```

### Memory Profiling
1. **Memory Profiler**: View → Tool Windows → Profiler
2. **Heap Dump**: Capture and analyze heap dumps
3. **Memory Leaks**: Use LeakCanary for detection

### Layout Inspector
```bash
# Inspect UI layout
# Tools → Layout Inspector
# Select your device and app
```

## Android Studio Configuration

### Project Settings
```kotlin
// Key settings for the project:
android {
    compileSdk 34
    defaultConfig {
        minSdk 21
        targetSdk 34
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
}
```

### Useful Plugins
Install these Android Studio plugins:
- **Kotlin** (included)
- **ADB Idea** (ADB commands from IDE)
- **Database Inspector** (SQLite debugging)
- **Network Inspector** (Network debugging)

### Code Style
Configure Kotlin code style:
- File → Settings → Editor → Code Style → Kotlin
- Set from... → Predefined Style → Kotlin style guide

## Performance Testing

### Performance Profiler
```bash
# CPU Profiler
# View → Tool Windows → Profiler → CPU

# Memory Profiler  
# View → Tool Windows → Profiler → Memory

# Network Profiler
# View → Tool Windows → Profiler → Network
```

### Benchmarking
```kotlin
// Use Jetpack Benchmark for performance testing
// Add to build.gradle:
androidTestImplementation 'androidx.benchmark:benchmark-junit4:1.2.2'
```

### Battery Usage
```bash
# Monitor battery usage
# Settings → Battery → Battery Usage
# Or use Battery Historian tool
```

## Common Issues & Solutions

### Sync Issues
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches

# Invalidate caches in Android Studio
# File → Invalidate Caches and Restart
```

### Build Issues
```bash
# Update Gradle Wrapper
./gradlew wrapper --gradle-version=8.0

# Check for Gradle compatibility
# File → Project Structure → Project → Gradle Version
```

### Emulator Issues
```bash
# Wipe emulator data
emulator -avd YOUR_AVD_NAME -wipe-data

# Cold boot emulator
emulator -avd YOUR_AVD_NAME -no-snapshot-load

# Fix HAXM issues (Intel systems)
# Reinstall Intel HAXM from SDK Manager
```

### WebView Issues
1. **WebView not loading**:
   ```xml
   <!-- Add to AndroidManifest.xml -->
   <uses-permission android:name="android.permission.INTERNET" />
   ```

2. **JavaScript not working**:
   ```kotlin
   webView.settings.javaScriptEnabled = true
   ```

3. **Network security issues**:
   ```xml
   <!-- Add to AndroidManifest.xml for development -->
   <application android:usesCleartextTraffic="true">
   ```

## CI/CD Setup

### GitHub Actions Example
```yaml
# .github/workflows/android.yml
name: Android CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
        distribution: 'adopt'
        
    - name: Cache Gradle packages
      uses: actions/cache@v2
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      working-directory: android-sdk
      
    - name: Run tests
      run: ./gradlew testDebugUnitTest
      working-directory: android-sdk
      
    - name: Build APK
      run: ./gradlew assembleDebug
      working-directory: android-sdk/example
```

### Local Build Scripts
```bash
#!/bin/bash
# build.sh - Local build script

set -e

echo "Building YourGPT Android SDK..."

# Clean previous builds
./gradlew clean

# Run tests
./gradlew testDebugUnitTest

# Build SDK
./gradlew assembleDebug

# Build example app
cd example
./gradlew assembleDebug

echo "Build completed successfully!"
```

## Release Testing

### Pre-release Checklist
- [ ] Test on different Android versions (API 21+)
- [ ] Test on different screen sizes and densities
- [ ] Test on physical devices
- [ ] Verify all SDK methods work correctly
- [ ] Test error handling scenarios
- [ ] Performance testing with Android Profiler
- [ ] Memory leak testing
- [ ] Network interruption testing
- [ ] ProGuard/R8 optimization testing

### Distribution Build
```bash
# Generate signed release APK
./gradlew assembleRelease

# Generate App Bundle (recommended for Play Store)
./gradlew bundleRelease

# Generate AAR for SDK distribution
./gradlew assembleRelease
# Output: build/outputs/aar/yourgpt-sdk-release.aar
```

### Testing Different Configurations
```bash
# Test with ProGuard/R8 enabled
./gradlew assembleRelease

# Test with different build types
./gradlew assembleDebug
./gradlew assembleBenchmark  # If you have benchmark build type
```

## Troubleshooting

### Common Error Messages

1. **"SDK location not found"**:
   ```bash
   # Create local.properties file with:
   sdk.dir=/path/to/Android/Sdk
   ```

2. **"Unable to resolve dependency"**:
   ```bash
   ./gradlew clean
   ./gradlew --refresh-dependencies
   ```

3. **"Emulator: Process finished with exit code 1"**:
   ```bash
   # Check available system images
   sdkmanager --list | grep system-images
   
   # Install required system image
   sdkmanager "system-images;android-34;google_apis;x86_64"
   ```

4. **"adb: device unauthorized"**:
   ```bash
   # Revoke USB debugging authorizations on device
   # Settings → Developer Options → Revoke USB debugging authorizations
   # Reconnect device and accept authorization dialog
   ```

## Support Resources

### Documentation
- [Android Developer Documentation](https://developer.android.com/docs)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Android Studio User Guide](https://developer.android.com/studio/intro)

### Tools
- [Android Debug Bridge (ADB)](https://developer.android.com/studio/command-line/adb)
- [Gradle Build Tool](https://gradle.org/guides/)
- [Android Emulator](https://developer.android.com/studio/run/emulator)

### Community
- [Android Developer Community](https://developer.android.com/community)
- [Kotlin Community](https://kotlinlang.org/community/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/android)