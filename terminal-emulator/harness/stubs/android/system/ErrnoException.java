package android.system;

/** JVM harness stub: checked exception type so Os signatures compile unchanged. */
public class ErrnoException extends Exception {

    public ErrnoException() {}

    public ErrnoException(String errMsg, int errno) {
        super(errMsg);
    }
}
