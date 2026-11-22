package com.termux.terminal;

import android.graphics.Bitmap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;

/**
 * Fixture for the kitty graphics protocol suite. Everything here drives the real input path
 * ({@link TerminalTestCase#enterString}) and asserts on what a client of the emulator observes:
 * the reply bytes on the output stream, the placement layer over the buffer, the cursor, the
 * image store and the virtual placements.
 *
 * <p>The harness Bitmap stub decodes no pixels, so the suite also steps the decode worker by
 * hand ({@code kitty.decoder} and {@code kitty.decodeForTests}, the protocol's own test hooks):
 * decodes run inline, which makes every transmission, placement and reply deterministic. That
 * needs an instantiable Bitmap, which only a stub runtime provides; if none can be built the
 * suite fails loudly instead of reporting green with nothing running.</p>
 */
abstract class KittyGraphicsTestCase extends TerminalTestCase {

    /** The screen every test runs against. */
    static final int COLUMNS = 5;
    static final int ROWS = 3;

    /** The protocol behind the emulator, fetched once per test for its test hooks. */
    KittyGraphicsProtocol kitty;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (testBitmap() == null) {
            throw new AssertionError("needs an instantiable Bitmap (the JVM harness stub)");
        }
        withTerminalSized(COLUMNS, ROWS);
        kitty = kittyGraphics(mTerminal);
        kitty.decoder = Runnable::run;
        kitty.decodeForTests = () -> testBitmap();
    }

    private static Bitmap cachedBitmap;

    /**
     * One Bitmap instance shared by every decode. The stub reports zero bytes, so byte
     * accounting falls back to width*height*4 and nothing depends on which instance is handed
     * back. Built reflectively so the suite compiles against android.jar too, where Bitmap has
     * no usable constructor.
     */
    static Bitmap testBitmap() {
        if (cachedBitmap != null) return cachedBitmap;
        try {
            Constructor<Bitmap> constructor = Bitmap.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            cachedBitmap = constructor.newInstance();
            return cachedBitmap;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    static KittyGraphicsProtocol kittyGraphics(TerminalEmulator terminal) {
        try {
            Field field = TerminalEmulator.class.getDeclaredField("mKittyGraphics");
            field.setAccessible(true);
            return (KittyGraphicsProtocol) field.get(terminal);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("TerminalEmulator has no mKittyGraphics field", e);
        }
    }

    static String apc(String controlAndPayload) {
        return "\033_G" + controlAndPayload + "\033\\";
    }

    static String reply(String keys, String status) {
        return "\033_G" + keys + ";" + status + "\033\\";
    }

    static String base64(int... bytes) {
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) data[i] = (byte) bytes[i];
        return base64(data);
    }

    static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** The 24 leading bytes of a PNG: signature plus an IHDR chunk declaring width x height. */
    static byte[] pngHeader(int width, int height) {
        byte[] png = new byte[24];
        png[0] = (byte) 0x89;
        png[1] = 'P';
        png[2] = 'N';
        png[3] = 'G';
        png[4] = 13;
        png[5] = 10;
        png[6] = 26;
        png[7] = 10;
        png[12] = 'I';
        png[13] = 'H';
        png[14] = 'D';
        png[15] = 'R';
        for (int i = 0; i < 4; i++) {
            png[16 + i] = (byte) (width >>> (24 - 8 * i));
            png[20 + i] = (byte) (height >>> (24 - 8 * i));
        }
        return png;
    }

    /** Every placement of the image on either buffer, with the anchor row filled in. */
    List<KittyPlacement> placements(long imageId) {
        return mTerminal.kittyPlacementsFor(imageId);
    }

    KittyImageStore.Entry storedImage(long id) {
        return kitty.store().get(id);
    }
}
