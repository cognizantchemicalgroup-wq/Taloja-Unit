package com.taloja.fiftytwoweeks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms are cleared on restart and app update; book the reminder again. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        ReminderReceiver.schedule(ctx);
        WeekWidget.updateAll(ctx);
    }
}
