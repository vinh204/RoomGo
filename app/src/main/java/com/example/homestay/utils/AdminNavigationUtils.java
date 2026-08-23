package com.example.homestay.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.homestay.*;

public final class AdminNavigationUtils {
    private AdminNavigationUtils() {}

    public static void setup(Activity activity, int selectedId) {
        NavItem[] items = {
            new NavItem(R.id.admin_nav_dashboard, R.id.admin_icon_dashboard, R.id.admin_text_dashboard, AdminDashboardActivity.class),
            new NavItem(R.id.admin_nav_rooms, R.id.admin_icon_rooms, R.id.admin_text_rooms, AdminRoomsActivity.class),
            new NavItem(R.id.admin_nav_bookings, R.id.admin_icon_bookings, R.id.admin_text_bookings, AdminBookingsActivity.class),
            new NavItem(R.id.admin_nav_users, R.id.admin_icon_users, R.id.admin_text_users, AdminUsersActivity.class)
        };
        for (NavItem item : items) {
            boolean selected = item.containerId == selectedId;
            int color = selected ? Color.rgb(0, 58, 140) : Color.rgb(110, 116, 133);
            activity.<ImageView>findViewById(item.iconId).setColorFilter(color);
            activity.<TextView>findViewById(item.textId).setTextColor(color);
            activity.findViewById(item.containerId).setOnClickListener(view -> {
                if (!selected) {
                    Intent intent = new Intent(activity, item.target)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                }
            });
        }
    }

    private static final class NavItem {
        final int containerId;
        final int iconId;
        final int textId;
        final Class<? extends Activity> target;
        NavItem(int containerId, int iconId, int textId, Class<? extends Activity> target) {
            this.containerId = containerId; this.iconId = iconId; this.textId = textId; this.target = target;
        }
    }
}
