package com.example.autoswipe;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sp = getSharedPreferences(SwipeService.PREFS, MODE_PRIVATE);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, pad);
        root.addView(status);

        Button open = new Button(this);
        open.setText("Открыть настройки спец. возможностей");
        open.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(open);

        Switch enabled = new Switch(this);
        enabled.setText("Автосвайп включён");
        enabled.setPadding(0, pad, 0, pad);
        enabled.setChecked(sp.getBoolean(SwipeService.KEY_ENABLED, false));
        enabled.setOnCheckedChangeListener((v, on) ->
                sp.edit().putBoolean(SwipeService.KEY_ENABLED, on).apply());
        root.addView(enabled);

        Switch detect = new Switch(this);
        detect.setText("Пытаться определить конец по полосе прогресса");
        detect.setPadding(0, 0, 0, pad);
        detect.setChecked(sp.getBoolean(SwipeService.KEY_DETECT, true));
        detect.setOnCheckedChangeListener((v, on) ->
                sp.edit().putBoolean(SwipeService.KEY_DETECT, on).apply());
        root.addView(detect);

        TextView label = new TextView(this);
        label.setText("Если конец определить не удалось, свайпать через (секунд):");
        root.addView(label);

        EditText delay = new EditText(this);
        delay.setInputType(InputType.TYPE_CLASS_NUMBER);
        delay.setText(String.valueOf(sp.getInt(SwipeService.KEY_DELAY, 15)));
        delay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                try {
                    int v = Integer.parseInt(s.toString().trim());
                    v = Math.max(3, Math.min(600, v));
                    sp.edit().putInt(SwipeService.KEY_DELAY, v).apply();
                } catch (NumberFormatException ignored) {
                }
            }
        });
        root.addView(delay);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status.setText(serviceOn()
                ? "Служба включена ✔"
                : "Служба выключена. Нажмите кнопку ниже и включите «TikTok АвтоСвайп» в списке служб.");
    }

    private boolean serviceOn() {
        String s = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getPackageName() + "/");
    }
}
