package android.app; import android.content.*;
public class PendingIntent { public static final int FLAG_UPDATE_CURRENT = 1, FLAG_IMMUTABLE = 2; public final Intent intent; public final int code;
  PendingIntent(Intent i, int c) { intent = i; code = c; }
  public static PendingIntent getBroadcast(Context c, int code, Intent i, int f) { return new PendingIntent(i, code); }
  public static PendingIntent getActivity(Context c, int code, Intent i, int f) { return new PendingIntent(i, code); }
  public String key() { return intent.cls + "/" + intent.action + "/" + code; } }
