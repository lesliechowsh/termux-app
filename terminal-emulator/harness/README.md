# JVM test harness for terminal-emulator

Runs the module's JUnit suite on the JVM with plain `javac`/`java` — no Android SDK or Gradle.
`stubs/` provides minimal `android.*`/`androidx.*` classes mirroring AGP's
`unitTests.returnDefaultValues = true` (methods return null/0/false), with honest exceptions:
`android.util.Base64` is real base64, `android.graphics.Color` does real math, `SystemClock`
is a monotonic `System.nanoTime` clock. Run: `bash terminal-emulator/harness/run-tests.sh`
(compiles the transitive closure of `src/test/java` against the stubs, runs every
`*Test.java`, exits nonzero on failure). Limitation: `android.graphics`/`android.os`
members are inert stubs, so behavior depending on decoded or drawn Bitmaps is NOT
exercised here — it only proves protocol/buffer logic. Jars (junit 4.13.2, hamcrest-core
1.3) live in `lib/`; refetch from Maven Central:
    curl -o lib/junit-4.13.2.jar https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
    curl -o lib/hamcrest-core-1.3.jar https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
