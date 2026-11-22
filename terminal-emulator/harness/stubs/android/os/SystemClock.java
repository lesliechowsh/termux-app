package android.os;

/** JVM harness stub: monotonic clock from System.nanoTime, honest where Android is monotonic. */
public final class SystemClock {

    private SystemClock() {}

    public static long uptimeMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    public static long elapsedRealtime() {
        return System.nanoTime() / 1_000_000L;
    }
}
