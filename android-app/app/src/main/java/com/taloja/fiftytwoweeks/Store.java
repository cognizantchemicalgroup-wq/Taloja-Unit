package com.taloja.fiftytwoweeks;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Native copy of the log for the widget and the reminder.
 * The page sends the logged dates through Android.syncState(); everything
 * else (week, streak, attendance) is worked out here so it stays correct
 * even when the app is closed.
 */
final class Store {
    static final int TOTAL = 364;
    private static final String PREFS = "w52";

    private Store() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static Calendar start() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 19);
        return c;
    }

    static Calendar today() {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        return c;
    }

    static String iso(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static int dayIdx(Calendar d) {
        long ms = d.getTimeInMillis() - start().getTimeInMillis();
        return (int) Math.round(ms / 86400000.0);
    }

    static Calendar dayAt(int k) {
        Calendar c = start();
        c.add(Calendar.DAY_OF_MONTH, k);
        return c;
    }

    private static Set<String> readSet(Context c, String key) {
        Set<String> out = new HashSet<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (JSONException ignored) { }
        return out;
    }

    static void saveState(Context c, String json) {
        try {
            JSONObject o = new JSONObject(json);
            JSONArray w = o.optJSONArray("workout");
            JSONArray off = o.optJSONArray("off");
            prefs(c).edit()
                    .putString("workout", w == null ? "[]" : w.toString())
                    .putString("off", off == null ? "[]" : off.toString())
                    .apply();
        } catch (JSONException ignored) { }
    }

    /** Status values used by {@link Snapshot#week} and {@link Snapshot#todayStatus}. */
    static final int NONE = 0, DONE = 1, OFF = 2, MISSED = 3;

    static final class Snapshot {
        boolean inRange;
        int week, dayNum, daysLeft, streak, present, off, missed, attendancePct;
        int todayStatus;          // NONE, DONE or OFF
        int todayCol;             // 0-6 position of today in this week's row, -1 if outside
        int[] week7 = new int[7]; // status of each day this week
    }

    static Snapshot snapshot(Context c) {
        Set<String> work = readSet(c, "workout");
        Set<String> offs = readSet(c, "off");
        Snapshot s = new Snapshot();
        int ti = dayIdx(today());
        s.inRange = ti >= 0 && ti < TOTAL;
        int i = Math.max(0, Math.min(TOTAL - 1, ti));
        s.week = i / 7 + 1;
        s.dayNum = i + 1;
        s.daysLeft = Math.max(0, TOTAL - 1 - i);

        for (int k = 0; k <= Math.min(ti, TOTAL - 1); k++) {
            String key = iso(dayAt(k));
            if (work.contains(key)) s.present++;
            else if (offs.contains(key)) s.off++;
            else if (k < ti) s.missed++;
        }
        int counted = s.present + s.off + s.missed;
        s.attendancePct = counted == 0 ? 0 : Math.round(s.present * 100f / counted);

        String todayKey = iso(today());
        s.todayStatus = work.contains(todayKey) ? DONE : offs.contains(todayKey) ? OFF : NONE;

        // Streak: consecutive logged days back from today (or yesterday if today isn't logged yet).
        int k = s.todayStatus == NONE ? ti - 1 : ti;
        while (k >= 0 && k < TOTAL) {
            String key = iso(dayAt(k));
            if (!work.contains(key) && !offs.contains(key)) break;
            s.streak++;
            k--;
        }

        int ws = (s.week - 1) * 7;
        s.todayCol = (ti >= ws && ti < ws + 7) ? ti - ws : -1;
        for (int d = 0; d < 7; d++) {
            int kk = ws + d;
            String key = iso(dayAt(kk));
            s.week7[d] = work.contains(key) ? DONE : offs.contains(key) ? OFF : (kk < ti ? MISSED : NONE);
        }
        return s;
    }
}
