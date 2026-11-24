package com.taloja.fiftytwoweeks;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Home-screen widget: this week's 7 days, today's status, streak and attendance. */
public class WeekWidget extends AppWidgetProvider {
    private static final int[] DOTS = {R.id.d0, R.id.d1, R.id.d2, R.id.d3, R.id.d4, R.id.d5, R.id.d6};
    private static final int[] LABELS = {R.id.l0, R.id.l1, R.id.l2, R.id.l3, R.id.l4, R.id.l5, R.id.l6};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        render(ctx, mgr, ids);
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        if (mgr == null) return;
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, WeekWidget.class));
        if (ids != null && ids.length > 0) render(ctx, mgr, ids);
    }

    private static void render(Context ctx, AppWidgetManager mgr, int[] ids) {
        Store.Snapshot s = Store.snapshot(ctx);
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);

        v.setTextViewText(R.id.w_week, "WEEK " + s.week + " / 52");
        if (!s.inRange) {
            v.setTextViewText(R.id.w_status, Store.dayIdx(Store.today()) < 0 ? "Starts 19 Nov" : "Challenge complete!");
            v.setTextColor(R.id.w_status, ctx.getColor(R.color.w_fg));
        } else if (s.todayStatus == Store.DONE) {
            v.setTextViewText(R.id.w_status, "✓ Workout logged today");
            v.setTextColor(R.id.w_status, ctx.getColor(R.color.w_done));
        } else if (s.todayStatus == Store.OFF) {
            v.setTextViewText(R.id.w_status, "✓ Day off logged");
            v.setTextColor(R.id.w_status, ctx.getColor(R.color.w_off));
        } else {
            v.setTextViewText(R.id.w_status, "Not logged today · tap to log");
            v.setTextColor(R.id.w_status, ctx.getColor(R.color.w_bad));
        }

        for (int d = 0; d < 7; d++) {
            int res;
            switch (s.week7[d]) {
                case Store.DONE: res = R.drawable.dot_done; break;
                case Store.OFF: res = R.drawable.dot_off; break;
                case Store.MISSED: res = R.drawable.dot_miss; break;
                default: res = R.drawable.dot_future; break;
            }
            v.setImageViewResource(DOTS[d], res);
            v.setTextColor(LABELS[d], ctx.getColor(d == s.todayCol ? R.color.w_fg : R.color.w_muted));
        }

        v.setTextViewText(R.id.w_foot, "Streak " + s.streak + " · Attendance " + s.attendancePct + "% · " + s.daysLeft + " days left");

        PendingIntent open = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, open);
        for (int id : ids) mgr.updateAppWidget(id, v);
    }
}
