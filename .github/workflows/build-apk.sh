#!/bin/bash

echo "===== Gamepad to ESP32 Controller APK Builder ====="
echo "This script will help you build the APK on your local machine."
echo ""

# Define color codes for better readability
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}There are two ways to build this APK:${NC}"
echo -e "${GREEN}OPTION 1: Using Android Studio (Recommended)${NC}"
echo "1. Open Android Studio"
echo "2. Select 'Open an Existing Project' and navigate to this folder"
echo "3. Let Gradle sync complete"
echo "4. In the menu, select Build → Build Bundle(s) / APK(s) → Build APK(s)"
echo "5. The APK will be in app/build/outputs/apk/debug/app-debug.apk"
echo ""

echo -e "${GREEN}OPTION 2: Using Command Line${NC}"
echo "This requires having Android SDK installed and configured."
echo ""

# Check if Android Studio or Android SDK is installed
if command -v java >/dev/null 2>&1 ; then
    echo -e "${BLUE}Java found.${NC}"
else
    echo -e "${RED}Java not found. Please install JDK 8 or newer.${NC}"
    echo "Download from: https://adoptopenjdk.net/"
    exit 1
fi

# Check if ANDROID_SDK_ROOT or ANDROID_HOME is set
if [ -z "$ANDROID_SDK_ROOT" ] && [ -z "$ANDROID_HOME" ]; then
    echo -e "${YELLOW}ANDROID_SDK_ROOT or ANDROID_HOME environment variable is not set.${NC}"
    
    # Try to detect Android SDK in common locations
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
        echo -e "${GREEN}Detected Android SDK at: $ANDROID_SDK_ROOT${NC}"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
        echo -e "${GREEN}Detected Android SDK at: $ANDROID_SDK_ROOT${NC}"
    else
        echo "Please set it to your Android SDK location, for example:"
        echo "export ANDROID_SDK_ROOT=~/Android/Sdk  # on Linux/Mac"
        echo "set ANDROID_SDK_ROOT=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk  # on Windows"
        echo ""
        echo -e "${YELLOW}Continuing anyway, will try to use the gradlew wrapper...${NC}"
    fi
else
    if [ -n "$ANDROID_SDK_ROOT" ]; then
        echo -e "${GREEN}Using Android SDK at: $ANDROID_SDK_ROOT${NC}"
    else
        echo -e "${GREEN}Using Android SDK at: $ANDROID_HOME${NC}"
        export ANDROID_SDK_ROOT="$ANDROID_HOME"
    fi
fi

echo "Making gradlew executable..."
chmod +x gradlew

echo -e "${BLUE}Do you want to attempt building the APK now? (y/n)${NC}"
read -r BUILD_NOW

if [[ $BUILD_NOW =~ ^[Yy]$ ]]; then
    echo "Cleaning project..."
    ./gradlew clean
    
    echo "Building debug APK..."
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        APK_PATH="./app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$APK_PATH" ]; then
            echo ""
            echo -e "${GREEN}===== BUILD SUCCESSFUL =====${NC}"
            echo "APK generated at: $APK_PATH"
            echo ""
            echo "You can install it on your device using:"
            echo "adb install $APK_PATH"
            echo ""
            echo "Or transfer the APK to your device manually."
        else
            echo -e "${YELLOW}Build succeeded but APK file not found at expected location.${NC}"
        fi
    else
        echo ""
        echo -e "${RED}===== BUILD FAILED =====${NC}"
        echo "Check the error messages above for details."
        echo -e "${YELLOW}Please try using Android Studio instead.${NC}"
    fi
else
    echo -e "${BLUE}Build skipped. Please use Android Studio to build the APK.${NC}"
fi