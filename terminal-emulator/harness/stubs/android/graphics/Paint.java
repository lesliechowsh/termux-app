package android.graphics;

/**
 * JVM harness stub mirroring returnDefaultValues=true: setters are no-ops, metrics return 0.
 * Members exist only so the terminal-view renderer sources type-check on the JVM; no behavior.
 */
public class Paint {

    public static final int FILTER_BITMAP_FLAG = 0x04;

    public Paint() {}

    public Paint(int flags) {}

    public void setTypeface(Typeface typeface) {}

    public void setTextSize(float textSize) {}

    public void setAntiAlias(boolean aa) {}

    public void setColor(int color) {}

    public void setFakeBoldText(boolean fakeBoldText) {}

    public void setUnderlineText(boolean underlineText) {}

    public void setTextSkewX(float skewX) {}

    public void setStrikeThruText(boolean strikeThruText) {}

    public float measureText(String text) { return 0; }

    public float measureText(CharSequence text, int start, int end) { return 0; }

    public float measureText(char[] text, int index, int count) { return 0; }

    public float getFontSpacing() { return 0; }

    public float ascent() { return 0; }
}
