package com.example.notificationdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Random;

final class RandomNotificationIcon {
    private static final String PREFS = "random_icon";
    private static final String[] KEYS = {"background", "accent_one", "accent_two"};
    private static final Random RANDOM = new Random();
    private static Bitmap cached;

    private RandomNotificationIcon() { }

    static Bitmap current(Context context) {
        if (cached != null) return cached;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEYS[0])) return null;
        cached = draw(prefs.getInt(KEYS[0], Color.BLUE),
                prefs.getInt(KEYS[1], Color.YELLOW), prefs.getInt(KEYS[2], Color.RED));
        return cached;
    }

    static Bitmap generate(Context context) {
        int background = randomColor();
        int first = randomColor();
        int second = randomColor();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEYS[0], background).putInt(KEYS[1], first)
                .putInt(KEYS[2], second).apply();
        cached = draw(background, first, second);
        return cached;
    }

    private static int randomColor() {
        return Color.HSVToColor(new float[]{RANDOM.nextInt(360),
                .65f + RANDOM.nextFloat() * .25f, .75f + RANDOM.nextFloat() * .2f});
    }

    private static Bitmap draw(int background, int first, int second) {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path outline = new Path();
        outline.addRoundRect(new RectF(0, 0, 128, 128), 28, 28, Path.Direction.CW);
        canvas.clipPath(outline);
        canvas.drawColor(background);
        paint.setColor(first);
        canvas.drawCircle(98, 32, 62, paint);
        paint.setColor(second);
        canvas.drawCircle(34, 108, 53, paint);
        paint.setColor(0x66ffffff);
        canvas.drawCircle(98, 32, 17, paint);
        return image;
    }
}
