#!/bin/bash

# Ensure we are in the script's directory (native-app)
cd "$(dirname "$0")"

echo "🧹 Cleaning project..."
./gradlew clean

echo "🏗️  Building Debug APK..."
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    APK_DIR="$(pwd)/app/build/outputs/apk/debug"
    ORIGINAL_APK="$APK_DIR/app-debug.apk"
    
    # Extract version info
    GRADLE_FILE="app/build.gradle.kts"
    VERSION_NAME=$(grep "versionName =" "$GRADLE_FILE" | cut -d'"' -f2)
    VERSION_CODE=$(grep "versionCode =" "$GRADLE_FILE" | awk '{print $3}')
    
    NEW_APK_NAME="v${VERSION_NAME}.apk"
    NEW_APK_PATH="$APK_DIR/$NEW_APK_NAME"

    if [ -f "$ORIGINAL_APK" ]; then
        mv "$ORIGINAL_APK" "$NEW_APK_PATH"
        
        echo ""
        echo "✅ Build Successful!"
        echo "📦 APK Created:"
        echo "👉 $NEW_APK_PATH"
        echo ""
    else
        echo "❌ Build reported success but APK file not found at expected path."
    fi
else
    echo "❌ Build Failed."
    exit 1
fi
