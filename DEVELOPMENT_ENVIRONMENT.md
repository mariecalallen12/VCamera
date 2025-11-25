# VCamera Project - Development Environment Report

## Project Overview
VCamera is an Android application written in Kotlin and Java that allows users to replace the camera feed with specified videos. It can be used as a live broadcast assistant or for privacy protection.

## Project Technology Stack
- **Language**: Kotlin 1.6.21 / Java 8
- **Platform**: Android (minSdk 24, targetSdk 34, compileSdk 34)
- **Build System**: Gradle 7.2 with Android Gradle Plugin 7.2.0
- **Architecture**: MVVM with ViewModel and LiveData

## Installed Development Tools

### Core Tools
| Tool | Version | Purpose |
|------|---------|---------|
| Java (OpenJDK Temurin) | 17.0.17 | Primary runtime for Android development |
| Kotlin (system) | 2.2.21 | System-wide Kotlin installation |
| Kotlin (project) | 1.6.21 | Project-configured Kotlin version |
| Gradle | 9.2.0 (system) / 7.2 (wrapper) | Build automation |
| Maven | 3.9.11 | Alternative build tool / dependency management |

### Additional Tools
| Tool | Version | Purpose |
|------|---------|---------|
| Node.js | 20.19.5 | JavaScript runtime |
| npm | 10.8.2 | JavaScript package manager |
| Python | 3.12.3 | Scripting and automation |
| pip | 24.0 | Python package manager |

## Android SDK Configuration

### Installed Platforms
- Android SDK Platform 34 (android-34)
- Android SDK Platform 34 Extensions (ext8-12)
- Android SDK Platform 35 (android-35)
- Android SDK Platform 35 Extensions (ext14-15)
- Android SDK Platform 36 (android-36, 36.1)
- Android SDK Platform 36 Extensions (ext18-19)

### Installed Build Tools
- Build-Tools 34.0.0
- Build-Tools 35.0.0, 35.0.1
- Build-Tools 36.0.0, 36.1.0

### NDK Versions
- NDK 26.3.11579264
- NDK 27.3.13750724
- NDK 28.2.13676358
- NDK 29.0.14206865

### Other Components
- CMake 3.31.5, 4.1.2
- Platform-Tools 36.0.0
- Android Support Repository 47.0.0
- Google Repository 58
- Google Play Services 49

## Project Dependencies

### Android Libraries
- Material Design: 1.3.0
- AndroidX Core KTX: 1.3.2
- AndroidX AppCompat: 1.3.0-rc01
- ConstraintLayout: 2.0.4
- RecyclerView: 1.2.0
- ViewPager2: 1.0.0
- Activity KTX: 1.2.2
- Fragment KTX: 1.3.3
- Preference KTX: 1.1.1

### Coroutines & Lifecycle
- Kotlinx Coroutines Android: 1.4.2
- Lifecycle ViewModel KTX: 2.3.1
- Lifecycle LiveData KTX: 2.3.1
- Lifecycle Runtime KTX: 2.3.1

### UI Components
- DotsIndicator: 4.2
- Material Dialogs Core: 3.3.0
- Material Dialogs Input: 3.3.0
- StateView Kotlin: 2.2.0
- CatLoadingLibrary: 1.0.9
- SimpleSearchView: 0.2.0
- CornerLabelView: 1.0.0
- OSMDroid Android: 6.1.11
- RVAdapter: 0.3.7
- FloatingView: 1.6

### Internal Dependencies
- opensdk (submodule): WaxMoon/opensdk
- camera library: virtual.camera.camera:camera:1.0.0

## Build Configuration Updates Made

1. **Updated Kotlin version** from 1.5.21 to 1.6.21
2. **Updated Android Gradle Plugin** from 7.0.2 to 7.2.0
3. **Updated compileSdkVersion** from 33 to 34
4. **Updated targetSdkVersion** from 31 to 34
5. **Updated buildToolsVersion** from 31.0.0 to 34.0.0
6. **Removed deprecated jcenter()** repository (replaced with google() and mavenCentral())
7. **Added Gradle wrapper** configuration for version 7.2

## Notes

### Network Limitations
The sandbox environment has restricted network access. The following domains are blocked:
- dl.google.com (Android SDK downloads)
- jitpack.io (custom library repository)

This prevents downloading new dependencies during build. For full builds, you need to run on a machine with unrestricted network access.

### Submodule Dependency
The project depends on the `opensdk` submodule from https://github.com/WaxMoon/opensdk. Before building, run:
```bash
git submodule update --init --recursive
```

### To Build the Project
```bash
# Set up Android SDK path
echo "sdk.dir=/path/to/android/sdk" > local.properties

# Initialize submodules
git submodule update --init --recursive

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Recommendations

1. **For local development**: Ensure you have Android Studio installed with SDK version 34
2. **For CI/CD**: Use GitHub Actions with Android environment or similar CI services
3. **Keep dependencies updated**: Consider updating to newer stable versions of dependencies
4. **Test on multiple API levels**: Test on API 24 (minimum) through API 34 (target)
