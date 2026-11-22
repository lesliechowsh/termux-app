package com.termux.terminal;

import java.util.zip.Deflater;

/**
 * The a=q validation surface and the reply rules: which errors a malformed command earns, how
 * ids are echoed back, and what the q flag suppresses.
 */
public class KittyGraphicsReplyTest extends KittyGraphicsTestCase {

    public void testQueryAnswersOkForMatchingRgbBytes() {
        enterString(apc("a=q,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "OK"), mOutput.getOutputAndClear());
    }

    public void testQueryRejectsRgbBytesOfWrongLength() {
        enterString(apc("a=q,f=24,s=1,v=1;" + base64(1, 2)));
        assertEquals(reply("", "EINVAL:invalid image data"), mOutput.getOutputAndClear());
    }

    public void testQueryByteCountFollowsThePixelFormat() {
        // f=32 asks for four bytes per pixel, so three bytes are refused and four accepted.
        enterString(apc("a=q,f=32,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "EINVAL:invalid image data"), mOutput.getOutputAndClear());
        enterString(apc("a=q,f=32,s=1,v=1;" + base64(1, 2, 3, 4)));
        assertEquals(reply("", "OK"), mOutput.getOutputAndClear());
    }

    public void testQueryValidatesPngHeader() {
        enterString(apc("a=q,f=100;" + base64(pngHeader(2, 3))));
        assertEquals(reply("", "OK"), mOutput.getOutputAndClear());
        enterString(apc("a=q,f=100;" + base64(1, 2, 3)));
        assertEquals(reply("", "EINVAL:invalid image data"), mOutput.getOutputAndClear());
    }

    public void testQueryValidatesCompressedPixelData() {
        // o=z asks the terminal to inflate before checking; the inflated size must match s and v.
        enterString(apc("a=q,o=z,f=24,s=1,v=1;" + base64(deflate(new byte[] {1, 2, 3}))));
        assertEquals(reply("", "OK"), mOutput.getOutputAndClear());
        enterString(apc("a=q,o=z,f=24,s=1,v=1;" + base64(deflate(new byte[] {1, 2}))));
        assertEquals(reply("", "EINVAL:invalid image data"), mOutput.getOutputAndClear());
    }

    public void testQueryEchoesImageAndNumberIdentifiers() {
        enterString(apc("a=q,i=9,I=3,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=9,I=3", "OK"), mOutput.getOutputAndClear());
    }

    public void testQueryRejectsUnsupportedFormat() {
        enterString(apc("a=q,f=5,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "ENOSYS:unsupported image format"), mOutput.getOutputAndClear());
    }

    public void testQueryRejectsUnsupportedMedium() {
        enterString(apc("a=q,t=c,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "ENOSYS:unsupported transmission medium"), mOutput.getOutputAndClear());
    }

    public void testQueryRejectsInvalidBase64Payload() {
        enterString(apc("a=q,f=24,s=1,v=1;!!"));
        assertEquals(reply("", "EINVAL:invalid base64 payload"), mOutput.getOutputAndClear());
    }

    public void testUnsupportedActionIsAnsweredWhenAddressed() {
        // An unsupported action is only answered when the client addressed an image to it;
        // an anonymous one is refused silently, like every other id-less refusal.
        enterString(apc("a=z"));
        assertEquals("", mOutput.getOutputAndClear());
        enterString(apc("a=z,i=7"));
        assertEquals(reply("i=7", "ENOSYS:unsupported action"), mOutput.getOutputAndClear());
    }

    public void testUnsupportedCompressionIsAnswered() {
        enterString(apc("a=T,i=7,f=24,s=1,v=1,o=y;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "ENOSYS:unsupported compression"), mOutput.getOutputAndClear());
    }

    public void testRawFormatRequiresSizeKeys() {
        // f defaults to 32, so a transmission without s= and v= names no pixel geometry.
        enterString(apc("a=T,i=7;" + base64(1, 2, 3, 4)));
        assertEquals(reply("i=7", "EINVAL:raw pixel data requires s and v"), mOutput.getOutputAndClear());
    }

    public void testOversizedRawImageIsRefused() {
        // 3000x3000 RGBA pixels decode to 36MB, over the 32MB session limit.
        enterString(apc("a=T,i=7,f=32,s=3000,v=3000;"));
        assertEquals(reply("i=7", "ENOSPC:decoded image exceeds session limit"), mOutput.getOutputAndClear());
    }

    public void testMalformedControlDataIsAnswered() {
        // A control entry shorter than "k=v" is malformed; the answer carries no command keys.
        enterString(apc("q"));
        assertEquals(reply("", "EINVAL:malformed control key"), mOutput.getOutputAndClear());
    }

    public void testOutOfRangeQuietValueIsAnswered() {
        enterString(apc("q=3"));
        assertEquals(reply("", "EINVAL:invalid q value"), mOutput.getOutputAndClear());
    }

    public void testQuietOneSuppressesSuccessButKeepsErrors() {
        enterString(apc("a=q,f=24,s=1,v=1,q=1;" + base64(1, 2, 3)));
        assertEquals("", mOutput.getOutputAndClear());
        // The success of a stored transmission is silenced the same way.
        enterString(apc("a=t,i=7,q=1,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals("", mOutput.getOutputAndClear());
        assertEquals(1, kitty.store().count());
        // But an error still comes back: an application asking for silence on success still
        // has to learn why its data was refused.
        enterString(apc("a=q,f=24,s=1,v=1,q=1;" + base64(1, 2)));
        assertEquals(reply("", "EINVAL:invalid image data"), mOutput.getOutputAndClear());
    }

    public void testQuietTwoSuppressesSuccessAndErrors() {
        enterString(apc("a=q,f=24,s=1,v=1,q=2;" + base64(1, 2, 3)));
        assertEquals("", mOutput.getOutputAndClear());
        enterString(apc("a=q,f=24,s=1,v=1,q=2;" + base64(1, 2)));
        assertEquals("", mOutput.getOutputAndClear());
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater();
        try {
            deflater.setInput(data);
            deflater.finish();
            byte[] buffer = new byte[64];
            int length = deflater.deflate(buffer);
            byte[] out = new byte[length];
            System.arraycopy(buffer, 0, out, 0, length);
            return out;
        } finally {
            deflater.end();
        }
    }
}
