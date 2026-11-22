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
}
