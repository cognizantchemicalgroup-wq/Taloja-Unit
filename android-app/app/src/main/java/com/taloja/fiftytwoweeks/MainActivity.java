package com.taloja.fiftytwoweeks;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Hosts the tracker page (assets/index.html) in a full-screen WebView.
 * All workout data is kept in the WebView's localStorage on the phone.
 */
public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);     // localStorage = where the log lives
        s.setAllowFileAccess(true);       // needed to load the bundled page
        s.setTextZoom(100);

        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep the app on its own page; open anything else outside.
                String url = request.getUrl().toString();
                if (url.startsWith("file:///android_asset/")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl())); } catch (Exception ignored) { }
                return true;
            }
        });

        // Ask once for notification permission (Android 13+) and book the daily reminder.
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && Store.prefs(this).getBoolean("remind", true)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        ReminderReceiver.schedule(this);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    /** Colours the status and navigation bars to match the page's Day/Night theme. */
    private void applyBars(boolean dark) {
        Window w = getWindow();
        int color = dark ? 0xFF0E1117 : 0xFFF3F5F8;
        w.setStatusBarColor(color);
        w.setNavigationBarColor(color);
        webView.setBackgroundColor(color);
        View decor = w.getDecorView();
        int flags = decor.getSystemUiVisibility();
        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) light |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        flags = dark ? (flags & ~light) : (flags | light);
        decor.setSystemUiVisibility(flags);
    }

    /** Called from the page as window.Android.shareText(...) / setBars(...). */
    private class Bridge {
        /** The page sends its logged dates; the widget and reminder read them. */
        @JavascriptInterface
        public void syncState(String json) {
            Store.saveState(MainActivity.this, json);
            WeekWidget.updateAll(MainActivity.this);
        }

        @JavascriptInterface
        public String getReminder() {
            SharedPreferences p = Store.prefs(MainActivity.this);
            boolean allowed = ReminderReceiver.canNotify(MainActivity.this);
            return "{\"on\":" + p.getBoolean("remind", true)
                    + ",\"hour\":" + p.getInt("hour", 20)
                    + ",\"minute\":" + p.getInt("minute", 0)
                    + ",\"allowed\":" + allowed + "}";
        }

        @JavascriptInterface
        public void setReminder(boolean on, int hour, int minute) {
            Store.prefs(MainActivity.this).edit()
                    .putBoolean("remind", on).putInt("hour", hour).putInt("minute", minute).apply();
            ReminderReceiver.schedule(MainActivity.this);
            if (on && Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7));
            }
        }

        /** Shows the reminder right now so you can see how it looks. Returns false if notifications are blocked. */
        @JavascriptInterface
        public boolean testReminder() {
            if (!ReminderReceiver.canNotify(MainActivity.this)) {
                if (Build.VERSION.SDK_INT >= 33) {
                    runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7));
                }
                return false;
            }
            ReminderReceiver.show(MainActivity.this, Store.snapshot(MainActivity.this), true);
            return true;
        }

        @JavascriptInterface
        public void setBars(boolean dark) {
            runOnUiThread(() -> applyBars(dark));
        }

        @JavascriptInterface
        public void shareText(String text) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "52 Weeks backup");
            send.putExtra(Intent.EXTRA_TEXT, text);
            runOnUiThread(() -> startActivity(Intent.createChooser(send, "Save backup to…")));
        }
    }
}
