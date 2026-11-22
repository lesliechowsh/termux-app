package android.graphics;

/**
 * JVM harness stub mirroring returnDefaultValues=true: decodeByteArray returns null and the
 * bounds-populating fields of Options stay 0/null, matching what AGP unit tests see.
 */
public class BitmapFactory {

    public static class Options {
        public boolean inJustDecodeBounds;
        public int inSampleSize;
        public int outWidth;
        public int outHeight;
        public Bitmap.Config inPreferredConfig;
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return null;
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options options) {
        return null;
    }
}
