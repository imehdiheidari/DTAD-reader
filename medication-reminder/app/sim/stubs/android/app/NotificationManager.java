package android.app; import java.util.*;
public class NotificationManager { public static final int IMPORTANCE_HIGH = 4; public final Map<Integer,Notification> shown = new HashMap<>();
  public void cancel(int id) { shown.remove(id); } public void notify(int id, Notification n) { shown.put(id, n); }
  public boolean areNotificationsEnabled() { return true; } public void deleteNotificationChannel(String id) {} public void createNotificationChannel(NotificationChannel c) {} }
