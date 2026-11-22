package android.graphics;

/**
 * JVM harness stub mirroring returnDefaultValues=true: draw calls are no-ops, save() returns 0.
 * Members exist only so the terminal-view renderer sources type-check on the JVM; no behavior.
 */
public class Canvas {

    public int save() { return 0; }

    public void restore() {}

    public void restoreToCount(int saveCount) {}

    public boolean clipRect(float left, float top, float right, float bottom) { return false; }

    public void scale(float sx, float sy) {}

    public void drawColor(int color, PorterDuff.Mode mode) {}

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {}

    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {}

    public void drawText(char[] text, int index, int count, float x, float y, Paint paint) {}

    public void drawTextRun(char[] text, int index, int count, int contextIndex, int contextCount,
                            float x, float y, boolean isRtl, Paint paint) {}
}
