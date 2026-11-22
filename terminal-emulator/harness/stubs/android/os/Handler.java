package android.os;

/**
 * JVM harness stub mirroring returnDefaultValues=true: obtainMessage returns null and
 * sendMessage returns false. handleMessage is only invoked by a real looper, which never runs
 * on the JVM, so subclass overrides stay inert.
 */
public class Handler {

    public Handler() {}

    public void handleMessage(Message msg) {}

    public Message obtainMessage(int what, Object obj) {
        return null;
    }

    public boolean sendMessage(Message msg) {
        return false;
    }

    public boolean sendEmptyMessage(int what) {
        return false;
    }
}
