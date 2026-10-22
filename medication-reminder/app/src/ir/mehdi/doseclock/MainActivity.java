package ir.mehdi.doseclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    static final int PICK_PHOTO = 7, PHOTO_PX = 320;
    private String photoReq, photoSource;

    static final class Bridge {
        private final MainActivity a;
        private final Context c;
        Bridge(MainActivity a) { this.a = a; this.c = a.getApplicationContext(); }
        @JavascriptInterface public void pickPhoto(String req, String source) { a.pickPhotoLater(req, source); }
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
        web.addJavascriptInterface(new Bridge(this), "DoseNative");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        Alarms.createChannel(this);
        Alarms.log(this, "app opened");
        Alarms.reschedule(this);
        askNotificationPermission();
    }

    void pickPhotoLater(final String req, final String source) { runOnUiThread(() -> pickPhoto(req, source)); }

    // Camera or gallery; the answer goes back to the page through window.__photoPicked(req, dataUrl).
    private void pickPhoto(String req, String source) {
        if (photoReq != null) answerPhoto(photoReq, null);
        photoReq = req.replaceAll("[^A-Za-z0-9]", "");
        photoSource = source;
        Intent i;
        if ("camera".equals(source)) {
            PhotoProvider.cameraFile(this).delete();
            i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, PhotoProvider.CAMERA_URI)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setClipData(ClipData.newRawUri("", PhotoProvider.CAMERA_URI));
        } else {
            i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        }
        try { startActivityForResult(i, PICK_PHOTO); }
        catch (Exception e) { Alarms.log(this, "photo: no " + source + " app (" + e + ")"); answerPhoto(photoReq, null); photoReq = null; }
    }

    @Override
    protected void onActivityResult(int code, int result, final Intent data) {
        super.onActivityResult(code, result, data);
        if (code != PICK_PHOTO || photoReq == null) return;
        final String req = photoReq;
        final boolean camera = "camera".equals(photoSource);
        photoReq = null;
        if (result != RESULT_OK) { answerPhoto(req, null); return; }
        new Thread(() -> {
            String url = null;
            try {
                Uri uri = camera ? Uri.fromFile(PhotoProvider.cameraFile(this)) : data != null ? data.getData() : null;
                if (uri != null) url = squarePhoto(uri);
            } catch (Throwable e) { Alarms.log(this, "photo failed: " + e); }
            final String answer = url;
            runOnUiThread(() -> answerPhoto(req, answer));
        }).start();
    }

    private InputStream open(Uri uri) throws java.io.IOException {
        return "file".equals(uri.getScheme()) ? new java.io.FileInputStream(uri.getPath()) : getContentResolver().openInputStream(uri);
    }

    /** Centre square of the photo, turned upright, 320 x 320 JPEG as a data URL. */
    private String squarePhoto(Uri uri) throws java.io.IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = open(uri)) { BitmapFactory.decodeStream(in, null, o); }
        int side = Math.min(o.outWidth, o.outHeight), sample = 1;
        while (side / (sample * 2) >= PHOTO_PX) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        Bitmap bm;
        try (InputStream in = open(uri)) { bm = BitmapFactory.decodeStream(in, null, o); }
        if (bm == null) return null;
        int rotate = 0;
        try (InputStream in = open(uri)) {
            int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            rotate = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90 : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180 : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
        } catch (Exception ignored) {}
        int s = Math.min(bm.getWidth(), bm.getHeight());
        Matrix m = new Matrix();
        m.postScale(PHOTO_PX / (float) s, PHOTO_PX / (float) s);
        m.postRotate(rotate);
        Bitmap sq = Bitmap.createBitmap(bm, (bm.getWidth() - s) / 2, (bm.getHeight() - s) / 2, s, s, m, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sq.compress(Bitmap.CompressFormat.JPEG, 82, out);
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    private void answerPhoto(String req, String dataUrl) {
        if (web == null || req == null) return;
        web.evaluateJavascript("window.__photoPicked && window.__photoPicked('" + req + "', " + (dataUrl == null ? "null" : "'" + dataUrl + "'") + ")", null);
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
