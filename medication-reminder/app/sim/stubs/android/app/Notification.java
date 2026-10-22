package android.app; import java.util.*;
public class Notification { public static final int VISIBILITY_PUBLIC = 1, PRIORITY_MAX = 2, DEFAULT_ALL = -1; public static final String CATEGORY_ALARM = "alarm";
  public String title, text, sub; public List<String> actions = new ArrayList<>(); public List<PendingIntent> actionPis = new ArrayList<>(); public PendingIntent delete; public android.graphics.Bitmap large;
  public abstract static class Style {}
  public static class BigTextStyle extends Style { public BigTextStyle bigText(CharSequence t) { return this; } }
  public static class Builder { Notification n = new Notification();
    public Builder(android.content.Context c, String ch) {} public Builder(android.content.Context c) {}
    public Builder setPriority(int p) { return this; } public Builder setDefaults(int d) { return this; } public Builder setSmallIcon(int i) { return this; } public Builder setColor(int c) { return this; }
    public Builder setContentTitle(CharSequence t) { n.title = t.toString(); return this; } public Builder setContentText(CharSequence t) { n.text = t.toString(); return this; }
    public Builder setStyle(Style s) { return this; } public Builder setSubText(CharSequence t) { n.sub = t.toString(); return this; } public Builder setCategory(String c) { return this; }
    public Builder setVisibility(int v) { return this; } public Builder setOngoing(boolean b) { return this; } public Builder setAutoCancel(boolean b) { return this; } public Builder setShowWhen(boolean b) { return this; }
    public Builder setContentIntent(PendingIntent p) { return this; } public Builder setFullScreenIntent(PendingIntent p, boolean h) { return this; } public Builder setDeleteIntent(PendingIntent p) { n.delete = p; return this; }
    public Builder addAction(int i, CharSequence t, PendingIntent p) { n.actions.add(t.toString()); n.actionPis.add(p); return this; } public Builder setLargeIcon(android.graphics.Bitmap b) { n.large = b; return this; } public Notification build() { return n; } } }
