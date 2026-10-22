package android.content; import java.util.*;
public class Intent { public static final int FLAG_ACTIVITY_NEW_TASK = 1, FLAG_ACTIVITY_CLEAR_TOP = 2; public String action; public final Map<String,Object> extras = new HashMap<>(); public final String cls;
  public Intent(Context c, Class<?> k) { cls = k.getSimpleName(); }
  public Intent setAction(String a) { action = a; return this; } public Intent setFlags(int f) { return this; }
  public Intent putExtra(String k, String v) { extras.put(k, v); return this; } public Intent putExtra(String k, int v) { extras.put(k, v); return this; } public Intent putExtra(String k, boolean v) { extras.put(k, v); return this; } }
