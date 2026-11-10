package com.example.notificationdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DemoReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;
        switch (intent.getAction()) {
            case "media_toggle": MainActivity.showMedia(context, true); break;
            case "media_next": MainActivity.showMedia(context, false); break;
            case "call_answer":
                MainActivity.post(context, MainActivity.CALL, MainActivity.base(context, "通话中", "演示通话已接听").build()); break;
            case "call_decline":
                ((android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MainActivity.CALL); break;
        }
    }
}
