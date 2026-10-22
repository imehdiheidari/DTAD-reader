package android.content; import java.util.*;
public class Context { public static final int MODE_PRIVATE = 0; public static final String NOTIFICATION_SERVICE = "n", ALARM_SERVICE = "a";
  final Map<String,SharedPreferences> prefs = new HashMap<>();
  public final android.app.NotificationManager nm = new android.app.NotificationManager(); public final android.app.AlarmManager am = new android.app.AlarmManager();
  public SharedPreferences getSharedPreferences(String n, int mode) { return prefs.computeIfAbsent(n, x -> new SharedPreferences()); }
  public Object getSystemService(String s) { return s.equals("n") ? nm : am; } }
