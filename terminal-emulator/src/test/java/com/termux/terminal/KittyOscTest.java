package com.termux.terminal;

/**
 * The OSC escapes that reach the client or the extended clipboard: the notification escapes
 * (OSC 9, OSC 777, OSC 99) and OSC 5522 write packets longer than the default OSC argument
 * limit.
 */
public class KittyOscTest extends TerminalTestCase {

    public void testOsc9ReachesTheClient() {
        // iTerm2's growl-style notification: OSC 9 ; body BEL, no title.
        withTerminalSized(5, 3).enterString("\033]9;build done\007");
        assertEquals(1, mOutput.notifications.size());
        assertEquals(new TerminalTestCase.Notification(null, "build done"), mOutput.notifications.get(0));
        assertLinesAre("     ", "     ", "     ");
    }

    public void testOsc777ReachesTheClient() {
        // rxvt-unicode's notification: OSC 777 ; notify ; title ; body BEL.
        withTerminalSized(5, 3).enterString("\033]777;notify;Title;Body\007");
        assertEquals(1, mOutput.notifications.size());
        assertEquals(new TerminalTestCase.Notification("Title", "Body"), mOutput.notifications.get(0));
        assertLinesAre("     ", "     ", "     ");
    }

    public void testOsc99NotificationReachesTheOldNotificationPath() {
        // TerminalOutput's onKittyNotification default promises to pass the request along the
        // plainer onNotification path, so a client that only knows title and body keeps working.
        withTerminalSized(5, 3).enterString("\033]99;i=n1;Hello\007");
        assertEquals(1, mOutput.notifications.size());
        assertEquals(new TerminalTestCase.Notification("Hello", ""), mOutput.notifications.get(0));
        assertEquals("", mOutput.getOutputAndClear());
    }

    public void testOsc5522WriteBeyondTheDefaultOscLimitDoesNotLeakToTheScreen() {
        // A write packet's base64 payload runs far past the 16384-char default OSC argument
        // limit. Collecting must survive it whole: an overflow mid-packet would finish the
        // sequence and print the rest of the base64 on the screen.
        withTerminalSized(5, 3);
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < 4097; i++) chunk.append("QUJD"); // 16388 base64 chars, "ABC" x 4097
        String mime = "dGV4dC9wbGFpbg=="; // base64("text/plain")
        enterString("\033]5522;type=write:mime=" + mime + ";\033\\");
        enterString("\033]5522;type=wdata:mime=" + mime + ";" + chunk + "\033\\");
        enterString("\033]5522;type=wdata;\033\\"); // Closes the write and answers DONE.
        assertEquals("\033]5522;type=write:status=DONE\033\\", mOutput.getOutputAndClear());
        assertEquals(1, mOutput.clipboardPuts.size());
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 4097; i++) text.append("ABC");
        assertEquals(text.toString(), mOutput.clipboardPuts.get(0));
        assertLinesAre("     ", "     ", "     ");
    }

    public void testOsc5522WriteBeyondThePerPacketCapIsRefusedCleanly() {
        // A write chunk over the per-packet cap must not overflow mid-packet and print the rest
        // of its base64 on the screen: the packet is refused whole and the sequence ends at its
        // ST, leaving the write to its closing packet, which reports EINVAL (nothing arrived).
        withTerminalSized(5, 3);
        String mime = "dGV4dC9wbGFpbg=="; // base64("text/plain")
        enterString("\033]5522;type=write:mime=" + mime + ";\033\\");
        mOutput.getOutputAndClear();
        int overCap = KittyClipboard.MAX_PACKET_CHARS + 512;
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < overCap / 4; i++) chunk.append("QUJD");
        enterString("\033]5522;type=wdata:mime=" + mime + ";" + chunk + "\033\\");
        assertLinesAre("     ", "     ", "     "); // no base64 leaked to the screen
        enterString("\033]5522;type=wdata;\033\\"); // The write's closing packet.
        assertEquals("\033]5522;type=write:status=EINVAL\033\\", mOutput.getOutputAndClear());
        assertEquals(0, mOutput.clipboardPuts.size());
    }

    public void testOsc5522PasteEventRoundTripsBase64() {
        // KittyClipboard base64-encodes on the way out too. java.util.Base64 here was an
        // API 26+ class behind a minSdk 21 build: the swap to android.util.Base64 must not
        // change a byte of what the program receives.
        withTerminalSized(5, 3);
        mTerminal.sendPasteEvent(java.util.Arrays.asList("text/plain"));
        String out = mOutput.getOutputAndClear();
        assertTrue(out.contains("type=read:status=OK:pw="));
        assertTrue(out.contains("mime=" + "Lg==" + ":pw=")); // base64("."), the listing type
        assertTrue(out.contains(";" + "dGV4dC9wbGFpbg==" + "\033\\")); // base64("text/plain")
        assertTrue(out.contains("type=read:status=DONE:pw="));
    }

    public void testPastingWithDecset5522SendsThePasteEventInsteadOfTheText() {
        // DECSET 5522 is kitty's paste-events mode: the user's paste is consent, so paste()
        // tells the program with a paste-event packet (a password and the offered types) and
        // writes nothing to the screen; the program fetches the text with the password.
        withTerminalSized(5, 3);
        enterString("\033[?5522h");
        mTerminal.paste("hi");
        String out = mOutput.getOutputAndClear();
        assertTrue(out.contains("type=read:status=OK:pw="));
        assertTrue(out.contains("status=DATA"));
        assertTrue(out.contains("type=read:status=DONE:pw="));
        assertLinesAre("     ", "     ", "     "); // the raw text did not land in the buffer
        // Mode off: insertion as before.
        enterString("\033[?5522l");
        mTerminal.paste("hi");
        assertEquals("hi", mOutput.getOutputAndClear());
    }
}
