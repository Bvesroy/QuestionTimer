#!/usr/bin/env bash
set -euo pipefail

# Physical path resolution resisting symlink traversal
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

# Helper function for monotonic time in milliseconds
mono_ms() {
    python3 -c 'import time; print(int(time.monotonic() * 1000))'
}

BUILD_START_MS=$(mono_ms)
BUILD_TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Resolve Active Session ID if present
ACTIVE_SESSION_ID="${SWARM_SESSION_ID:-}"
if [ -z "$ACTIVE_SESSION_ID" ] && [ -f "/Users/jeemainsadv/Swarm/metrics/.active_session.json" ]; then
    ACTIVE_SESSION_ID=$(python3 -c "import json; print(json.load(open('/Users/jeemainsadv/Swarm/metrics/.active_session.json')).get('session_id', ''))" 2>/dev/null || echo "")
fi

# Ensure JAVA_HOME points to Java 17
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    elif [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
    fi
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; then
    echo "ERROR: Valid JAVA_HOME with javac not found." >&2
    exit 1
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

if [ -z "${BUILD_TOOLS:-}" ] || [ ! -d "$BUILD_TOOLS" ]; then
    echo "ERROR: Android build tools not found in $ANDROID_SDK" >&2
    exit 1
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

if [ -z "${PLATFORM_JAR:-}" ] || [ ! -f "$PLATFORM_JAR" ]; then
    echo "ERROR: Android platform JAR not found." >&2
    exit 1
fi

echo "=== Building QuestionTimer.apk ==="
echo "DIR:            $DIR"
echo "JAVA_HOME:      $JAVA_HOME"
echo "BUILD_TOOLS:    $BUILD_TOOLS"
echo "PLATFORM:       $PLATFORM_JAR"
if [ -n "$ACTIVE_SESSION_ID" ]; then
    echo "SESSION_ID:     $ACTIVE_SESSION_ID"
fi

BUILD_DIR="$DIR/build"
if [ -n "$BUILD_DIR" ] && [ "$BUILD_DIR" != "/" ] && [ "$BUILD_DIR" = "$DIR/build" ]; then
    rm -rf "$BUILD_DIR"
fi
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/apk" "$DIR/release"

# -------------------------------------------------------------
# Stage 1: AAPT2 compile
# -------------------------------------------------------------
echo "[1/9] Compiling resources with AAPT2..."
S1_START=$(mono_ms)
"$BUILD_TOOLS/aapt2" compile --dir "$DIR/app/src/main/res" -o "$BUILD_DIR/resources.zip"
S1_END=$(mono_ms)
AAPT2_COMPILE_MS=$(( S1_END - S1_START ))
echo "      AAPT2 compile finished in ${AAPT2_COMPILE_MS}ms"

# -------------------------------------------------------------
# Stage 2: AAPT2 link
# -------------------------------------------------------------
echo "[2/9] Linking resources & generating R.java..."
S2_START=$(mono_ms)
if [ -d "$DIR/app/src/main/assets" ]; then
    "$BUILD_TOOLS/aapt2" link \
        -I "$PLATFORM_JAR" \
        --manifest "$DIR/app/src/main/AndroidManifest.xml" \
        --java "$BUILD_DIR/gen" \
        -o "$BUILD_DIR/linked.apk" \
        "$BUILD_DIR/resources.zip" \
        -A "$DIR/app/src/main/assets" \
        --auto-add-overlay
else
    "$BUILD_TOOLS/aapt2" link \
        -I "$PLATFORM_JAR" \
        --manifest "$DIR/app/src/main/AndroidManifest.xml" \
        --java "$BUILD_DIR/gen" \
        -o "$BUILD_DIR/linked.apk" \
        "$BUILD_DIR/resources.zip" \
        --auto-add-overlay
fi
S2_END=$(mono_ms)
AAPT2_LINK_MS=$(( S2_END - S2_START ))
echo "      AAPT2 link finished in ${AAPT2_LINK_MS}ms"

# -------------------------------------------------------------
# Stage 3: Javac compile
# -------------------------------------------------------------
echo "[3/9] Compiling Java sources with javac 17..."
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

S3_START=$(mono_ms)
"$JAVA_HOME/bin/javac" \
    -cp "$CP" \
    -d "$BUILD_DIR/obj" \
    -source 17 -target 17 \
    -encoding UTF-8 \
    @"$JAVA_SOURCES_FILE"
S3_END=$(mono_ms)
JAVAC_COMPILE_MS=$(( S3_END - S3_START ))
echo "      Javac compile finished in ${JAVAC_COMPILE_MS}ms"

# -------------------------------------------------------------
# Stage 4: D8 dex
# -------------------------------------------------------------
echo "[4/9] Converting bytecode with D8..."
CLASS_SOURCES_FILE="$BUILD_DIR/classes.txt"
find "$BUILD_DIR/obj" -name "*.class" > "$CLASS_SOURCES_FILE"

S4_START=$(mono_ms)
"$BUILD_TOOLS/d8" \
    --lib "$PLATFORM_JAR" \
    --min-api 26 \
    --output "$BUILD_DIR/apk" \
    @"$CLASS_SOURCES_FILE"
S4_END=$(mono_ms)
D8_DEX_MS=$(( S4_END - S4_START ))
echo "      D8 dex finished in ${D8_DEX_MS}ms"

# -------------------------------------------------------------
# Stage 5: jar package
# -------------------------------------------------------------
echo "[5/9] Packaging DEX into unaligned APK..."
S5_START=$(mono_ms)
cp "$BUILD_DIR/linked.apk" "$BUILD_DIR/unaligned.apk"
(
    cd "$BUILD_DIR/apk"
    if compgen -G "classes*.dex" > /dev/null; then
        "$JAVA_HOME/bin/jar" -uf "$BUILD_DIR/unaligned.apk" classes*.dex
    else
        echo "ERROR: No dex files found in $BUILD_DIR/apk" >&2
        exit 1
    fi
)
S5_END=$(mono_ms)
JAR_PACKAGE_MS=$(( S5_END - S5_START ))
echo "      Jar packaging finished in ${JAR_PACKAGE_MS}ms"

# -------------------------------------------------------------
# Stage 6: zipalign
# -------------------------------------------------------------
echo "[6/9] Aligning APK with zipalign..."
S6_START=$(mono_ms)
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/zipalign" -c -v 4 "$BUILD_DIR/aligned.apk" > /dev/null
S6_END=$(mono_ms)
ZIPALIGN_MS=$(( S6_END - S6_START ))
echo "      Zipalign verified in ${ZIPALIGN_MS}ms (4-byte alignment)"

# Keystore check/generate & permissions hardening
KEYSTORE="$DIR/debug.keystore"
export KS_PASS="${KEYSTORE_PASS:-android}"

if [ ! -f "$KEYSTORE" ]; then
    echo "Generating debug keystore at $KEYSTORE..."
    "$JAVA_HOME/bin/keytool" -genkeypair -v \
        -keystore "$KEYSTORE" -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass:env KS_PASS -keypass:env KS_PASS \
        -dname "CN=Android Debug,O=Android,C=US"
fi

# Hardening: Restrict keystore to owner read/write (0600)
chmod 600 "$KEYSTORE"

# Verify signing keystore is strictly isolated from release artifact directory
if [[ "$KEYSTORE" == "$DIR/release"* ]]; then
    echo "SECURITY ERROR: Signing keystore must not reside inside release artifact directory!" >&2
    exit 1
fi

# -------------------------------------------------------------
# Stage 7: apksigner sign
# -------------------------------------------------------------
echo "[7/9] Signing APK with v2 and v3 signatures..."
RELEASE_APK="$DIR/release/QuestionTimer.apk"
MIRROR_APK="/Users/jeemainsadv/QuestionTimer.apk"

S7_START=$(mono_ms)
"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass "env:KS_PASS" \
    --key-pass "env:KS_PASS" \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out "$RELEASE_APK" \
    "$BUILD_DIR/aligned.apk"

chmod 644 "$RELEASE_APK"
S7_END=$(mono_ms)
APKSIGNER_SIGN_MS=$(( S7_END - S7_START ))
echo "      Apksigner sign finished in ${APKSIGNER_SIGN_MS}ms"

# -------------------------------------------------------------
# Stage 8: apksigner verify
# -------------------------------------------------------------
echo "[8/9] Verifying APK signature..."
S8_START=$(mono_ms)
"$BUILD_TOOLS/apksigner" verify --verbose "$RELEASE_APK" > "$BUILD_DIR/verify_output.txt"
S8_END=$(mono_ms)
APKSIGNER_VERIFY_MS=$(( S8_END - S8_START ))
echo "      Apksigner verify finished in ${APKSIGNER_VERIFY_MS}ms"

# Mirror to user root with symlink hijacking prevention
rm -f "$MIRROR_APK"
cp "$RELEASE_APK" "$MIRROR_APK"
chmod 644 "$MIRROR_APK"

# -------------------------------------------------------------
# Stage 9: checksum verify
# -------------------------------------------------------------
echo "[9/9] Verifying SHA-256 provenance checksum..."
S9_START=$(mono_ms)
RELEASE_SHA=$(shasum -a 256 "$RELEASE_APK" | awk '{print $1}')
MIRROR_SHA=$(shasum -a 256 "$MIRROR_APK" | awk '{print $1}')

if [ "$RELEASE_SHA" != "$MIRROR_SHA" ]; then
    echo "ERROR: Checksum mismatch between release and mirror APK!" >&2
    exit 1
fi
S9_END=$(mono_ms)
CHECKSUM_VERIFY_MS=$(( S9_END - S9_START ))
echo "      Checksum verification finished in ${CHECKSUM_VERIFY_MS}ms"

# -------------------------------------------------------------
# Final telemetry aggregation & atomic write
# -------------------------------------------------------------
BUILD_END_MS=$(mono_ms)
TOTAL_DURATION_MS=$(( BUILD_END_MS - BUILD_START_MS ))
APK_SIZE=$(wc -c < "$RELEASE_APK" | tr -d ' ')

echo "=== Build & Verification Complete ==="
echo "Artifact:    $RELEASE_APK"
echo "Mirror:      $MIRROR_APK"
echo "Size:        $APK_SIZE bytes"
echo "SHA-256:     $RELEASE_SHA"
echo "Total Time:  ${TOTAL_DURATION_MS}ms"

# Export structured build telemetry to Swarm metrics
python3 - <<EOF
import json
import os
import sys
from pathlib import Path

metrics_data = {
    "session_id": "${ACTIVE_SESSION_ID}" if "${ACTIVE_SESSION_ID}" else None,
    "timestamp": "${BUILD_TIMESTAMP}",
    "exit_code": 0,
    "success": True,
    "total_duration_ms": ${TOTAL_DURATION_MS},
    "stages": {
        "aapt2_compile": ${AAPT2_COMPILE_MS},
        "aapt2_link": ${AAPT2_LINK_MS},
        "javac_compile": ${JAVAC_COMPILE_MS},
        "d8_dex": ${D8_DEX_MS},
        "jar_package": ${JAR_PACKAGE_MS},
        "zipalign": ${ZIPALIGN_MS},
        "apksigner_sign": ${APKSIGNER_SIGN_MS},
        "apksigner_verify": ${APKSIGNER_VERIFY_MS},
        "checksum_verify": ${CHECKSUM_VERIFY_MS}
    },
    "stage_list": [
        {"name": "AAPT2 compile", "duration_ms": ${AAPT2_COMPILE_MS}, "status": "SUCCESS"},
        {"name": "AAPT2 link", "duration_ms": ${AAPT2_LINK_MS}, "status": "SUCCESS"},
        {"name": "Javac compile", "duration_ms": ${JAVAC_COMPILE_MS}, "status": "SUCCESS"},
        {"name": "D8 dex", "duration_ms": ${D8_DEX_MS}, "status": "SUCCESS"},
        {"name": "jar package", "duration_ms": ${JAR_PACKAGE_MS}, "status": "SUCCESS"},
        {"name": "zipalign", "duration_ms": ${ZIPALIGN_MS}, "status": "SUCCESS"},
        {"name": "apksigner sign", "duration_ms": ${APKSIGNER_SIGN_MS}, "status": "SUCCESS"},
        {"name": "apksigner verify", "duration_ms": ${APKSIGNER_VERIFY_MS}, "status": "SUCCESS"},
        {"name": "checksum verify", "duration_ms": ${CHECKSUM_VERIFY_MS}, "status": "SUCCESS"}
    ],
    "artifact": {
        "path": "${RELEASE_APK}",
        "bytes": ${APK_SIZE},
        "sha256": "${RELEASE_SHA}",
        "verified": True
    }
}

builds_dir = Path("/Users/jeemainsadv/Swarm/metrics/builds")
builds_dir.mkdir(parents=True, exist_ok=True)

# Atomic write to latest.json
latest_path = builds_dir / "latest.json"
tmp_path = builds_dir / f".tmp.{os.getpid()}.json"
with open(tmp_path, "w", encoding="utf-8") as f:
    json.dump(metrics_data, f, indent=2)
    f.flush()
    os.fsync(f.fileno())
os.replace(tmp_path, latest_path)

# Archive per session if session_id is available
sid = "${ACTIVE_SESSION_ID}"
if sid:
    archive_path = builds_dir / f"{sid}_build.json"
    with open(archive_path, "w", encoding="utf-8") as f:
        json.dump(metrics_data, f, indent=2)

print(f"Metrics written to {latest_path}")
EOF

# Notify active Swarm session collector if running
COLLECTOR_SCRIPT="/Users/jeemainsadv/Swarm/metrics/collector.py"
if [ -f "$COLLECTOR_SCRIPT" ] && [ -n "$ACTIVE_SESSION_ID" ]; then
    python3 "$COLLECTOR_SCRIPT" record-build --session-id "$ACTIVE_SESSION_ID" --build-file "/Users/jeemainsadv/Swarm/metrics/builds/latest.json" 2>/dev/null || true
    python3 "$COLLECTOR_SCRIPT" record-artifact --session-id "$ACTIVE_SESSION_ID" --path "$RELEASE_APK" --verified 2>/dev/null || true
fi
