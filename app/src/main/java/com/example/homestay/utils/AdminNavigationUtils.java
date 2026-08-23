package com.example.homestay.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.homestay.*;

public final class AdminNavigationUtils {
  private AdminNavigationUtils() {}

  public static void setup(Activity activity, int selectedId) {
    applyBottomInset(activity.findViewById(R.id.admin_bottom_nav));

    NavItem[] items = {
      new NavItem(
          R.id.admin_nav_dashboard,
          R.id.admin_icon_dashboard,
          R.id.admin_text_dashboard,
          AdminDashboardActivity.class),
      new NavItem(
          R.id.admin_nav_rooms,
          R.id.admin_icon_rooms,
          R.id.admin_text_rooms,
          AdminRoomsActivity.class),
      new NavItem(
          R.id.admin_nav_bookings,
          R.id.admin_icon_bookings,
          R.id.admin_text_bookings,
          AdminBookingsActivity.class),
      new NavItem(
          R.id.admin_nav_users,
          R.id.admin_icon_users,
          R.id.admin_text_users,
          AdminUsersActivity.class)
    };
    for (NavItem item : items) {
      boolean selected = item.containerId == selectedId;
      int color = selected ? Color.rgb(0, 58, 140) : Color.rgb(110, 116, 133);
      activity.<ImageView>findViewById(item.iconId).setColorFilter(color);
      activity.<TextView>findViewById(item.textId).setTextColor(color);
      activity
          .findViewById(item.containerId)
          .setOnClickListener(
              view -> {
                if (!selected) {
                  Intent intent =
                      new Intent(activity, item.target)
                          .addFlags(
                              Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                  | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                  activity.startActivity(intent);
                  activity.overridePendingTransition(0, 0);
                }
              });
    }
  }

  private static void applyBottomInset(View navigation) {
    ViewGroup.LayoutParams initialParams = navigation.getLayoutParams();
    int baseHeight = initialParams.height;
    int baseLeft = navigation.getPaddingLeft();
    int baseTop = navigation.getPaddingTop();
    int baseRight = navigation.getPaddingRight();

    ViewCompat.setOnApplyWindowInsetsListener(
        navigation,
        (view, windowInsets) -> {
          Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
          ViewGroup.LayoutParams params = view.getLayoutParams();
          int desiredHeight = baseHeight + navigationBars.bottom;
          if (params.height != desiredHeight) {
            params.height = desiredHeight;
            view.setLayoutParams(params);
          }
          view.setPadding(
              baseLeft + navigationBars.left,
              baseTop,
              baseRight + navigationBars.right,
              navigationBars.bottom);
          return windowInsets;
        });
    ViewCompat.requestApplyInsets(navigation);
  }

  private static final class NavItem {
    final int containerId;
    final int iconId;
    final int textId;
    final Class<? extends Activity> target;

    NavItem(int containerId, int iconId, int textId, Class<? extends Activity> target) {
      this.containerId = containerId;
      this.iconId = iconId;
      this.textId = textId;
      this.target = target;
    }
  }
}
