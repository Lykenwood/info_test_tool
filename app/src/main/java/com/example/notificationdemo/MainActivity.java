package com.example.notificationdemo;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    static final String CHANNEL = "notification_examples";
    static final int PROGRESS = 101, MEDIA = 102, TIMER = 103, CALL = 104,
            MESSAGE = 105, TEXT = 106, PICTURE = 107, PROGRESS_FINE = 108;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int progressRun = 0;
    private TextView status;
    private static MediaSession mediaSession;
    private static boolean playing;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "通知演示", NotificationManager.IMPORTANCE_DEFAULT));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        list.setPadding(pad, pad, pad, pad);
        scroll.addView(list);
        TextView title = new TextView(this);
        title.setText("通知实验室"); title.setTextSize(28); title.setTextColor(Color.rgb(25, 35, 55));
        list.addView(title);
        status = new TextView(this);
        status.setText("点按按钮后，下拉通知栏查看效果。每种示例使用独立通知 ID。");
        status.setTextSize(15); status.setPadding(0, dp(10), 0, dp(16));
        list.addView(status);
        ImageView iconPreview = new ImageView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        previewParams.bottomMargin = dp(8);
        list.addView(iconPreview, previewParams);
        Bitmap currentIcon = RandomNotificationIcon.current(this);
        if (currentIcon == null) iconPreview.setImageResource(R.drawable.ic_launcher);
        else iconPreview.setImageBitmap(currentIcon);
        Button generateIcon = new Button(this);
        generateIcon.setText("生成随机通知图标"); generateIcon.setAllCaps(false);
        LinearLayout.LayoutParams iconButtonParams = new LinearLayout.LayoutParams(-1, dp(54));
        iconButtonParams.bottomMargin = dp(5);
        list.addView(generateIcon, iconButtonParams);
        generateIcon.setOnClickListener(v -> {
            iconPreview.setImageBitmap(RandomNotificationIcon.generate(this));
            status.setText("已生成随机图标。之后发送的通知会使用它作为大图标。");
        });
        TextView iconHint = new TextView(this);
        iconHint.setText("随机图标用于通知大图标；桌面图标保持固定。生成后再发送通知即可测试取色。");
        iconHint.setTextSize(13);
        iconHint.setPadding(0, 0, 0, dp(14));
        list.addView(iconHint);
        button(list, "进度条 · 每次增加 10%", () -> showProgress(10, PROGRESS));
        button(list, "进度条 · 1% 步进（每秒一次）", () ->
                startForegroundService(new Intent(this, FineProgressService.class)));
        button(list, "播放器 · 播放 / 暂停 / 下一首", () -> showMedia(this, false));
        button(list, "倒计时 · 60 秒", this::showTimer);
        button(list, "来电 · 接听 / 拒接", this::showCall);
        button(list, "聊天消息 · 多条会话", this::showMessage);
        button(list, "长文本 · 展开查看", this::showText);
        button(list, "大图片 · 展开查看", this::showPicture);
        button(list, "清除所有示例", () -> {
            progressRun++;
            stopService(new Intent(this, FineProgressService.class));
            manager.cancelAll();
            status.setText("已清除示例通知");
        });
        setContentView(scroll);
        getWindow().getDecorView().post(() -> {
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = getWindow().getDecorView().getWindowInsetsController();
                if (controller != null) controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        });
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private void button(LinearLayout list, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.bottomMargin = dp(9); list.addView(b, p);
        b.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                status.setText("请先允许发送通知，然后再点一次按钮");
            } else { action.run(); status.setText("已发送：" + label); }
        });
    }
    static Notification.Builder base(Context c, String title, String text) {
        Notification.Builder builder = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(text).setAutoCancel(false);
        Bitmap icon = RandomNotificationIcon.current(c);
        if (icon != null) builder.setLargeIcon(icon);
        return builder;
    }
    static void post(Context c, int id, Notification n) {
        ((NotificationManager)c.getSystemService(NOTIFICATION_SERVICE)).notify(id, n);
    }
    static PendingIntent action(Context c, String name, int request) {
        Intent i = new Intent(c, DemoReceiver.class).setAction(name);
        return PendingIntent.getBroadcast(c, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private void showProgress(int step, int notificationId) {
        int run = ++progressRun;
        long intervalMs = 1000L;
        Notification.Builder builder = base(this, step == 1 ? "精细下载演示" : "文件下载演示", "已完成 0%")
                .setOnlyAlertOnce(true);
        Runnable update = new Runnable() {
            int value;
            @Override public void run() {
                if (run != progressRun) return;
                long nextAt = SystemClock.uptimeMillis() + intervalMs;
                builder.setContentText(value == 100 ? "下载完成" : "已完成 " + value + "%")
                        .setProgress(100, value, false).setOngoing(value < 100);
                if (value == 100) builder.setProgress(0, 0, false);
                post(MainActivity.this, notificationId, builder.build());
                if (value < 100) {
                    value += step;
                    handler.postAtTime(this, nextAt);
                }
            }
        };
        update.run();
    }
    static void showMedia(Context c, boolean toggle) {
        if (toggle) playing = !playing; else playing = true;
        if (mediaSession == null) {
            mediaSession = new MediaSession(c.getApplicationContext(), "NotificationDemo");
            mediaSession.setActive(true);
        }
        mediaSession.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "演示曲目")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "通知实验室")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 180000).build());
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, playing ? 1f : 0f).build());
        Notification.Action play = new Notification.Action.Builder(R.drawable.ic_notification,
                playing ? "暂停" : "播放", action(c, "media_toggle", 1)).build();
        Notification.Action next = new Notification.Action.Builder(R.drawable.ic_notification,
                "下一首", action(c, "media_next", 2)).build();
        Notification n = base(c, "演示曲目", "通知实验室 · 模拟播放器（无音频）")
                .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(0, 1))
                .addAction(play).addAction(next).setOngoing(playing).setOnlyAlertOnce(true).build();
        post(c, MEDIA, n);
    }
    private void showTimer() {
        long end = System.currentTimeMillis() + 60000;
        post(this, TIMER, base(this, "60 秒倒计时", "系统计时显示，无需每秒更新通知")
                .setWhen(end).setUsesChronometer(true).setChronometerCountDown(true)
                .setOngoing(true).build());
        handler.postDelayed(() -> post(this, TIMER, base(this, "倒计时结束", "60 秒已到").build()), 60000);
    }
    private void showCall() {
        Notification.Builder b = base(this, "演示来电", "小明正在呼叫")
                .setCategory(Notification.CATEGORY_CALL).setOngoing(true);
        if (Build.VERSION.SDK_INT >= 31) {
            b.setStyle(Notification.CallStyle.forIncomingCall(
                    new android.app.Person.Builder().setName("小明").setImportant(true).build(),
                    action(this, "call_decline", 3), action(this, "call_answer", 4)));
        } else {
            b.addAction(R.drawable.ic_notification, "拒接", action(this, "call_decline", 3));
            b.addAction(R.drawable.ic_notification, "接听", action(this, "call_answer", 4));
        }
        post(this, CALL, b.build());
    }
    private void showMessage() {
        android.app.Person me = new android.app.Person.Builder().setName("我").build();
        android.app.Person friend = new android.app.Person.Builder().setName("小明").build();
        long now = System.currentTimeMillis();
        Notification.MessagingStyle style = new Notification.MessagingStyle(me).setConversationTitle("周末计划")
                .addMessage("周末有空吗？", now - 10000, friend)
                .addMessage("一起去公园吧", now, friend);
        post(this, MESSAGE, base(this, "周末计划", "小明：一起去公园吧")
                .setCategory(Notification.CATEGORY_MESSAGE).setStyle(style).build());
    }
    private void showText() {
        String content = "这是长文本通知示例。展开后可以看到更多内容。通知监听器通常可从 extras 中读取正文和扩展文本；实际字段取决于发布通知的应用。";
        post(this, TEXT, base(this, "长文本通知", "下拉展开查看完整内容")
                .setStyle(new Notification.BigTextStyle().bigText(content)).build());
    }
    private void showPicture() {
        Bitmap bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(48, 96, 174));
        Paint p = new Paint(3); p.setColor(Color.WHITE); p.setTextSize(64);
        canvas.drawText("通知图片示例", 130, 215, p);
        post(this, PICTURE, base(this, "大图片通知", "下拉展开查看图片")
                .setStyle(new Notification.BigPictureStyle().bigPicture(bitmap)).build());
    }
}
