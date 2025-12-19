#!/bin/bash

GRADLE_FILE="app/build.gradle.kts"

# Check if file exists
if [ ! -f "$GRADLE_FILE" ]; then
    echo "Error: $GRADLE_FILE not found!"
    exit 1
fi

# Read current versionCode
CURRENT_CODE=$(grep "versionCode =" "$GRADLE_FILE" | awk '{print $3}')
NEW_CODE=$((CURRENT_CODE + 1))

# Read current versionName (format "x.y.z")
CURRENT_NAME=$(grep "versionName =" "$GRADLE_FILE" | cut -d'"' -f2)
IFS='.' read -r -a VERSION_PARTS <<< "$CURRENT_NAME"

MAJOR=${VERSION_PARTS[0]}
MINOR=${VERSION_PARTS[1]}
PATCH=${VERSION_PARTS[2]}

NEW_PATCH=$((PATCH + 1))
NEW_NAME="$MAJOR.$MINOR.$NEW_PATCH"

# Update file using sed (compatible with Linux/GNU sed)
sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i "s/versionName = \"$CURRENT_NAME\"/versionName = \"$NEW_NAME\"/" "$GRADLE_FILE"

echo "Updated Version:"
echo "  versionCode: $CURRENT_CODE -> $NEW_CODE"
echo "  versionName: $CURRENT_NAME -> $NEW_NAME"
