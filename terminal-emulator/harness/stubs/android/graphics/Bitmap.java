package android.graphics;

/**
 * JVM harness stub mirroring returnDefaultValues=true: factories return null, getters return
 * 0/false, mutators are no-ops. Behavior that depends on decoded bitmaps is NOT exercised on
 * the JVM; see harness/README.md.
 */
public class Bitmap {

    public enum Config {
        ALPHA_8,
        RGB_565,
        ARGB_4444,
        ARGB_8888
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return null;
    }

    public static Bitmap createBitmap(int[] colors, int width, int height, Config config) {
        return null;
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        return null;
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        return null;
    }

    public int getWidth() { return 0; }

    public int getHeight() { return 0; }

    public int getAllocationByteCount() { return 0; }

    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {}

    public void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {}

    public void setPixel(int x, int y, int color) {}

    public void eraseColor(int color) {}

    public void recycle() {}

    public boolean isRecycled() { return false; }
}
