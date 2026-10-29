package com.example.autoswipe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class SwipeService extends AccessibilityService {

    static final String PREFS = "cfg";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_DETECT = "detect";
    static final String KEY_DELAY = "delay";

    private static final long TICK_MS = 400;
    private static final long COOLDOWN_MS = 1500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private long videoStart = 0;
    private long lastSwipe = 0;
    private float lastFraction = -1f;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                check();
            } catch (Exception ignored) {
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        videoStart = System.currentTimeMillis();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.removeCallbacks(tick);
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Любая прокрутка (наша или ручная) = началось новое видео, отсчёт заново.
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            videoStart = System.currentTimeMillis();
            lastFraction = -1f;
        }
    }

    @Override
    public void onInterrupt() {
    }

    private void check() {
        long now = System.currentTimeMillis();

        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            videoStart = now;
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        CharSequence pkg = root.getPackageName();
        boolean tiktok = pkg != null
                && (pkg.equals("com.zhiliaoapp.musically") || pkg.equals("com.ss.android.ugc.trill"));
        if (!tiktok) {
            videoStart = now;
            lastFraction = -1f;
            return;
        }

        if (now - lastSwipe < COOLDOWN_MS) return;

        boolean done = false;
        boolean barFound = false;

        if (prefs.getBoolean(KEY_DETECT, true)) {
            float f = findProgress(root, 0);
            if (f >= 0f) {
                barFound = true;
                if (f >= 0.97f) {
                    done = true;                       // дошли до конца
                } else if (lastFraction > 0.9f && f < 0.1f) {
                    done = true;                       // видео зациклилось
                }
                lastFraction = f;
            } else {
                lastFraction = -1f;
            }
        }

        if (!barFound) {
            long delay = prefs.getInt(KEY_DELAY, 15) * 1000L;
            if (now - videoStart >= delay) done = true; // запасной вариант: таймер
        }

        if (done) swipeUp();
    }

    /** Ищет в дереве элементов ползунок/прогресс и возвращает долю 0..1, либо -1, если не нашёл. */
    private float findProgress(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 25) return -1f;

        AccessibilityNodeInfo.RangeInfo r = node.getRangeInfo();
        if (r != null && r.getMax() > r.getMin()) {
            float f = (r.getCurrent() - r.getMin()) / (r.getMax() - r.getMin());
            return Math.max(0f, Math.min(1f, f));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            float f = findProgress(node.getChild(i), depth + 1);
            if (f >= 0f) return f;
        }
        return -1f;
    }

    private void swipeUp() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float x = dm.widthPixels * 0.5f;
        float y1 = dm.heightPixels * 0.78f;
        float y2 = dm.heightPixels * 0.25f;

        Path path = new Path();
        path.moveTo(x, y1);
        path.lineTo(x, y2);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 250);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);

        long now = System.currentTimeMillis();
        lastSwipe = now;
        videoStart = now;
        lastFraction = -1f;
    }
}
