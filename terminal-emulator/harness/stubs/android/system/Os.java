package android.system;

/** JVM harness stub: no-op Os calls; kill is a no-op on the JVM. */
public final class Os {

    private Os() {}

    public static void kill(int pid, int signal) throws ErrnoException {}
}
