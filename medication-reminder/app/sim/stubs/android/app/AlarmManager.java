package android.app; import java.util.*;
public class AlarmManager { public static final int RTC_WAKEUP = 0; public final Map<String,Object[]> alarms = new TreeMap<>();
  public boolean canScheduleExactAlarms() { return true; }
  public void setAndAllowWhileIdle(int t, long at, PendingIntent pi) { alarms.put(pi.key(), new Object[]{at, pi}); }
  public void setAlarmClock(AlarmClockInfo info, PendingIntent pi) { alarms.put(pi.key(), new Object[]{info.at, pi}); }
  public void cancel(PendingIntent pi) { alarms.remove(pi.key()); }
  public static class AlarmClockInfo { final long at; public AlarmClockInfo(long at, PendingIntent show) { this.at = at; } } }
