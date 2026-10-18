package org.example.termuxbridge;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.util.function.Consumer;

public class CommandResultReceiver extends BroadcastReceiver {
    private static Consumer<String> currentCallback;

    static void execute(Context context, Consumer<String> callback) {
        currentCallback = callback;
        Intent resultIntent = new Intent(context, CommandResultReceiver.class);
        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                context, (int)(System.currentTimeMillis() & 0x7fffffff),
                resultIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE);

        Intent command = new Intent();
        command.setClassName("com.termux", "com.termux.app.RunCommandService");
        command.setAction("com.termux.RUN_COMMAND");
        command.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh");
        command.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        command.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
                new String[]{"-c", "printf 'TERMUX_BRIDGE_OK\\n'; printf 'user='; id -un; printf 'time='; date -Iseconds"});
        command.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        command.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", resultPendingIntent);
        command.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "Bridge diagnostic");
        command.putExtra("com.termux.RUN_COMMAND_COMMAND_DESCRIPTION", "Runs a fixed read-only diagnostic.");
        try {
            context.startService(command);
        } catch (Exception e) {
            if (callback != null) callback.accept("Could not start Termux command service: " + e);
        }
    }

    @Override public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            if (currentCallback != null) currentCallback.accept("Termux returned no result bundle.");
            return;
        }
        Bundle result = extras.getBundle("com.termux.app.notify.plugin_result_bundle");
        if (result == null) {
            if (currentCallback != null) currentCallback.accept("Termux callback did not contain the documented result bundle.");
            return;
        }
        String stdout = result.getString("stdout", "");
        String stderr = result.getString("stderr", "");
        int exit = result.getInt("exitCode", -999);
        int errCode = result.getInt("err", 0);
        String err = result.getString("errmsg", "");
        String text = "Exit code: " + exit + "\nService error code: " + errCode +
                "\n\nSTDOUT:\n" + stdout +
                "\n\nSTDERR:\n" + stderr + (err.isEmpty() ? "" : "\n\nService error:\n" + err);
        if (currentCallback != null) currentCallback.accept(text);
    }
}
