package com.termux.terminal;

import java.nio.charset.StandardCharsets;

/** A client which receives callbacks from events triggered by feeding input to a {@link TerminalEmulator}. */
public abstract class TerminalOutput {

    /** Write a string using the UTF-8 encoding to the terminal client. */
    public final void write(String data) {
        if (data == null) return;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        write(bytes, 0, bytes.length);
    }

    /** Write bytes to the terminal client. */
    public abstract void write(byte[] data, int offset, int count);

    /** Notify the terminal client that the terminal title has changed. */
    public abstract void titleChanged(String oldTitle, String newTitle);

    /** Notify the terminal client that text should be copied to clipboard. */
    public abstract void onCopyTextToClipboard(String text);

    /** Notify the terminal client that text should be pasted from clipboard. */
    public abstract void onPasteTextFromClipboard();

    /** Notify the terminal client that a bell character (ASCII 7, bell, BEL, \a, ^G)) has been received. */
    public abstract void onBell();

    public abstract void onColorsChanged();


    /**
     * A kitty graphics animation flipped a stored frame. Call only when something changed a
     * cell or byte of the screen buffer changing. The default has no client to tell.
     */
    public void onScreenChanged() {
    }

    /** Return work produced off-thread to the terminal's serialized update thread. */
    public void postTerminalUpdate(Runnable update) {
        update.run();
    }

    /** Run work on the terminal's serialized update thread after a delay. Default drops it. */
    public void postTerminalUpdateDelayed(Runnable update, long delayMillis) {
    }

    /** Withdraw a pending {@link #postTerminalUpdateDelayed} of exactly this runnable. */
    public void cancelTerminalUpdateDelayed(Runnable update) {
    }
}
