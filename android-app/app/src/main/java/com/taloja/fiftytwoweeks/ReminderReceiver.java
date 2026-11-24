package com.taloja.fiftytwoweeks;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;

/** Fires once a day at the chosen time and nudges you if today isn't logged. */
public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL = "daily_reminder";
    private static final int NOTIF_ID = 52;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        schedule(ctx);              // book tomorrow's reminder
        WeekWidget.updateAll(ctx);  // new day: refresh the widget too
        if (!Store.prefs(ctx).getBoolean("remind", true)) return;
        Store.Snapshot s = Store.snapshot(ctx);
        if (s.inRange && s.todayStatus == Store.NONE) show(ctx, s, false);
    }

    static void schedule(Context ctx) {
        SharedPreferences p = Store.prefs(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1, new Intent(ctx, ReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (!p.getBoolean("remind", true)) return;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, p.getInt("hour", 20));
        c.set(Calendar.MINUTE, p.getInt("minute", 0));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_MONTH, 1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
    }

    static boolean canNotify(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }

    static void show(Context ctx, Store.Snapshot s, boolean test) {
        if (!canNotify(ctx)) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Daily reminder",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("A nudge when you haven't logged a workout or day off today");
            nm.createNotificationChannel(ch);
        }
        PendingIntent open = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = test ? "Reminder test: this is how it looks" : "You haven't logged today";
        String text = s.streak > 0
                ? "Week " + s.week + " · Day " + s.dayNum + ". Log a workout or a day off to keep your " + s.streak + "-day streak."
                : "Week " + s.week + " · Day " + s.dayNum + ". Log a workout or a day off.";

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, CHANNEL)
                : new Notification.Builder(ctx);
        b.setSmallIcon(R.drawable.ic_stat_dumbbell)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true);
        nm.notify(NOTIF_ID, b.build());
    }
}
