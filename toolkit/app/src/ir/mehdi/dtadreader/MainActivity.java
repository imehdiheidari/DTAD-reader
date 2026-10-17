package ir.mehdi.dtadreader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("content", MODE_PRIVATE);
        // white status bar with dark icons, to match the page
        getWindow().setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        setContentView(web);
        if (b != null) web.restoreState(b); else load();
        checkForUpdate();
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    private File downloaded() { return new File(getFilesDir(), "content.html"); }

    private int bundledVersion() {
        try { return new JSONObject(readStream(getAssets().open("version.json"))).getInt("version"); }
        catch (Exception e) { return 0; }
    }

    // The newest copy wins: a downloaded update, or the content shipped inside the APK.
    private void load() {
        File f = downloaded();
        if (f.exists() && prefs.getInt("version", 0) > bundledVersion()) web.loadUrl("file://" + f.getAbsolutePath());
        else web.loadUrl("file:///android_asset/content.html");
    }

    private void checkForUpdate() {
        new Thread(() -> {
            try {
                String base = new JSONObject(readStream(getAssets().open("config.json"))).optString("updateBase", "");
                if (base.isEmpty()) return;
                long t = System.currentTimeMillis();
                JSONObject remote = new JSONObject(fetch(base + "version.json?t=" + t));
                int v = remote.getInt("version");
                int have = Math.max(prefs.getInt("version", 0), bundledVersion());
                if (v <= have) return;
                File tmp = new File(getFilesDir(), "content.tmp");
                download(base + remote.optString("file", "content.html") + "?t=" + t, tmp);
                // GitHub's CDN can serve an older content.html for a few minutes after a push: keep only the announced version
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("<meta name=\"dtad-version\" content=\"(\\d+)\"")
                        .matcher(readStream(new FileInputStream(tmp)));
                if (!m.find() || Integer.parseInt(m.group(1)) < v) { tmp.delete(); return; }
                if (!tmp.renameTo(downloaded())) return;
                prefs.edit().putInt("version", v).apply();
                String msg = remote.optString("message", "محتوای جدید اضافه شد");
                runOnUiThread(() -> { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); load(); });
            } catch (Exception ignored) {
                // offline or host unreachable: keep showing what we have
            }
        }).start();
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(60000);
        c.setUseCaches(false);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        return c;
    }

    private static String fetch(String url) throws IOException { return readStream(open(url).getInputStream()); }

    private static void download(String url, File to) throws IOException {
        try (InputStream in = open(url).getInputStream(); OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static String readStream(InputStream in) throws IOException {
        try (InputStream i = in) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = i.read(buf)) > 0) o.write(buf, 0, n);
            return o.toString("UTF-8");
        }
    }
}
