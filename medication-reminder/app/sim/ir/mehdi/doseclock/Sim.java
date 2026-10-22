package ir.mehdi.doseclock;
import android.app.*; import android.content.*; import java.util.*; import org.json.*;
public class Sim {
  static Context c = new Context();
  static String hhmm(long ms) { Calendar k = Calendar.getInstance(); k.setTimeInMillis(ms); return String.format(Locale.US, "%02d:%02d", k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE)); }
  static void alarms() { for (Map.Entry<String,Object[]> e : c.am.alarms.entrySet()) System.out.println("   alarm " + e.getKey() + " at " + new Date((Long) e.getValue()[0])); }
  static void shown() { for (Map.Entry<Integer,Notification> e : c.nm.shown.entrySet()) System.out.println("   NOTIF " + e.getValue().title + " | " + e.getValue().text + " | " + e.getValue().sub + " | " + e.getValue().actions); if (c.nm.shown.isEmpty()) System.out.println("   (no notification)"); }
  static void press(int which) { Notification n = c.nm.shown.values().iterator().next(); Intent i = n.actionPis.get(which).intent;
    String a = i.action; String id = (String) i.extras.get("id");
    if (a.equals(Alarms.ACTION_DONE)) Alarms.done(c, id, (String) i.extras.get("key")); else Alarms.later(c, id, (String) i.extras.get("key"), (Integer) i.extras.get("count")); }
  public static void main(String[] x) throws Exception {
    long now = System.currentTimeMillis();
    String lang = x.length > 0 ? x[0] : "fa";
    String snz = x.length > 1 ? ",\"snoozeMin\":" + x[1] : "";
    String st = "{\"lang\":\"" + lang + "\"" + snz + ",\"doses\":[{\"id\":\"am\",\"time\":\"" + hhmm(now - 60000) + "\",\"meds\":[{\"id\":\"m\",\"name\":\"Prednisolone\",\"qty\":1,\"unit\":\"tablet\"}]},{\"id\":\"pm\",\"time\":\"" + hhmm(now + 3 * 60000) + "\",\"meds\":[]}],\"log\":{},\"snoozes\":{}}";
    System.out.println("1) page saves state (dose am due 1 min ago, pm in 3 min)");
    Alarms.saveState(c, st); Alarms.reschedule(c); alarms();
    System.out.println("2) am alarm fires"); Alarms.fire(c, "am"); shown(); alarms();
    System.out.println("3) page saves again while it shows (must not re-alarm)"); Alarms.reschedule(c); shown(); alarms();
    System.out.println("4) press Later"); press(1); shown(); alarms();
    System.out.println("5) stale fire before snooze ends"); Alarms.fire(c, "am"); shown();
    System.out.println("6) 30 min pass: move snooze to the past, alarm fires");
    JSONObject s = new JSONObject(Alarms.prefs(c).getString("state", null)); String key = Alarms.dayKey(now) + "|am";
    s.getJSONObject("snoozes").getJSONObject(key).put("nextAt", now - 1000); Alarms.saveState(c, s.toString()); Alarms.fire(c, "am"); shown();
    System.out.println("7) press Done"); press(0); shown(); alarms();
    System.out.println("8) pm is reached: fire pm early (should say not due)"); Alarms.fire(c, "pm"); shown();
    System.out.println("--- log ---"); System.out.println(Alarms.prefs(c).getString("log", ""));
  }
}
