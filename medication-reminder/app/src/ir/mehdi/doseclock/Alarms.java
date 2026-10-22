package ir.mehdi.doseclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.PowerManager;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The schedule lives here so reminders work while the app is closed. The page saves its whole state
 * (lang, doses, log, snoozes — the same JSON it keeps in localStorage) through the bridge, and this class
 * keeps one exact alarm per dose: the next time that dose is due, or its next snooze reminder.
 * Keys are "YYYY-MM-DD|doseId", as in the page.
 */
final class Alarms {
    static final String PREFS = "dose", STATE = "state", FIRED = "fired", IDS = "ids";
    // A new channel id: channel importance and sound are fixed once created, and the first one had no alarm sound.
    static final String CHANNEL = "dose-alarms-v2", OLD_CHANNEL = "dose-reminders";
    static final String ACTION_TEST = "ir.mehdi.doseclock.TEST", ACTION_TEST_CLOSE = "ir.mehdi.doseclock.TEST_CLOSE";
    static final int TEST_ID = 999;
    static final String ACTION_FIRE = "ir.mehdi.doseclock.FIRE";
    static final String ACTION_DONE = "ir.mehdi.doseclock.DONE";
    static final String ACTION_LATER = "ir.mehdi.doseclock.LATER";
    static final long SNOOZE_MS = 30 * 60 * 1000L;
    static final long DAY_MS = 24 * 60 * 60 * 1000L;

    private Alarms() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static synchronized JSONObject state(Context c) {
        String s = prefs(c).getString(STATE, null);
        try { return s == null ? new JSONObject() : new JSONObject(s); } catch (JSONException e) { return new JSONObject(); }
    }

    static synchronized void saveState(Context c, String json) { prefs(c).edit().putString(STATE, json).commit(); }

    static String dayKey(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    static long atTime(long dayMs, String hhmm) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dayMs);
        String[] p = hhmm.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(p[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    static int notifId(String doseId) { return 1000 + (doseId.hashCode() & 0x3fffff); }

    private static JSONObject obj(JSONObject parent, String name) {
        JSONObject o = parent.optJSONObject(name);
        if (o == null) { o = new JSONObject(); try { parent.put(name, o); } catch (JSONException ignored) {} }
        return o;
    }

    private static List<JSONObject> sortedDoses(JSONObject s) {
        List<JSONObject> list = new ArrayList<>();
        JSONArray a = s.optJSONArray("doses");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject d = a.optJSONObject(i);
            if (d != null && d.optString("time").matches("\\d{1,2}:\\d{2}") && !d.optString("id").isEmpty()) list.add(d);
        }
        Collections.sort(list, (x, y) -> x.optString("time").compareTo(y.optString("time")));
        return list;
    }

    /** Recompute every dose's alarm from the saved state. Called after any change and at boot. */
    static synchronized void reschedule(Context c) {
        JSONObject s = state(c);
        JSONObject log = obj(s, "log"), snoozes = obj(s, "snoozes");
        SharedPreferences p = prefs(c);
        Set<String> fired = new HashSet<>(p.getStringSet(FIRED, new HashSet<String>()));
        Set<String> oldIds = p.getStringSet(IDS, new HashSet<String>());
        Set<String> ids = new HashSet<>();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        long now = System.currentTimeMillis();
        String today = dayKey(now);

        for (JSONObject d : sortedDoses(s)) {
            String id = d.optString("id"), time = d.optString("time");
            ids.add(id);
            String key = today + "|" + id;
            long sched = atTime(now, time);
            JSONObject sn = snoozes.optJSONObject(key);
            int count = (sn != null ? sn.optInt("count", 0) : 0) + 1;
            long tomorrow = atTime(now + DAY_MS, time);
            long at;
            if (log.has(key)) { at = tomorrow; nm.cancel(notifId(id)); }
            else if (now < sched) { at = sched; nm.cancel(notifId(id)); }
            else if (sn != null && now < sn.optLong("nextAt")) { at = sn.optLong("nextAt"); nm.cancel(notifId(id)); }
            else if (fired.contains(key + "#" + count)) { at = tomorrow; }   // already showing; wait for Done / Later
            else { at = now + 1000; }                                         // due now and not shown yet
            setAlarm(c, id, at);
        }
        for (String id : oldIds) if (!ids.contains(id)) { cancelAlarm(c, id); nm.cancel(notifId(id)); }

        Set<String> keep = new HashSet<>();
        for (String f : fired) if (f.startsWith(today)) keep.add(f);
        p.edit().putStringSet(IDS, ids).putStringSet(FIRED, keep).apply();
    }

    private static PendingIntent firePi(Context c, String id) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_FIRE).putExtra("id", id);
        return PendingIntent.getBroadcast(c, notifId(id), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(Context c, String id, long at) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = firePi(c, id);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); return; }
        // Alarm-clock alarms are the ones Android (and phone makers' battery savers) delay least: they wake the phone from Doze.
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, openPi(c, id, false)), pi);
    }

    private static void cancelAlarm(Context c, String id) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(firePi(c, id));
    }

    /** An alarm went off: show the dose if it is really due now, then line up its next alarm. */
    static synchronized void fire(Context c, String id) {
        JSONObject s = state(c);
        JSONObject log = obj(s, "log"), snoozes = obj(s, "snoozes");
        long now = System.currentTimeMillis();
        String key = dayKey(now) + "|" + id;
        List<JSONObject> doses = sortedDoses(s);
        for (int i = 0; i < doses.size(); i++) {
            JSONObject d = doses.get(i);
            if (!d.optString("id").equals(id)) continue;
            JSONObject sn = snoozes.optJSONObject(key);
            boolean due = !log.has(key) && now >= atTime(now, d.optString("time")) - 5000
                    && (sn == null || now >= sn.optLong("nextAt") - 5000);
            if (due) {
                int count = (sn != null ? sn.optInt("count", 0) : 0) + 1;
                show(c, s, d, i, key, count);
                SharedPreferences p = prefs(c);
                Set<String> fired = new HashSet<>(p.getStringSet(FIRED, new HashSet<String>()));
                fired.add(key + "#" + count);
                p.edit().putStringSet(FIRED, fired).commit();
            }
        }
        reschedule(c);
    }

    static synchronized void done(Context c, String id, String key) {
        JSONObject s = state(c);
        try {
            obj(s, "log").put(key, System.currentTimeMillis());
            obj(s, "snoozes").remove(key);
        } catch (JSONException ignored) {}
        saveState(c, s.toString());
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notifId(id));
        reschedule(c);
    }

    static synchronized void later(Context c, String id, String key, int count) {
        JSONObject s = state(c);
        if (!obj(s, "log").has(key)) {
            try {
                JSONObject sn = new JSONObject();
                sn.put("nextAt", System.currentTimeMillis() + SNOOZE_MS);
                sn.put("count", count);
                obj(s, "snoozes").put(key, sn);
            } catch (JSONException ignored) {}
            saveState(c, s.toString());
        }
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notifId(id));
        reschedule(c);
    }

    static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        Txt t = Txt.of(state(c).optString("lang", "fa"));
        NotificationChannel ch = new NotificationChannel(CHANNEL, t.channel, NotificationManager.IMPORTANCE_HIGH);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[] {0, 300, 150, 300});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        // Plays on the alarm volume, so it is heard even when the ringer is on silent.
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.deleteNotificationChannel(OLD_CHANNEL);
        nm.createNotificationChannel(ch);
    }

    private static PendingIntent actionPi(Context c, String action, String id, String key, int count, int slot) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(action).putExtra("id", id).putExtra("key", key).putExtra("count", count);
        return PendingIntent.getBroadcast(c, notifId(id) * 4 + slot, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressWarnings("deprecation")
    private static void show(Context c, JSONObject s, JSONObject dose, int index, String key, int count) {
        createChannel(c);
        Txt t = Txt.of(s.optString("lang", "fa"));
        String id = dose.optString("id");
        StringBuilder body = new StringBuilder();
        JSONArray meds = dose.optJSONArray("meds");
        if (meds != null) for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;
            if (body.length() > 0) body.append('\n');
            body.append("■ ").append(m.optString("name")).append(" — ")
                .append(t.num(m.optDouble("qty", 1))).append(' ').append(t.unit(m.optString("unit"), m.optDouble("qty", 1)));
        }
        String text = body.length() > 0 ? body.toString() : t.noMeds;
        String head = t.title + " · " + t.time(dose.optString("time"));

        Notification.Builder b = builder(c);
        b.setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFD52A1E)
            .setContentTitle(head)
            .setContentText(text.split("\n")[0])
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setSubText(t.dose(index + 1) + " · " + t.reminder(count))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setContentIntent(openPi(c, id, false))
            // Shown as a banner at the top while the phone is in use; on the lock screen it opens the reminder.
            .setFullScreenIntent(openPi(c, id, true), true)
            .setDeleteIntent(actionPi(c, ACTION_LATER, id, key, count, 2))   // swiping it away counts as "Later"
            .addAction(0, t.done, actionPi(c, ACTION_DONE, id, key, count, 1))
            .addAction(0, t.later, actionPi(c, ACTION_LATER, id, key, count, 2));
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(notifId(id), b.build());
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder builder(Context c) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c).setPriority(Notification.PRIORITY_MAX).setDefaults(Notification.DEFAULT_ALL);
    }

    private static PendingIntent openPi(Context c, String id, boolean overLock) {
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_REMINDER, overLock);
        return PendingIntent.getActivity(c, notifId(id) * 4 + (overLock ? 0 : 3), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** What could keep a reminder from showing, for the setup checklist in the page. */
    static JSONObject status(Context c) {
        JSONObject o = new JSONObject();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        try {
            boolean on = nm.areNotificationsEnabled();
            o.put("notif", on);
            boolean high = true;
            if (Build.VERSION.SDK_INT >= 26) {
                createChannel(c);
                NotificationChannel ch = nm.getNotificationChannel(CHANNEL);
                high = ch != null && ch.getImportance() >= NotificationManager.IMPORTANCE_HIGH;
            }
            o.put("channel", on && high);
            o.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
            o.put("battery", pm.isIgnoringBatteryOptimizations(c.getPackageName()));
            o.put("fsi", Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent());
        } catch (JSONException ignored) {}
        return o;
    }

    static void scheduleTest(Context c, long inMs) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_TEST).putExtra("id", "test");
        PendingIntent pi = PendingIntent.getBroadcast(c, TEST_ID, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long at = System.currentTimeMillis() + inMs;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, openPi(c, "test", false)), pi);
    }

    @SuppressWarnings("deprecation")
    static void showTest(Context c) {
        createChannel(c);
        Txt t = Txt.of(state(c).optString("lang", "fa"));
        Intent close = new Intent(c, AlarmReceiver.class).setAction(ACTION_TEST_CLOSE).putExtra("id", "test");
        PendingIntent closePi = PendingIntent.getBroadcast(c, TEST_ID + 1, close, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = builder(c);
        b.setSmallIcon(R.drawable.ic_stat).setColor(0xFF1447A6)
            .setContentTitle(t.testTitle).setContentText(t.testBody)
            .setStyle(new Notification.BigTextStyle().bigText(t.testBody))
            .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true).setShowWhen(true)
            .setContentIntent(openPi(c, "test", false))
            .setFullScreenIntent(openPi(c, "test", true), true)
            .addAction(0, t.done, closePi).addAction(0, t.later, closePi);
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(TEST_ID, b.build());
    }

    /** Notification wording in the language chosen in the app. */
    static final class Txt {
        final String lang, title, done, later, reminder, dose, noMeds, channel, testTitle, testBody;

        private Txt(String lang, String title, String done, String later, String reminder, String dose, String noMeds, String channel,
                    String testTitle, String testBody) {
            this.lang = lang; this.title = title; this.done = done; this.later = later;
            this.reminder = reminder; this.dose = dose; this.noMeds = noMeds; this.channel = channel;
            this.testTitle = testTitle; this.testBody = testBody;
        }

        static Txt of(String lang) {
            if ("en".equals(lang)) return new Txt("en", "Time for your medication", "Done", "Later", "Reminder %s", "Dose %s", "Take the medication for this dose.", "Medication reminders",
                    "Test notification", "If you see this at the top of the screen, reminders work.");
            if ("de".equals(lang)) return new Txt("de", "Zeit für Ihre Medikamente", "Erledigt", "Später", "Erinnerung %s", "Dosis %s", "Nehmen Sie die Medikamente dieser Dosis.", "Medikamenten-Erinnerungen",
                    "Testbenachrichtigung", "Wenn Sie das oben auf dem Bildschirm sehen, funktionieren die Erinnerungen.");
            return new Txt("fa", "وقت داروست", "خوردم", "بعدا", "یادآوری %s", "نوبت %s", "داروهای این نوبت را بخورید.", "یادآور دارو",
                "اعلان آزمایشی", "اگر این را بالای صفحه می‌بینید، یادآوری‌ها درست کار می‌کنند.");
        }

        String digits(String s) {
            if (!"fa".equals(lang)) return s;
            StringBuilder b = new StringBuilder();
            for (char ch : s.toCharArray()) b.append(ch >= '0' && ch <= '9' ? (char) ('۰' + (ch - '0')) : ch == '.' ? '٫' : ch);
            return b.toString();
        }

        String num(double v) {
            String s = v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
            return digits(s);
        }

        String reminder(int n) { return String.format(Locale.US, reminder, digits(String.valueOf(n))); }
        String dose(int n) { return String.format(Locale.US, dose, digits(String.valueOf(n))); }

        String time(String hhmm) {
            if (!"en".equals(lang)) return digits(String.format(Locale.US, "%05s", hhmm).replace(' ', '0'));
            try {
                Date d = new SimpleDateFormat("HH:mm", Locale.US).parse(hhmm);
                return new SimpleDateFormat("hh:mm a", Locale.US).format(d);
            } catch (Exception e) { return hhmm; }
        }

        String unit(String u, double qty) {
            boolean one = qty == 1;
            switch (lang) {
                case "en":
                    switch (u) {
                        case "capsule": return one ? "capsule" : "capsules";
                        case "ml": return "ml";
                        case "drop": return one ? "drop" : "drops";
                        case "spoon": return one ? "teaspoon" : "teaspoons";
                        default: return one ? "tablet" : "tablets";
                    }
                case "de":
                    switch (u) {
                        case "capsule": return one ? "Kapsel" : "Kapseln";
                        case "ml": return "ml";
                        case "drop": return "Tropfen";
                        case "spoon": return "Teelöffel";
                        default: return one ? "Tablette" : "Tabletten";
                    }
                default:
                    switch (u) {
                        case "capsule": return "کپسول";
                        case "ml": return "میلی‌لیتر";
                        case "drop": return "قطره";
                        case "spoon": return "قاشق چای‌خوری";
                        default: return "قرص";
                    }
            }
        }
    }
}
