#!/bin/bash

# Ensure we are in the script's directory (native-app)
cd "$(dirname "$0")"

echo "🧹 Cleaning project..."
./gradlew clean

echo "🏗️  Building Debug APK..."
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    APK_PATH="$(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        echo ""
        echo "✅ Build Successful!"
        echo "📦 APK Location:"
        echo "👉 $APK_PATH"
        echo ""
    else
        echo "❌ Build reported success but APK file not found at expected path."
    fi
else
    echo "❌ Build Failed."
    exit 1
fi
