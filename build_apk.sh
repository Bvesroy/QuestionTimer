#!/usr/bin/env bash
set -euo pipefail

# Physical path resolution resisting symlink traversal
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
SWARM_DIR="${SWARM_DIR:-${SWARM_ROOT:-$(cd "$DIR/../Swarm" 2>/dev/null && pwd -P || echo "${HOME:-}/Swarm")}}"

# Helper function for monotonic time in milliseconds
mono_ms() {
    python3 -c 'import time; print(int(time.monotonic() * 1000))'
}

BUILD_START_MS=$(mono_ms)
BUILD_TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Resolve Active Session ID if present
ACTIVE_SESSION_ID="${SWARM_SESSION_ID:-}"
if [ -z "$ACTIVE_SESSION_ID" ] && [ -f "$SWARM_DIR/metrics/.active_session.json" ]; then
    ACTIVE_SESSION_ID=$(python3 -c "import json; print(json.load(open('$SWARM_DIR/metrics/.active_session.json')).get('session_id', ''))" 2>/dev/null || echo "")
fi

# Track stages and durations for reconciliation and failure recovery
CURRENT_STAGE="init"
CURRENT_STAGE_KEY="init"
CURRENT_STAGE_START=$BUILD_START_MS

AAPT2_COMPILE_MS=0
AAPT2_LINK_MS=0
JAVAC_COMPILE_MS=0
D8_DEX_MS=0
JAR_PACKAGE_MS=0
ZIPALIGN_MS=0
APKSIGNER_SIGN_MS=0
APKSIGNER_VERIFY_MS=0
CHECKSUM_VERIFY_MS=0

# Trap errors to record structured failure metrics with non-zero exit status
record_failure() {
    local exit_code=$?
    trap - ERR
    set +e
    local line_no="${1:-$LINENO}"
    [ "$exit_code" -eq 0 ] && exit_code=1

    local fail_time_ms
    fail_time_ms=$(mono_ms)
    local total_dur=$(( fail_time_ms - BUILD_START_MS ))
    local failed_stage_dur=$(( fail_time_ms - CURRENT_STAGE_START ))

    echo "" >&2
    echo "========================================================" >&2
    echo "BUILD FAILED at line $line_no in stage '${CURRENT_STAGE}'" >&2
    echo "Exit Code:     $exit_code" >&2
    echo "Stage Elapsed: ${failed_stage_dur}ms" >&2
    echo "Total Elapsed: ${total_dur}ms" >&2
    echo "========================================================" >&2

    case "$CURRENT_STAGE_KEY" in
        aapt2_compile)    AAPT2_COMPILE_MS=$failed_stage_dur ;;
        aapt2_link)       AAPT2_LINK_MS=$failed_stage_dur ;;
        javac_compile)    JAVAC_COMPILE_MS=$failed_stage_dur ;;
        d8_dex)           D8_DEX_MS=$failed_stage_dur ;;
        jar_package)      JAR_PACKAGE_MS=$failed_stage_dur ;;
        zipalign)         ZIPALIGN_MS=$failed_stage_dur ;;
        apksigner_sign)   APKSIGNER_SIGN_MS=$failed_stage_dur ;;
        apksigner_verify) APKSIGNER_VERIFY_MS=$failed_stage_dur ;;
        checksum_verify)  CHECKSUM_VERIFY_MS=$failed_stage_dur ;;
        *)                ;;
    esac

    python3 - <<EOF
import json
import os
import sys
from pathlib import Path

stages = {
    "aapt2_compile": ${AAPT2_COMPILE_MS:-0},
    "aapt2_link": ${AAPT2_LINK_MS:-0},
    "javac_compile": ${JAVAC_COMPILE_MS:-0},
    "d8_dex": ${D8_DEX_MS:-0},
    "jar_package": ${JAR_PACKAGE_MS:-0},
    "zipalign": ${ZIPALIGN_MS:-0},
    "apksigner_sign": ${APKSIGNER_SIGN_MS:-0},
    "apksigner_verify": ${APKSIGNER_VERIFY_MS:-0},
    "checksum_verify": ${CHECKSUM_VERIFY_MS:-0}
}

stage_meta = [
    ("aapt2_compile", "AAPT2 compile"),
    ("aapt2_link", "AAPT2 link"),
    ("javac_compile", "Javac compile"),
    ("d8_dex", "D8 dex"),
    ("jar_package", "jar package"),
    ("zipalign", "zipalign"),
    ("apksigner_sign", "apksigner sign"),
    ("apksigner_verify", "apksigner verify"),
    ("checksum_verify", "checksum verify")
]

failed_key = "${CURRENT_STAGE_KEY}"
stage_list = []
found_failed = False

for key, name in stage_meta:
    dur = stages[key]
    if key == failed_key:
        stage_list.append({"name": name, "duration_ms": dur, "status": "FAILED"})
        found_failed = True
    elif not found_failed:
        stage_list.append({"name": name, "duration_ms": dur, "status": "SUCCESS"})
    else:
        stage_list.append({"name": name, "duration_ms": 0, "status": "SKIPPED"})

if not found_failed and failed_key not in stages:
    stage_list.insert(0, {"name": "${CURRENT_STAGE}", "duration_ms": ${failed_stage_dur}, "status": "FAILED"})

stages_total = sum(s["duration_ms"] for s in stage_list)
total_dur = ${total_dur}
uninstrumented_overhead = max(0, total_dur - stages_total)

metrics_data = {
    "session_id": "${ACTIVE_SESSION_ID}" if "${ACTIVE_SESSION_ID}" else None,
    "timestamp": "${BUILD_TIMESTAMP}",
    "exit_code": ${exit_code},
    "success": False,
    "failed_stage": "${CURRENT_STAGE}",
    "failed_line": ${line_no},
    "total_duration_ms": total_dur,
    "stages_total_ms": stages_total,
    "uninstrumented_overhead_ms": uninstrumented_overhead,
    "stages": stages,
    "stage_list": stage_list,
    "artifact": None
}

builds_dir = Path(os.environ.get("SWARM_METRICS_BUILDS_DIR", "${SWARM_DIR}/metrics/builds"))
builds_dir.mkdir(parents=True, exist_ok=True)

latest_path = builds_dir / "latest.json"
tmp_path = builds_dir / f".tmp.{os.getpid()}.json"
with open(tmp_path, "w", encoding="utf-8") as f:
    json.dump(metrics_data, f, indent=2)
    f.flush()
    os.fsync(f.fileno())
os.replace(tmp_path, latest_path)

sid = "${ACTIVE_SESSION_ID}"
if sid:
    archive_path = builds_dir / f"{sid}_build.json"
    with open(archive_path, "w", encoding="utf-8") as f:
        json.dump(metrics_data, f, indent=2)

print(f"Failure metrics recorded in {latest_path}")
EOF

    COLLECTOR_SCRIPT="${SWARM_DIR}/metrics/collector.py"
    if [ -f "$COLLECTOR_SCRIPT" ] && [ -n "${ACTIVE_SESSION_ID:-}" ]; then
        python3 "$COLLECTOR_SCRIPT" record-build --session-id "$ACTIVE_SESSION_ID" --build-file "${SWARM_DIR}/metrics/builds/latest.json" 2>/dev/null || true
    fi

    exit "$exit_code"
}

trap 'record_failure $LINENO' ERR

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
CURRENT_STAGE="AAPT2 compile"
CURRENT_STAGE_KEY="aapt2_compile"
S1_START=$(mono_ms)
CURRENT_STAGE_START=$S1_START
"$BUILD_TOOLS/aapt2" compile --dir "$DIR/app/src/main/res" -o "$BUILD_DIR/resources.zip"
S1_END=$(mono_ms)
AAPT2_COMPILE_MS=$(( S1_END - S1_START ))
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S1_END
echo "      AAPT2 compile finished in ${AAPT2_COMPILE_MS}ms"

# -------------------------------------------------------------
# Stage 2: AAPT2 link
# -------------------------------------------------------------
echo "[2/9] Linking resources & generating R.java..."
CURRENT_STAGE="AAPT2 link"
CURRENT_STAGE_KEY="aapt2_link"
S2_START=$(mono_ms)
CURRENT_STAGE_START=$S2_START
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
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S2_END
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

CURRENT_STAGE="Javac compile"
CURRENT_STAGE_KEY="javac_compile"
S3_START=$(mono_ms)
CURRENT_STAGE_START=$S3_START
"$JAVA_HOME/bin/javac" \
    -cp "$CP" \
    -d "$BUILD_DIR/obj" \
    -source 17 -target 17 \
    -encoding UTF-8 \
    @"$JAVA_SOURCES_FILE"
S3_END=$(mono_ms)
JAVAC_COMPILE_MS=$(( S3_END - S3_START ))
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S3_END
echo "      Javac compile finished in ${JAVAC_COMPILE_MS}ms"

# -------------------------------------------------------------
# Stage 4: D8 dex
# -------------------------------------------------------------
echo "[4/9] Converting bytecode with D8..."
CLASS_SOURCES_FILE="$BUILD_DIR/classes.txt"
find "$BUILD_DIR/obj" -name "*.class" > "$CLASS_SOURCES_FILE"

CURRENT_STAGE="D8 dex"
CURRENT_STAGE_KEY="d8_dex"
S4_START=$(mono_ms)
CURRENT_STAGE_START=$S4_START
"$BUILD_TOOLS/d8" \
    --lib "$PLATFORM_JAR" \
    --min-api 26 \
    --output "$BUILD_DIR/apk" \
    @"$CLASS_SOURCES_FILE"
S4_END=$(mono_ms)
D8_DEX_MS=$(( S4_END - S4_START ))
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S4_END
echo "      D8 dex finished in ${D8_DEX_MS}ms"

# -------------------------------------------------------------
# Stage 5: jar package
# -------------------------------------------------------------
echo "[5/9] Packaging DEX into unaligned APK..."
CURRENT_STAGE="jar package"
CURRENT_STAGE_KEY="jar_package"
S5_START=$(mono_ms)
CURRENT_STAGE_START=$S5_START
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
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S5_END
echo "      Jar packaging finished in ${JAR_PACKAGE_MS}ms"

# -------------------------------------------------------------
# Stage 6: zipalign
# -------------------------------------------------------------
echo "[6/9] Aligning APK with zipalign..."
CURRENT_STAGE="zipalign"
CURRENT_STAGE_KEY="zipalign"
S6_START=$(mono_ms)
CURRENT_STAGE_START=$S6_START
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/zipalign" -c -v 4 "$BUILD_DIR/aligned.apk" > /dev/null
S6_END=$(mono_ms)
ZIPALIGN_MS=$(( S6_END - S6_START ))
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S6_END
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
if [ -n "${MIRROR_DIR:-}" ]; then
    MIRROR_APK="$MIRROR_DIR/QuestionTimer.apk"
elif [ -n "${MIRROR_APK:-}" ]; then
    : # already specified via environment
elif [ -n "${HOME:-}" ] && [ -d "$HOME" ] && [ -w "$HOME" ]; then
    MIRROR_APK="$HOME/QuestionTimer.apk"
else
    MIRROR_APK="$DIR/QuestionTimer.apk"
fi

CURRENT_STAGE="apksigner sign"
CURRENT_STAGE_KEY="apksigner_sign"
S7_START=$(mono_ms)
CURRENT_STAGE_START=$S7_START
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
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S7_END
echo "      Apksigner sign finished in ${APKSIGNER_SIGN_MS}ms"

# -------------------------------------------------------------
# Stage 8: apksigner verify
# -------------------------------------------------------------
echo "[8/9] Verifying APK signature..."
CURRENT_STAGE="apksigner verify"
CURRENT_STAGE_KEY="apksigner_verify"
S8_START=$(mono_ms)
CURRENT_STAGE_START=$S8_START
"$BUILD_TOOLS/apksigner" verify --verbose "$RELEASE_APK" > "$BUILD_DIR/verify_output.txt"
S8_END=$(mono_ms)
APKSIGNER_VERIFY_MS=$(( S8_END - S8_START ))
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S8_END
echo "      Apksigner verify finished in ${APKSIGNER_VERIFY_MS}ms"

# Mirror to user root with symlink hijacking prevention
rm -f "$MIRROR_APK"
cp "$RELEASE_APK" "$MIRROR_APK"
chmod 644 "$MIRROR_APK"

# -------------------------------------------------------------
# Stage 9: checksum verify
# -------------------------------------------------------------
echo "[9/9] Verifying SHA-256 provenance checksum..."
CURRENT_STAGE="checksum verify"
CURRENT_STAGE_KEY="checksum_verify"
S9_START=$(mono_ms)
CURRENT_STAGE_START=$S9_START
RELEASE_SHA=$(shasum -a 256 "$RELEASE_APK" | awk '{print $1}')
MIRROR_SHA=$(shasum -a 256 "$MIRROR_APK" | awk '{print $1}')

if [ "$RELEASE_SHA" != "$MIRROR_SHA" ]; then
    echo "ERROR: Checksum mismatch between release and mirror APK!" >&2
    exit 1
fi
S9_END=$(mono_ms)
CHECKSUM_VERIFY_MS=$(( S9_END - S9_START ))
CURRENT_STAGE="idle"
CURRENT_STAGE_KEY="idle"
CURRENT_STAGE_START=$S9_END
echo "      Checksum verification finished in ${CHECKSUM_VERIFY_MS}ms"

# Versioned preservation: QuestionTimer_v4_release.apk in timestorage/
TIMESTORAGE_DIR="$DIR/timestorage"
mkdir -p "$TIMESTORAGE_DIR"
V4_RELEASE_APK="$TIMESTORAGE_DIR/QuestionTimer_v4_release.apk"
rm -f "$V4_RELEASE_APK"
cp "$RELEASE_APK" "$V4_RELEASE_APK"
chmod 644 "$V4_RELEASE_APK"
V4_SHA=$(shasum -a 256 "$V4_RELEASE_APK" | awk '{print $1}')

if [ "$RELEASE_SHA" != "$V4_SHA" ]; then
    echo "ERROR: Checksum mismatch between release and timestorage v4 APK!" >&2
    exit 1
fi
echo "      Preserved versioned snapshot: $V4_RELEASE_APK (SHA-256: $V4_SHA)"

# -------------------------------------------------------------
# Final telemetry aggregation & atomic write
# -------------------------------------------------------------
BUILD_END_MS=$(mono_ms)
TOTAL_DURATION_MS=$(( BUILD_END_MS - BUILD_START_MS ))
STAGES_TOTAL_MS=$(( AAPT2_COMPILE_MS + AAPT2_LINK_MS + JAVAC_COMPILE_MS + D8_DEX_MS + JAR_PACKAGE_MS + ZIPALIGN_MS + APKSIGNER_SIGN_MS + APKSIGNER_VERIFY_MS + CHECKSUM_VERIFY_MS ))
UNINSTRUMENTED_OVERHEAD_MS=$(( TOTAL_DURATION_MS - STAGES_TOTAL_MS ))

INIT_OVERHEAD_MS=$(( S1_START - BUILD_START_MS ))
GAP_1_2_MS=$(( S2_START - S1_END ))
GAP_2_3_MS=$(( S3_START - S2_END ))
GAP_3_4_MS=$(( S4_START - S3_END ))
GAP_4_5_MS=$(( S5_START - S4_END ))
GAP_5_6_MS=$(( S6_START - S5_END ))
GAP_6_7_MS=$(( S7_START - S6_END ))
GAP_7_8_MS=$(( S8_START - S7_END ))
GAP_8_9_MS=$(( S9_START - S8_END ))
POST_OVERHEAD_MS=$(( BUILD_END_MS - S9_END ))

APK_SIZE=$(wc -c < "$RELEASE_APK" | tr -d ' ')

echo "=== Build & Verification Complete ==="
echo "Artifact:    $RELEASE_APK"
echo "Mirror:      $MIRROR_APK"
echo "Preserved:   $V4_RELEASE_APK"
echo "Size:        $APK_SIZE bytes"
echo "SHA-256:     $RELEASE_SHA"
echo "Total Time:  ${TOTAL_DURATION_MS}ms"
echo "Stages Time: ${STAGES_TOTAL_MS}ms"
echo "Overhead:    ${UNINSTRUMENTED_OVERHEAD_MS}ms"

# Export structured build telemetry to Swarm metrics
python3 - <<EOF
import json
import os
import sys
from pathlib import Path

stages = {
    "aapt2_compile": ${AAPT2_COMPILE_MS},
    "aapt2_link": ${AAPT2_LINK_MS},
    "javac_compile": ${JAVAC_COMPILE_MS},
    "d8_dex": ${D8_DEX_MS},
    "jar_package": ${JAR_PACKAGE_MS},
    "zipalign": ${ZIPALIGN_MS},
    "apksigner_sign": ${APKSIGNER_SIGN_MS},
    "apksigner_verify": ${APKSIGNER_VERIFY_MS},
    "checksum_verify": ${CHECKSUM_VERIFY_MS}
}
stages_total = sum(stages.values())
total_duration = ${TOTAL_DURATION_MS}
uninstrumented_overhead = total_duration - stages_total

# Reconcile mathematical integrity
assert stages_total + uninstrumented_overhead == total_duration, (
    f"Mathematical inconsistency: sum({stages_total}) + overhead({uninstrumented_overhead}) != total({total_duration})"
)

init_setup_ms = ${INIT_OVERHEAD_MS}
gap_1_2_ms = ${GAP_1_2_MS}
gap_2_3_ms = ${GAP_2_3_MS}
gap_3_4_ms = ${GAP_3_4_MS}
gap_4_5_ms = ${GAP_4_5_MS}
gap_5_6_ms = ${GAP_5_6_MS}
gap_6_7_ms = ${GAP_6_7_MS}
gap_7_8_ms = ${GAP_7_8_MS}
gap_8_9_ms = ${GAP_8_9_MS}
post_verification_ms = ${POST_OVERHEAD_MS}

sum_overhead_components = (
    init_setup_ms + gap_1_2_ms + gap_2_3_ms + gap_3_4_ms +
    gap_4_5_ms + gap_5_6_ms + gap_6_7_ms + gap_7_8_ms +
    gap_8_9_ms + post_verification_ms
)

assert sum_overhead_components == uninstrumented_overhead, (
    f"Overhead breakdown mismatch: sum({sum_overhead_components}) != overhead({uninstrumented_overhead})"
)

metrics_data = {
    "session_id": "${ACTIVE_SESSION_ID}" if "${ACTIVE_SESSION_ID}" else None,
    "timestamp": "${BUILD_TIMESTAMP}",
    "exit_code": 0,
    "success": True,
    "total_duration_ms": total_duration,
    "stages_total_ms": stages_total,
    "uninstrumented_overhead_ms": uninstrumented_overhead,
    "stages": stages,
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
    "overhead_breakdown": {
        "init_setup_ms": init_setup_ms,
        "stage_gap_1_2_ms": gap_1_2_ms,
        "stage_gap_2_3_ms": gap_2_3_ms,
        "stage_gap_3_4_ms": gap_3_4_ms,
        "stage_gap_4_5_ms": gap_4_5_ms,
        "stage_gap_5_6_ms": gap_5_6_ms,
        "stage_gap_6_7_ms": gap_6_7_ms,
        "stage_gap_7_8_ms": gap_7_8_ms,
        "stage_gap_8_9_ms": gap_8_9_ms,
        "post_verification_ms": post_verification_ms
    },
    "artifact": {
        "path": "${RELEASE_APK}",
        "bytes": ${APK_SIZE},
        "sha256": "${RELEASE_SHA}",
        "verified": True
    }
}

builds_dir = Path(os.environ.get("SWARM_METRICS_BUILDS_DIR", "${SWARM_DIR}/metrics/builds"))
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
COLLECTOR_SCRIPT="${SWARM_DIR}/metrics/collector.py"
if [ -f "$COLLECTOR_SCRIPT" ] && [ -n "$ACTIVE_SESSION_ID" ]; then
    python3 "$COLLECTOR_SCRIPT" record-build --session-id "$ACTIVE_SESSION_ID" --build-file "${SWARM_DIR}/metrics/builds/latest.json" 2>/dev/null || true
    python3 "$COLLECTOR_SCRIPT" record-artifact --session-id "$ACTIVE_SESSION_ID" --path "$RELEASE_APK" --verified 2>/dev/null || true
fi
