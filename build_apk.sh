#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Ensure JAVA_HOME points to Java 17
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    elif [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

# Resolve Android SDK
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/homebrew/share/android-commandlinetools}}"

# Resolve build tools (prefer 35.0.0 or 34.0.0)
if [ -z "${BUILD_TOOLS:-}" ]; then
    for bt in "$ANDROID_SDK/build-tools/35.0.0" "$ANDROID_SDK/build-tools/34.0.0"; do
        if [ -d "$bt" ]; then
            BUILD_TOOLS="$bt"
            break
        fi
    done
fi

# Resolve Android platform JAR
if [ -z "${PLATFORM_JAR:-}" ]; then
    for pf in "$ANDROID_SDK/platforms/android-35/android.jar" "$ANDROID_SDK/platforms/android-34/android.jar"; do
        if [ -f "$pf" ]; then
            PLATFORM_JAR="$pf"
            break
        fi
    done
fi

echo "=== Building QuestionTimer.apk ==="
echo "DIR:         $DIR"
echo "JAVA_HOME:   $JAVA_HOME"
echo "BUILD_TOOLS: $BUILD_TOOLS"
echo "PLATFORM:    $PLATFORM_JAR"

BUILD_DIR="$DIR/build"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/apk" "$DIR/release"

# 1. Compile resources (including drawables, vector assets, layouts, values)
echo "[1/8] Compiling resources with AAPT2..."
"$BUILD_TOOLS/aapt2" compile --dir "$DIR/app/src/main/res" -o "$BUILD_DIR/resources.zip"

# 2. Link resources & generate R.java
echo "[2/8] Linking resources & generating R.java..."
EXTRA_LINK_FLAGS=""
if [ -d "$DIR/app/src/main/assets" ]; then
    EXTRA_LINK_FLAGS="-A $DIR/app/src/main/assets"
fi

"$BUILD_TOOLS/aapt2" link \
    -I "$PLATFORM_JAR" \
    --manifest "$DIR/app/src/main/AndroidManifest.xml" \
    --java "$BUILD_DIR/gen" \
    -o "$BUILD_DIR/linked.apk" \
    "$BUILD_DIR/resources.zip" \
    $EXTRA_LINK_FLAGS \
    --auto-add-overlay

# 3. Compile Java sources
echo "[3/8] Compiling Java sources with javac 17..."
JAVA_SOURCES_FILE="$BUILD_DIR/sources.txt"
find "$DIR/app/src/main/java" "$BUILD_DIR/gen" -name "*.java" > "$JAVA_SOURCES_FILE"

CP="$PLATFORM_JAR"
if [ -d "$DIR/app/libs" ]; then
    for jar in "$DIR/app/libs"/*.jar; do
        if [ -f "$jar" ]; then
            CP="$CP:$jar"
        fi
    done
fi

"$JAVA_HOME/bin/javac" \
    -cp "$CP" \
    -d "$BUILD_DIR/obj" \
    -source 17 -target 17 \
    -encoding UTF-8 \
    @"$JAVA_SOURCES_FILE"

# 4. Dex bytecode with D8
echo "[4/8] Converting bytecode with D8..."
CLASS_SOURCES_FILE="$BUILD_DIR/classes.txt"
find "$BUILD_DIR/obj" -name "*.class" > "$CLASS_SOURCES_FILE"

"$BUILD_TOOLS/d8" \
    --lib "$PLATFORM_JAR" \
    --min-api 26 \
    --output "$BUILD_DIR/apk" \
    @"$CLASS_SOURCES_FILE"

# 5. Package APK
echo "[5/8] Packaging DEX into unaligned APK..."
cp "$BUILD_DIR/linked.apk" "$BUILD_DIR/unaligned.apk"
cd "$BUILD_DIR/apk"
"$JAVA_HOME/bin/jar" -uf "$BUILD_DIR/unaligned.apk" classes*.dex
cd "$DIR"

# 6. Zipalign
echo "[6/8] Aligning APK with zipalign..."
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/zipalign" -c -v 4 "$BUILD_DIR/aligned.apk" > /dev/null
echo "Zipalign verification: SUCCESS (4-byte alignment verified)"

# 7. Keystore check/generate
echo "[7/8] Checking keystore..."
KEYSTORE="$DIR/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    echo "Generating debug keystore at $KEYSTORE..."
    "$JAVA_HOME/bin/keytool" -genkey -v \
        -keystore "$KEYSTORE" -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Android Debug,O=Android,C=US"
fi

# 8. Sign APK with apksigner (v2 and v3 signatures)
echo "[8/8] Signing APK with v2 and v3 signatures..."
RELEASE_APK="$DIR/release/QuestionTimer.apk"
MIRROR_APK="/Users/jeemainsadv/QuestionTimer.apk"

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out "$RELEASE_APK" \
    "$BUILD_DIR/aligned.apk"

# Verify APK signature
echo "=== Verifying APK Signature ==="
"$BUILD_TOOLS/apksigner" verify --verbose "$RELEASE_APK"

# Mirror to user root
echo "=== Mirroring Artifact ==="
cp -f "$RELEASE_APK" "$MIRROR_APK"

# Provenance and checksum verification
RELEASE_SHA=$(shasum -a 256 "$RELEASE_APK" | awk '{print $1}')
MIRROR_SHA=$(shasum -a 256 "$MIRROR_APK" | awk '{print $1}')

if [ "$RELEASE_SHA" != "$MIRROR_SHA" ]; then
    echo "ERROR: Checksum mismatch between release and mirror APK!" >&2
    exit 1
fi

echo "=== Build & Verification Complete ==="
echo "Artifact:    $RELEASE_APK"
echo "Mirror:      $MIRROR_APK"
echo "Size:        $(wc -c < "$RELEASE_APK" | tr -d ' ') bytes"
echo "SHA-256:     $RELEASE_SHA"
