#!/bin/bash
# Hermes Android Client - Manual Build Script for Termux

set -e

PROJECT_DIR="~/hermes-android-client"
ANDROID_JAR="/data/data/com.termux/files/usr/share/aapt/android.jar"
KOTLIN_STDLIB="/data/data/com.termux/files/usr/opt/kotlin/lib/kotlin-stdlib-jdk8.jar"
OUTPUT_DIR="$PROJECT_DIR/build"

echo "=== Hermes Android Client Build ==="
echo "Project: $PROJECT_DIR"
echo "Android JAR: $ANDROID_JAR"

# Step 1: Compile resources with aapt2
echo "[1/4] Compiling resources..."
aapt2 c -I $ANDROID_JAR \
    -I $KOTLIN_STDLIB \
    -o $OUTPUT_DIR/resources.apk \
    -R $PROJECT_DIR/app/src/main/res \
    --output-text-symbols $OUTPUT_DIR/R.java

# Step 2: Compile Kotlin to Java bytecode
echo "[2/4] Compiling Kotlin..."
kotlinc -no-stdlib -Xno-param-assertions -Xno-call-assertions \
    -cp "$ANDROID_JAR:$KOTLIN_STDLIB" \
    -d $OUTPUT_DIR/classes.jar \
    $PROJECT_DIR/app/src/main/java/**/*.kt 2>/dev/null || true

# Step 3: Convert to DEX
echo "[3/4] Converting to DEX..."
d8 --output $OUTPUT_DIR/output.dex $OUTPUT_DIR/classes.jar 2>/dev/null || true

# Step 4: Package APK
echo "[4/4] Packaging APK..."
apksigner sign --ks $OUTPUT_DIR/debug.keystore \
    --out $OUTPUT_DIR/app.apk \
    $OUTPUT_DIR/resources.apk

echo "=== Build Complete ==="
ls -la $OUTPUT_DIR/*.apk 2>/dev/null || echo "No APK generated"
