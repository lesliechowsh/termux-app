package com.termux.terminal;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The interface for communication between {@link TerminalSession} and its client. It is used to
 * send callbacks to the client when {@link TerminalSession} changes or for sending other
 * back data to the client like logs.
 */
public interface TerminalSessionClient {

    void onTextChanged(@NonNull TerminalSession changedSession);

    void onTitleChanged(@NonNull TerminalSession changedSession);

    void onSessionFinished(@NonNull TerminalSession finishedSession);

    void onCopyTextToClipboard(@NonNull TerminalSession session, String text);

    /**
     * Answer an OSC 52 clipboard read query. The default has nothing to offer, so it answers as
     * if the clipboard were empty rather than leaving the query unanswered.
     */
    default String onReadTextFromClipboard(@NonNull TerminalSession session) {
        return null;
    }

    /**
     * Answer an OSC 5522 clipboard read: call {@code callback} exactly once with what the
     * clipboard holds, or null when the read is refused.
     */
    default void onReadClipboard(@NonNull TerminalSession session, boolean pasted,
                                 @NonNull KittyClipboard.ReadCallback callback) {
        String text = onReadTextFromClipboard(session);
        callback.onOffer(text == null ? null : ClipboardOffer.ofText(text));
    }

    /**
     * Put the pictures of a finished OSC 5522 write on the clipboard, by MIME type, with the
     * write's text or null. The default has nowhere to put a picture and keeps the text.
     */
    default void onWriteClipboard(@NonNull TerminalSession session, @NonNull Map<String, byte[]> images,
                                  @Nullable String text) {
        if (text != null) onCopyTextToClipboard(session, text);
    }

    /**
     * The program asked for the user with a message (a terminal notification escape). Like a bell
     * with words: the default ignores it.
     */
    default void onNotification(@NonNull TerminalSession session, String title, String body) {
    }

    /**
     * The program asked for the user with a whole notification request. The default passes the
     * words on to {@link #onNotification}.
     */
    default void onKittyNotification(@NonNull TerminalSession session,
                                     @NonNull KittyNotification notification) {
        onNotification(session, notification.getTitle(), notification.getBody());
    }

    /** The program asked for the notification it named earlier to be taken down. */
    default void onKittyNotificationClose(@NonNull TerminalSession session, @NonNull String id) {
    }

    /**
     * The program asked for a mouse pointer shape, by its CSS/X11 name, or with null for the
     * terminal's own again. The default has no pointer to change.
     */
    default void onPointerShapeChanged(@NonNull TerminalSession session, @Nullable String shape) {
    }

    void onPasteTextFromClipboard(@Nullable TerminalSession session);

    void onBell(@NonNull TerminalSession session);

    void onColorsChanged(@NonNull TerminalSession session);

    void onTerminalCursorStateChange(boolean state);

    void setTerminalShellPid(@NonNull TerminalSession session, int pid);



    Integer getTerminalCursorStyle();



    void logError(String tag, String message);

    void logWarn(String tag, String message);

    void logInfo(String tag, String message);

    void logDebug(String tag, String message);

    void logVerbose(String tag, String message);

    void logStackTraceWithMessage(String tag, String message, Exception e);

    void logStackTrace(String tag, Exception e);

}
