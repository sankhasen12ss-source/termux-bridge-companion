package org.example.termuxbridge;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class CommandResultReceiver extends BroadcastReceiver {
    private static final ConcurrentMap<String, Consumer<String>> CALLBACKS =
            new ConcurrentHashMap<>();

    private static final java.util.concurrent.atomic.AtomicInteger NEXT_REQUEST_CODE =
            new java.util.concurrent.atomic.AtomicInteger(1);

    static void execute(Context context, Consumer<String> callback) {
        String requestId = UUID.randomUUID().toString();
        int requestCode = NEXT_REQUEST_CODE.getAndIncrement();

        if (callback != null) {
            CALLBACKS.put(requestId, callback);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Consumer<String> expired = CALLBACKS.remove(requestId);
                if (expired != null) {
                    expired.accept("Timed out waiting 60 seconds for Termux. "
                            + "Check that Termux is installed and allowed to run commands.");
                }
            }, 60_000L);
        }

        Intent resultIntent = new Intent(context, CommandResultReceiver.class);
        resultIntent.setAction(context.getPackageName() + ".TERMUX_RESULT." + requestId);
        resultIntent.putExtra("requestId", requestId);

        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                context, requestCode, resultIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE);

        Intent command = new Intent();
        command.setClassName("com.termux", "com.termux.app.RunCommandService");
        command.setAction("com.termux.RUN_COMMAND");
        command.putExtra("com.termux.RUN_COMMAND_PATH",
                "/data/data/com.termux/files/usr/bin/sh");
        command.putExtra("com.termux.RUN_COMMAND_WORKDIR",
                "/data/data/com.termux/files/home");
        command.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
                new String[]{"-c",
                        "printf 'TERMUX_BRIDGE_OK\\n'; printf 'user='; id -un; printf 'time='; date -Iseconds"});
        command.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        command.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", resultPendingIntent);
        command.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "Bridge diagnostic");
        command.putExtra("com.termux.RUN_COMMAND_COMMAND_DESCRIPTION",
                "Runs a fixed read-only diagnostic.");

        try {
            context.startService(command);
        } catch (Exception e) {
            Consumer<String> saved = CALLBACKS.remove(requestId);
            if (saved != null) {
                saved.accept("Could not start Termux command service: " + e);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String requestId = intent.getStringExtra("requestId");
        Consumer<String> callback =
                requestId == null ? null : CALLBACKS.remove(requestId);
        if (callback == null) return;

        Bundle extras = intent.getExtras();
        if (extras == null) {
            callback.accept("Termux returned no result bundle.");
            return;
        }

        Bundle result =
                extras.getBundle("com.termux.app.notify.plugin_result_bundle");

        if (!looksLikeResult(result)) {
            result = extras.getBundle("result");
        }

        if (!looksLikeResult(result)) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                if (value instanceof Bundle && looksLikeResult((Bundle) value)) {
                    result = (Bundle) value;
                    break;
                }
            }
        }

        if (!looksLikeResult(result)) {
            callback.accept("Termux callback received, but no recognizable result bundle "
                    + "was found. Extra keys: " + extras.keySet());
            return;
        }

        String stdout = result.getString("stdout", "");
        String stderr = result.getString("stderr", "");
        int exit = result.getInt("exitCode", -999);
        int errCode = result.getInt("err", 0);
        String err = result.getString("errmsg", "");

        String text = "Exit code: " + exit
                + "\nService error code: " + errCode
                + "\n\nSTDOUT:\n" + stdout
                + "\n\nSTDERR:\n" + stderr
                + (err.isEmpty() ? "" : "\n\nService error:\n" + err);

        callback.accept(text);
    }

    private static boolean looksLikeResult(Bundle bundle) {
        return bundle != null && (bundle.containsKey("stdout")
                || bundle.containsKey("stderr")
                || bundle.containsKey("exitCode")
                || bundle.containsKey("err")
                || bundle.containsKey("errmsg"));
    }
}
