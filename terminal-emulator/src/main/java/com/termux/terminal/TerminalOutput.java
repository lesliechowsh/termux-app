package com.termux.terminal;

import java.util.Map;

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
    public abstract void titleChanged(String oldTitle, String newTitle);

    /** Notify the terminal client that text should be copied to clipboard. */
    public abstract void onCopyTextToClipboard(String text);

    /** Notify the terminal client that text should be pasted from clipboard. */
    public abstract void onPasteTextFromClipboard();

    /** Write bytes to the terminal client. */
    public abstract void write(byte[] data, int offset, int count);

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

    /**
     * Ask the terminal client for the text on the clipboard, for an OSC 52 query
     * ({@code ESC ] 52 ; c ; ? BEL}). The default has no client to ask, so it answers as if the
     * clipboard were empty rather than leaving the query unanswered.
     */
    public String onReadTextFromClipboard() {
        return null;
    }

    /**
     * Read the clipboard for an {@code OSC 5522} read and answer {@code callback} exactly once,
     * with what it holds or null when the read is refused; see {@link KittyClipboard.Handler#read}
     * for {@code pasted} and the threading. The default offers the text
     * {@link #onReadTextFromClipboard()} answers, at once.
     */
    public void onReadClipboard(boolean pasted, KittyClipboard.ReadCallback callback) {
        String text = onReadTextFromClipboard();
        callback.onOffer(text == null ? null : ClipboardOffer.ofText(text));
    }

    /**
     * Put the pictures of a finished {@code OSC 5522} write on the clipboard, by MIME type, with
     * the write's text or null. The default has nowhere to put a picture and keeps the text.
     */
    public void onWriteClipboard(Map<String, byte[]> images, String text) {
        if (text != null) onCopyTextToClipboard(text);
    }

    /**
     * Notify the terminal client that a bell character (ASCII 7, bell, BEL, \a, ^G)) has been received.
     */


    /**
     * Notify the terminal client that the program asked for the user's attention with a message —
     * an iTerm2-style {@code OSC 9 ; body} or an rxvt-style {@code OSC 777 ; notify ; title ; body}.
     * Agents emit these when a turn ends or an approval is needed. The default drops it.
     */
    public void onNotification(String title, String body) {
    }

    /**
     * Notify the terminal client of a whole desktop-notification request — {@code OSC 99} — with
     * everything the program said about it: a name to close or replace it by, how urgent it is,
     * whether it wants to be told the user tapped it.
     *
     * <p>The default passes the words along the older, plainer path, so a client that only knows
     * how to show a title and a body keeps working.
     */
    public void onKittyNotification(KittyNotification notification) {
    }

    /** Take down the notification the program named, if it is still up. */
    public void onKittyNotificationClose(String id) {
    }

    /**
     * The running program asked for a mouse pointer shape — {@code OSC 22} — or, with null, for
     * the terminal's own again. The default has no pointer to change.
     */
    public void onPointerShapeChanged(String shape) {
    }




}
