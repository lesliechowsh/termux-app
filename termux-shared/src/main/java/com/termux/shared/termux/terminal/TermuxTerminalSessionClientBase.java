package com.termux.shared.termux.terminal;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;
import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.notification.TermuxNotificationUtils;
import com.termux.terminal.KittyNotification;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

public class TermuxTerminalSessionClientBase implements TerminalSessionClient {

    /** The application context used to post program notifications; set once by {@code TermuxService#onCreate}. */
    private static Context sAppContext;

    public TermuxTerminalSessionClientBase() {
    }

    /**
     * Register the application context that program notifications are posted with. Called when the
     * app service is created, so that it is set before any session can exist to send an escape.
     */
    public static void setAppContext(@NonNull Context context) {
        sAppContext = context.getApplicationContext();
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession updatedSession) {
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
    }

    @Override
    public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession changedSession) {
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession session, int pid) {
    }


    @Override
    public void onNotification(@NonNull TerminalSession session, String title, String body) {
        if (title == null || title.isEmpty())
            title = TermuxConstants.TERMUX_APP_NAME;
        postProgramNotification(session, title, body, getProgramNotificationId(session, null));
    }

    @Override
    public void onKittyNotification(@NonNull TerminalSession session, @NonNull KittyNotification notification) {
        if (notification.isEmpty()) return;

        String title = notification.getTitle();
        if (title.isEmpty())
            title = TermuxConstants.TERMUX_APP_NAME;
        postProgramNotification(session, title, notification.getBody(),
            getProgramNotificationId(session, notification.getId()));
    }

    @Override
    public void onKittyNotificationClose(@NonNull TerminalSession session, @NonNull String id) {
        Context context = sAppContext;
        if (context == null) return;

        NotificationManager notificationManager = NotificationUtils.getNotificationManager(context);
        if (notificationManager != null)
            notificationManager.cancel(getProgramNotificationId(session, id));
    }

    @Override
    public Integer getTerminalCursorStyle() {
        return null;
    }



    @Override
    public void logError(String tag, String message) {
        Logger.logError(tag, message);
    }

    @Override
    public void logWarn(String tag, String message) {
        Logger.logWarn(tag, message);
    }

    @Override
    public void logInfo(String tag, String message) {
        Logger.logInfo(tag, message);
    }

    @Override
    public void logDebug(String tag, String message) {
        Logger.logDebug(tag, message);
    }

    @Override
    public void logVerbose(String tag, String message) {
        Logger.logVerbose(tag, message);
    }

    @Override
    public void logStackTraceWithMessage(String tag, String message, Exception e) {
        Logger.logStackTraceWithMessage(tag, message, e);
    }

    @Override
    public void logStackTrace(String tag, Exception e) {
        Logger.logStackTrace(tag, e);
    }



    /** Post a program notification on the program notifications channel, dismissed on click. */
    private static void postProgramNotification(@NonNull TerminalSession session, @NonNull String title,
                                                @NonNull String body, int notificationId) {
        Context context = sAppContext;
        if (context == null) return;

        // Open the app launcher activity when the notification is clicked
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent contentIntent = null;
        if (launchIntent != null)
            contentIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);

        Notification.Builder builder = TermuxNotificationUtils.getTermuxOrPluginAppNotificationBuilder(context,
            context, TermuxConstants.TERMUX_APP_PROGRAM_NOTIFICATION_CHANNEL_ID, Notification.PRIORITY_DEFAULT,
            title, body, null, contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null) return;

        NotificationManager notificationManager = NotificationUtils.getNotificationManager(context);
        if (notificationManager != null)
            notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Derive a stable notification id for a program notification, from the session and the id the
     * program named, so that a later {@code OSC 99} with the same id replaces the notification and
     * an {@code OSC 99} close takes down the one that was posted. Notifications without a program
     * id, like {@code OSC 9} and {@code OSC 777}, share the session's id, so the next one replaces
     * the last.
     */
    private static int getProgramNotificationId(@NonNull TerminalSession session, @Nullable String programId) {
        return (31 * session.hashCode() + (programId == null ? 0 : programId.hashCode())) & 0x7fffffff;
    }

}
