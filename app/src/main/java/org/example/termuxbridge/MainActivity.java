package org.example.termuxbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private String sharedText = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) sharedText = "";
        render();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) sharedText = "";
        render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 40, 32, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Termux Bridge — safe test");
        title.setTextSize(22);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView intro = new TextView(this);
        intro.setPadding(0, 24, 0, 20);
        intro.setText("Shared text is treated as data only. It is never executed. This test runs one fixed diagnostic command after you tap the button.");
        root.addView(intro, new LinearLayout.LayoutParams(-1, -2));

        TextView payload = new TextView(this);
        payload.setText("Received text: " + (sharedText.isEmpty() ? "(none)" : sharedText));
        payload.setTextIsSelectable(true);
        root.addView(payload, new LinearLayout.LayoutParams(-1, -2));

        Button copy = new Button(this);
        copy.setText("Copy received text");
        copy.setEnabled(!sharedText.isEmpty());
        copy.setOnClickListener(v -> { android.content.ClipboardManager c = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE); c.setPrimaryClip(android.content.ClipData.newPlainText("Received text", sharedText)); status.setText("Received text copied to clipboard."); });
        root.addView(copy, new LinearLayout.LayoutParams(-1, -2));

        Button run = new Button(this);
        run.setText("Run fixed Termux diagnostic");
        run.setOnClickListener(v -> {
            run.setEnabled(false);
            status.setText("Request sent to Termux. Waiting for result…");
            CommandResultReceiver.execute(this, result -> run.post(() -> {
                status.setText(result);
                run.setEnabled(true);
            }));
        });
        root.addView(run, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setPadding(0, 24, 0, 0);
        status.setText("No command run yet.");
        status.setTextIsSelectable(true);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }
}
