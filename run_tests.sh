#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    elif [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

JUNIT_JAR="/Users/jeemainsadv/.gradle/wrapper/dists/gradle-8.11.1-all/2qik7nd48slq1ooc2496ixf4i/gradle-8.11.1/lib/junit-4.13.2.jar"
[ -f "$JUNIT_JAR" ] || JUNIT_JAR="/tmp/junit-4.13.2.jar"

HAMCREST_JAR="/Users/jeemainsadv/.gradle/wrapper/dists/gradle-8.11.1-all/2qik7nd48slq1ooc2496ixf4i/gradle-8.11.1/lib/hamcrest-core-1.3.jar"
[ -f "$HAMCREST_JAR" ] || HAMCREST_JAR="/tmp/hamcrest-core-1.3.jar"

JSON_JAR="/tmp/json-20240303.jar"
ANDROID_JAR="/opt/homebrew/share/android-commandlinetools/platforms/android-35/android.jar"
[ -f "$ANDROID_JAR" ] || ANDROID_JAR="/opt/homebrew/share/android-commandlinetools/platforms/android-34/android.jar"

TEST_OUT="$DIR/build-tests"
rm -rf "$TEST_OUT"
mkdir -p "$TEST_OUT"

echo "=== Compiling unit tests with OpenJDK 17 ==="
javac -d "$TEST_OUT" \
  -cp "$JSON_JAR:$JUNIT_JAR:$HAMCREST_JAR:$ANDROID_JAR" \
  $(find "$DIR/app/src/main/java/com/example/timerapp/data" -name "*.java") \
  $(find "$DIR/app/src/main/java/com/example/timerapp/model" -name "*.java") \
  $(find "$DIR/app/src/test/java/com/example/timerapp/data" -name "*.java")

echo "=== Running unit tests ==="
java -cp "$JSON_JAR:$TEST_OUT:$JUNIT_JAR:$HAMCREST_JAR:$ANDROID_JAR" \
  org.junit.runner.JUnitCore \
  com.example.timerapp.data.DualClockAndResetTest \
  com.example.timerapp.data.StudyCoachEngineTest \
  com.example.timerapp.data.JeeAnalyticsTest \
  com.example.timerapp.data.SafBackupRestoreTest \
  com.example.timerapp.data.AdvancedAnalyticsAndCsvTest \
  com.example.timerapp.data.AnalyticsAndDateGroupingTest \
  com.example.timerapp.data.BackupManagerTest \
  com.example.timerapp.data.TimerDatabaseManagerTest
