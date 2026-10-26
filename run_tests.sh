#!/usr/bin/env bash
set -euo pipefail

# Physical path resolution resisting symlink traversal
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

# Safe JAVA_HOME resolution
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

# Dynamic Gradle wrapper / JUnit JAR resolution
GRADLE_DIR="${GRADLE_USER_HOME:-${HOME:-}/.gradle}"
JUNIT_JAR="${JUNIT_JAR:-}"
if [ -z "$JUNIT_JAR" ] || [ ! -f "$JUNIT_JAR" ]; then
    if [ -n "$GRADLE_DIR" ] && [ -d "$GRADLE_DIR" ]; then
        JUNIT_JAR="$(find "$GRADLE_DIR" -name "junit-4*.jar" 2>/dev/null | head -n 1 || true)"
    fi
fi
if [ -z "$JUNIT_JAR" ] || [ ! -f "$JUNIT_JAR" ]; then
    if [ -f "/tmp/junit-4.13.2.jar" ]; then
        JUNIT_JAR="/tmp/junit-4.13.2.jar"
    elif [ -f "/usr/share/java/junit4.jar" ]; then
        JUNIT_JAR="/usr/share/java/junit4.jar"
    fi
fi

# Dynamic Hamcrest JAR resolution
HAMCREST_JAR="${HAMCREST_JAR:-}"
if [ -z "$HAMCREST_JAR" ] || [ ! -f "$HAMCREST_JAR" ]; then
    if [ -n "$GRADLE_DIR" ] && [ -d "$GRADLE_DIR" ]; then
        HAMCREST_JAR="$(find "$GRADLE_DIR" -name "hamcrest-core*.jar" 2>/dev/null | head -n 1 || true)"
    fi
fi
if [ -z "$HAMCREST_JAR" ] || [ ! -f "$HAMCREST_JAR" ]; then
    if [ -f "/tmp/hamcrest-core-1.3.jar" ]; then
        HAMCREST_JAR="/tmp/hamcrest-core-1.3.jar"
    elif [ -f "/usr/share/java/hamcrest-core.jar" ]; then
        HAMCREST_JAR="/usr/share/java/hamcrest-core.jar"
    fi
fi

JSON_JAR="/tmp/json-20240303.jar"
if [ ! -f "$JSON_JAR" ]; then
    mkdir -p /tmp
    curl -fsSL -o "$JSON_JAR" https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar 2>/dev/null || true
fi
ANDROID_JAR="/opt/homebrew/share/android-commandlinetools/platforms/android-35/android.jar"
[ -f "$ANDROID_JAR" ] || ANDROID_JAR="/opt/homebrew/share/android-commandlinetools/platforms/android-34/android.jar"

# Verify all required dependency JARs exist and are regular files
for req_jar in "$JUNIT_JAR" "$HAMCREST_JAR" "$JSON_JAR" "$ANDROID_JAR"; do
    if [ ! -f "$req_jar" ]; then
        echo "ERROR: Required JAR dependency missing or not a regular file: $req_jar" >&2
        exit 1
    fi
done

TEST_OUT="$DIR/build-tests"
# Guarded cleanup preventing unbounded or root directory deletion
if [ -n "$TEST_OUT" ] && [ "$TEST_OUT" != "/" ] && [ "$TEST_OUT" = "$DIR/build-tests" ]; then
    rm -rf "$TEST_OUT"
fi
mkdir -p "$TEST_OUT"

echo "=== Compiling unit tests with OpenJDK 17 ==="
JAVA_SOURCES_FILE="$TEST_OUT/sources.txt"
find "$DIR/app/src/main/java/com/example/timerapp/data" \
     "$DIR/app/src/main/java/com/example/timerapp/model" \
     "$DIR/app/src/test/java/com/example/timerapp" \
     -name "*.java" > "$JAVA_SOURCES_FILE"

"$JAVA_HOME/bin/javac" -d "$TEST_OUT" \
  -cp "$JSON_JAR:$JUNIT_JAR:$HAMCREST_JAR:$ANDROID_JAR" \
  -encoding UTF-8 \
  @"$JAVA_SOURCES_FILE"

echo "=== Running unit tests with ReportRunner ==="
XML_REPORT="$TEST_OUT/junit-report.xml"
JSON_REPORT="$TEST_OUT/test-results.json"

TEST_EXIT_CODE=0
"$JAVA_HOME/bin/java" -cp "$JSON_JAR:$TEST_OUT:$JUNIT_JAR:$HAMCREST_JAR:$ANDROID_JAR" \
  com.example.timerapp.testrunner.ReportRunner \
  --xml "$XML_REPORT" \
  --json "$JSON_REPORT" \
  com.example.timerapp.data.DualClockAndResetTest \
  com.example.timerapp.data.StudyCoachEngineTest \
  com.example.timerapp.data.JeeAnalyticsTest \
  com.example.timerapp.data.SafBackupRestoreTest \
  com.example.timerapp.data.AdvancedAnalyticsAndCsvTest \
  com.example.timerapp.data.AnalyticsAndDateGroupingTest \
  com.example.timerapp.data.BackupManagerTest \
  com.example.timerapp.data.TimerDatabaseManagerTest \
  com.example.timerapp.data.SensitiveDataRedactionTest \
  com.example.timerapp.data.SessionStateMachineTest \
  com.example.timerapp.data.TwoZoneHitDetectionAndUndoTest \
  com.example.timerapp.data.EstimatedMarksCalculatorTest \
  com.example.timerapp.data.TargetCountdownMathTest \
  com.example.timerapp.data.SchemaV4AndSafSettingsTest \
  com.example.timerapp.data.DatabaseV4MigrationAndAuditTest \
  com.example.timerapp.data.OutcomeQuestionTimerAndAnalyticsTest || TEST_EXIT_CODE=$?

SWARM_DIR="${SWARM_DIR:-${SWARM_ROOT:-$(cd "$DIR/../Swarm" 2>/dev/null && pwd -P || echo "${HOME:-}/Swarm")}}"
ARCHIVE_DIR="$SWARM_DIR/metrics/tests"
mkdir -p "$ARCHIVE_DIR/archive"

if [ -f "$XML_REPORT" ]; then
  rm -f "$ARCHIVE_DIR/junit-report.xml"
  cp "$XML_REPORT" "$ARCHIVE_DIR/junit-report.xml"
  TIMESTAMP=$(date +%Y%m%d_%H%M%S)
  cp "$XML_REPORT" "$ARCHIVE_DIR/archive/junit-report-${TIMESTAMP}.xml"
fi

if [ -f "$JSON_REPORT" ]; then
  rm -f "$ARCHIVE_DIR/test-results.json"
  cp "$JSON_REPORT" "$ARCHIVE_DIR/test-results.json"
  TIMESTAMP=$(date +%Y%m%d_%H%M%S)
  cp "$JSON_REPORT" "$ARCHIVE_DIR/archive/test-results-${TIMESTAMP}.json"
fi

echo "Archived test results to $ARCHIVE_DIR"
exit "$TEST_EXIT_CODE"
