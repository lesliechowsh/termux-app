#!/usr/bin/env bash
# JVM-only JUnit harness for the terminal-emulator module.
#
# Compiles the transitive closure of the test sources against minimal android.*/androidx.*
# stubs (see stubs/ and README.md), then runs every *Test.java in the module's test dir with
# org.junit.runner.JUnitCore. New test classes are picked up automatically, no edit needed.
#
# Idempotent: full clean rebuild of harness/build on every run. Exits nonzero if compilation
# fails or any test fails. Needs a JDK (javac, java) and nothing else.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SRC_MAIN="$MODULE_DIR/src/main/java"
SRC_TEST_ROOT="$MODULE_DIR/src/test/java"
SRC_TEST="$SRC_TEST_ROOT/com/termux/terminal"
STUBS="$SCRIPT_DIR/stubs"
LIB="$SCRIPT_DIR/lib"
BUILD="$SCRIPT_DIR/build"
CLASSES_DIR="$BUILD/classes"
LOGS_DIR="$BUILD/logs"
PKG="com.termux.terminal"

for tool in javac java; do
    command -v "$tool" >/dev/null 2>&1 || { echo "ERROR: $tool not found (a JDK is required)" >&2; exit 1; }
done
for jar in junit-4.13.2.jar hamcrest-core-1.3.jar; do
    [ -f "$LIB/$jar" ] || { echo "ERROR: $LIB/$jar missing (see README.md)" >&2; exit 1; }
done

CLASSPATH_JARS="$LIB/junit-4.13.2.jar:$LIB/hamcrest-core-1.3.jar"

# --- Compile: test sources as roots; javac lazily pulls the closure from main sources and stubs.
rm -rf "$BUILD"
mkdir -p "$CLASSES_DIR" "$LOGS_DIR" || { echo "ERROR: cannot create $BUILD" >&2; exit 1; }

TEST_SOURCES=("$SRC_TEST"/*Test.java)
if [ ! -e "${TEST_SOURCES[0]}" ]; then
    echo "ERROR: no *Test.java found in $SRC_TEST" >&2
    exit 1
fi

javac -encoding UTF-8 -Xlint:none \
    -cp "$CLASSPATH_JARS" \
    -sourcepath "$STUBS:$SRC_MAIN:$SRC_TEST_ROOT" \
    -d "$CLASSES_DIR" \
    "${TEST_SOURCES[@]}" 2>&1 || { echo "COMPILATION FAILED" >&2; exit 1; }

# --- Run: one JUnitCore invocation per class for exact per-class PASS/FAIL.
TOTAL_TESTS=0; TOTAL_FAILURES=0; TOTAL_ERRORS=0
FAILED_CLASSES=(); PASSED_CLASSES=(); RUN_START=$(date +%s)

for source in "${TEST_SOURCES[@]}"; do
    name="$(basename "$source" .java)"
    class="$PKG.$name"
    log="$LOGS_DIR/$name.txt"
    if output=$(java -cp "$CLASSES_DIR:$CLASSPATH_JARS" org.junit.runner.JUnitCore "$class" 2>&1); then
        tests=$(printf '%s' "$output" | sed -n 's/^OK (\([0-9][0-9]*\) tests*)$/\1/p')
        echo "PASS $name ($tests tests)"
        PASSED_CLASSES+=("$name")
        TOTAL_TESTS=$((TOTAL_TESTS + tests))
    else
        echo "$output" > "$log"
        # JUnit 4.13.2 JUnitCore prints "Tests run: N,  Failures: F" (thrown exceptions count
        # as failures); an ",  Errors: E" suffix is parsed too in case a future junit prints it.
        summary=$(printf '%s' "$output" | sed -n \
            -e 's/^Tests run: \([0-9][0-9]*\),[[:space:]]\+Failures: \([0-9][0-9]*\),[[:space:]]\+Errors: \([0-9][0-9]*\)$/\1 \2 \3/p' \
            -e 's/^Tests run: \([0-9][0-9]*\),[[:space:]]\+Failures: \([0-9][0-9]*\)$/\1 \2 0/p')
        if [ -n "$summary" ]; then
            read -r tests failures errors <<<"$summary"
        else
            # JVM crash / no summary line: count the run as one error so it cannot pass silently.
            read -r tests failures errors <<<"0 0 1"
        fi
        echo "FAIL $name ($tests tests, $failures failures, $errors errors) — log: $log"
        FAILED_CLASSES+=("$name")
        TOTAL_TESTS=$((TOTAL_TESTS + tests))
        TOTAL_FAILURES=$((TOTAL_FAILURES + failures))
        TOTAL_ERRORS=$((TOTAL_ERRORS + errors))
    fi
done

ELAPSED=$(( $(date +%s) - RUN_START ))

echo
echo "=== Summary ==="
for name in "${PASSED_CLASSES[@]}"; do echo "PASS $name"; done
for name in "${FAILED_CLASSES[@]}"; do echo "FAIL $name"; done
echo "Classes: ${#PASSED_CLASSES[@]} passed, ${#FAILED_CLASSES[@]} failed, $((${#PASSED_CLASSES[@]} + ${#FAILED_CLASSES[@]})) total"
echo "Tests run: $TOTAL_TESTS, Failures: $TOTAL_FAILURES, Errors: $TOTAL_ERRORS (${ELAPSED}s)"

if [ "${#FAILED_CLASSES[@]}" -gt 0 ]; then
    echo
    echo "Failing output is in $LOGS_DIR. Last failures per class:"
    for name in "${FAILED_CLASSES[@]}"; do
        echo "--- $name ---"
        tail -n 20 "$LOGS_DIR/$name.txt"
    done
    exit 1
fi
exit 0
