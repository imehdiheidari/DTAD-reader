package ir.mehdi.doseclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.lang.ref.WeakReference;

public class MainActivity extends Activity {
    static final String EXTRA_REMINDER = "reminder";
    private static WeakReference<MainActivity> open = new WeakReference<>(null);
    private WebView web;

    /** What the page sees as window.DoseNative: it keeps the state in the app so the alarms can use it. */
    static final class Bridge {
        private final Context c;
        Bridge(Context c) { this.c = c; }
        @JavascriptInterface public String load() { return Alarms.prefs(c).getString(Alarms.STATE, ""); }
        @JavascriptInterface public void save(String json) { Alarms.saveState(c, json); Alarms.reschedule(c); }
        @JavascriptInterface public String debugLog() { return Alarms.prefs(c).getString(Alarms.LOG, ""); }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF121212);
        getWindow().setNavigationBarColor(0xFF121212);
        overLockScreen(getIntent());
        web = new WebView(this);
        web.setBackgroundColor(0xFFF4F2ED);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(getApplicationContext()), "DoseNative");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        Alarms.createChannel(this);
        Alarms.log(this, "app opened");
        Alarms.reschedule(this);
        askNotificationPermission();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        overLockScreen(i);
    }

    // Opened by a reminder while the phone is locked: show it over the lock screen and turn the screen on.
    private void overLockScreen(Intent i) {
        boolean on = i != null && i.getBooleanExtra(EXTRA_REMINDER, false);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(on); setTurnScreenOn(on); }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(false); setTurnScreenOn(false); }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) { reload(); }

    @Override
    protected void onResume() {
        super.onResume();
        open = new WeakReference<>(this);
        reload();
    }

    @Override
    protected void onDestroy() {
        if (open.get() == this) open = new WeakReference<>(null);
        super.onDestroy();
    }

    private void reload() {
        if (web != null) web.evaluateJavascript("window.__doseReload && window.__doseReload()", null);
    }

    /** Done / Later on a notification while the app is on screen: show the change at once. */
    static void reloadIfOpen() {
        MainActivity a = open.get();
        if (a != null) a.runOnUiThread(a::reload);
    }
}
