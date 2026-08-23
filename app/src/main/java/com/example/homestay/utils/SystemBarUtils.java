package com.example.homestay.utils;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class SystemBarUtils {
    private SystemBarUtils() {}

    public static void keepContentBelowStatusBar(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        int left = content.getPaddingLeft();
        int top = content.getPaddingTop();
        int right = content.getPaddingRight();
        int bottom = content.getPaddingBottom();
        activity.getWindow().setStatusBarColor(Color.WHITE);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
            .setAppearanceLightStatusBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
