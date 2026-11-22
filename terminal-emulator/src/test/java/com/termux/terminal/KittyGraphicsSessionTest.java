package com.termux.terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * The command lifecycle: cancellation mid-command, a parked chunked upload meeting new work,
 * and what a screen switch does to uploads, stored images and placements still in flight.
 */
public class KittyGraphicsSessionTest extends KittyGraphicsTestCase {

    public void testScrollRecycleDropsThePlacementOfTheEvictedRow() {
        // The default transcript here is 2000 rows below the 3 screen rows: once it is full,
        // the next scroll recycles the oldest transcript slot — the image's anchor row — as
        // the blank bottom row. The placement died with its line: it must not render on the
        // recycled row.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        assertEquals(1, placements(7).size());
        StringBuilder linefeeds = new StringBuilder();
        linefeeds.append('\n'); // Reaches the bottom row.
        for (int i = 0; i < 2000; i++) linefeeds.append('\n'); // Scrolls, the last recycling the anchor.
        enterString(linefeeds.toString());
        assertEquals(0, placements(7).size());
        assertLinesAre("     ", "     ", "     ");
        assertCursorAt(2, 1); // The image advanced the cursor one column; linefeeds keep it.
    }

    public void testEraseInDisplayDropsPlacements() {
        // CSI 2J clears the screen; the placements on it are cleared with it.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        enterString("\033[2J");
        assertEquals(0, placements(7).size());
        assertLinesAre("     ", "     ", "     ");
    }

    public void testResetMidApcLeavesACleanParser() {
        // Half a non-kitty APC, then a reset (RIS) mid-APC: what follows must be classified
        // as if from a clean parser. A reset that bypassed the escape-state teardown left the
        // APC sequence length standing, so the unrelated APC below met a stale parser and a
        // real kitty upload was buffered under the plain 16 KiB sequence limit and silently
        // dropped.
        enterString("\033_x=1"); // Mid-APC, no terminator.
        mTerminal.reset();
        enterString("\033_a;b\033\\"); // Unrelated APC containing ';'.
        assertLinesAre("     ", "     ", "     ");
        assertEquals("", mOutput.getOutputAndClear());
        // A real kitty upload now streams and answers normally: its 16384-char base64 payload
        // (12288 pixel bytes) must never touch the buffered limit.
        StringBuilder payload = new StringBuilder();
        for (int i = 0; i < 4096; i++) payload.append("AQEB");
        enterString("\033_Ga=T,i=7,f=24,s=4096,v=1;" + payload + "\033\\");
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        assertLinesAre("     ", "     ", "     ");
    }

    public void testCancelMidPayloadDropsTheWholeCommand() {
        // CAN (0x18) is consumed silently: nothing is printed and the parser returns to ground.
        // The printed cell puts the cursor off the origin, so "cursor unmoved" is a real claim.
        enterString("X");
        enterString("\033_Ga=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3) + "\u0018");
        assertEquals("", mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertCursorAt(0, 1);
        // A resend of the same command succeeds as if nothing had been in flight — no
        // interruption notice, no leftover bytes from the cancelled payload.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
    }

    public void testCancelMidBufferedCommandDropsItSilently() {
        enterString("X");
        enterString("\033_Ga=q,f=24,s=1,v=1;" + base64(1, 2, 3) + "\u0018");
        assertEquals("", mOutput.getOutputAndClear());
        enterString(apc("a=q,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "OK"), mOutput.getOutputAndClear());
    }

    public void testSubCancelsABufferedCommandDropsItSilently() {
        // SUB (0x1A) cancels a buffered APC the same way CAN does: the store-only transmission
        // never reaches its terminator, so nothing is stored and nothing is answered.
        enterString("\033_Gi=1,a=t,f=24,s=1,v=1;" + base64(1, 2, 3));
        enterString("\u001A");
        assertEquals("", mOutput.getOutputAndClear());
        // The cancellation is observable: a later put finds no image, and the parser is clean.
        enterString(apc("a=p,i=1"));
        assertEquals(reply("i=1", "ENOENT:image not found"), mOutput.getOutputAndClear());
        enterString(apc("a=t,i=2,f=24,s=1,v=1;" + base64(4, 5, 6)));
        assertEquals(reply("i=2", "OK"), mOutput.getOutputAndClear());
    }

    public void testNewTransmissionInterruptsAParkedUpload() {
        enterString(apc("a=T,i=7,f=24,s=1,v=1,m=1;" + base64(1, 2, 3)));
        enterString(apc("a=t,i=8,f=24,s=1,v=1;" + base64(4, 5, 6)));
        assertEquals(reply("i=7", "EINVAL:chunk upload interrupted") + reply("i=8", "OK"),
            mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertEquals(1, kitty.store().count());
    }

    public void testQueriesDoNotInterruptAParkedUpload() {
        enterString(apc("a=T,i=7,f=24,s=1,v=1,m=1;" + base64(1, 2, 3)));
        enterString(apc("a=q,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "OK"), mOutput.getOutputAndClear());
        enterString(apc("m=0;"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
    }

    public void testScreenSwitchDropsAParkedUpload() {
        enterString(apc("a=T,i=7,f=24,s=1,v=1,m=1;" + base64(1, 2, 3)));
        enterString("\033[?1049h"); // to the alternate screen
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        // No "chunk upload interrupted" came back: half an image addressed to the screen being
        // left is not the start of an image on the screen being entered.
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
    }

    public void testStoredImagesSurviveAScreenSwitch() {
        enterString(apc("a=t,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        enterString("\033[?1049h");
        enterString(apc("a=p,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size()); // placed on the alternate screen
        enterString("\033[?1049l");
        // Leaving the alternate screen cleared it; the store itself is intact.
        assertEquals(0, placements(7).size());
        enterString(apc("a=p,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
    }

    public void testDecodeFinishingAfterAScreenSwitchPlacesNothingAndAnswersNothing() {
        // Hold the decode back until after the screen switch: its result belongs to the old
        // screen generation, so it must place nothing and answer nothing. The pixels still
        // land in the store, which is not screen state.
        List<Runnable> pending = new ArrayList<>();
        kitty.decoder = pending::add;
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals("", mOutput.getOutputAndClear());
        assertCursorAt(0, 1); // the cursor movement the placement owes was done at accept time
        enterString("\033[?1049h");
        for (Runnable task : new ArrayList<>(pending)) task.run();
        assertEquals("", mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertEquals(1, kitty.store().count());
        assertTrue(storedImage(7).completed);
    }
}
