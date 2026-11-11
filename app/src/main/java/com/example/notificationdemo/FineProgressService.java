package com.example.notificationdemo;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;

public final class FineProgressService extends Service {
    private static final long INTERVAL_MS = 1000L;
    private HandlerThread thread;
    private Handler handler;
    private volatile int generation;

    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("fine-progress-notification");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        int run = ++generation;
        handler.removeCallbacksAndMessages(null);
        long startedAt = SystemClock.uptimeMillis();
        Notification.Builder builder = MainActivity.base(this, "精细下载演示", "已完成 0%")
                .setOnlyAlertOnce(true).setOngoing(true).setProgress(100, 0, false);
        Notification initial = builder.build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(MainActivity.PROGRESS_FINE, initial,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else startForeground(MainActivity.PROGRESS_FINE, initial);
        schedule(builder, startedAt, 1, run, startId);
        return START_NOT_STICKY;
    }

    private void schedule(Notification.Builder builder, long startedAt,
                          int nextValue, int run, int startId) {
        handler.postAtTime(() -> {
            if (run != generation) return;
            int elapsed = (int)Math.min(100, (SystemClock.uptimeMillis() - startedAt) / INTERVAL_MS);
            int value = Math.max(nextValue, elapsed);
            builder.setContentText(value == 100 ? "下载完成" : "已完成 " + value + "%")
                    .setProgress(value == 100 ? 0 : 100, value == 100 ? 0 : value, false)
                    .setOngoing(value < 100);
            MainActivity.post(this, MainActivity.PROGRESS_FINE, builder.build());
            if (value == 100) {
                stopForeground(STOP_FOREGROUND_DETACH);
                stopSelf(startId);
            } else schedule(builder, startedAt, value + 1, run, startId);
        }, startedAt + nextValue * INTERVAL_MS);
    }

    @Override public void onDestroy() {
        generation++;
        handler.removeCallbacksAndMessages(null);
        thread.quitSafely();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
