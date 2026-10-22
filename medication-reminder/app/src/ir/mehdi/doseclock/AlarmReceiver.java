package ir.mehdi.doseclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm went off, or Done / Later was pressed on the notification. Not exported: only our own PendingIntents reach it. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String id = i.getStringExtra("id");
        if (id == null || i.getAction() == null) return;
        switch (i.getAction()) {
            case Alarms.ACTION_FIRE: Alarms.fire(c, id); break;
            case Alarms.ACTION_DONE: Alarms.done(c, id, i.getStringExtra("key")); break;
            case Alarms.ACTION_LATER: Alarms.later(c, id, i.getStringExtra("key"), i.getIntExtra("count", 1)); break;
            default: return;
        }
        MainActivity.reloadIfOpen();
    }
}
