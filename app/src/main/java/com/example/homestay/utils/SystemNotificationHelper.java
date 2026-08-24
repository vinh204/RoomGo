package com.example.homestay.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.homestay.NotificationsActivity;
import com.example.homestay.AdminNotificationsActivity;
import com.example.homestay.R;
import com.example.homestay.data.entity.AppNotification;
import java.util.List;

/** Hiển thị các thông báo SQLite mới trên khay thông báo của Android. */
public final class SystemNotificationHelper {
  private static final String CHANNEL_ID = "roomgo_booking_updates";
  private static final String PREFS = "RoomGoSystemNotifications";

  private SystemNotificationHelper() {}

  public static void createChannel(Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL_ID, "Cập nhật đặt phòng", NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription("Xác nhận, hoàn thành, hủy và các cập nhật đặt phòng RoomGo");
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null) manager.createNotificationChannel(channel);
  }

  public static void publishNew(Context context, List<AppNotification> notifications) {
    if (notifications == null || notifications.isEmpty() || !canNotify(context)) return;
    SharedPreferences shown = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = shown.edit();
    NotificationManagerCompat manager = NotificationManagerCompat.from(context);

    for (int i = notifications.size() - 1; i >= 0; i--) {
      AppNotification value = notifications.get(i);
      String key = value.getUserId() + "_" + value.getEventKey();
      if (value.isRead() || shown.getBoolean(key, false)) continue;

      boolean adminNotification = value.getType().startsWith("admin_");
      Intent intent =
          new Intent(
              context,
              adminNotification ? AdminNotificationsActivity.class : NotificationsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent pendingIntent =
          PendingIntent.getActivity(
              context,
              value.getEventKey().hashCode(),
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

      NotificationCompat.Builder builder =
          new NotificationCompat.Builder(context, CHANNEL_ID)
              .setSmallIcon(R.drawable.ic_notifications)
              .setContentTitle(value.getTitle())
              .setContentText(value.getMessage())
              .setStyle(new NotificationCompat.BigTextStyle().bigText(value.getMessage()))
              .setColor(ContextCompat.getColor(context, R.color.home_primary))
              .setContentIntent(pendingIntent)
              .setAutoCancel(true)
              .setOnlyAlertOnce(true)
              .setPriority(NotificationCompat.PRIORITY_DEFAULT)
              .setCategory(NotificationCompat.CATEGORY_STATUS);
      manager.notify(value.getEventKey().hashCode(), builder.build());
      editor.putBoolean(key, true);
    }
    editor.apply();
  }

  private static boolean canNotify(Context context) {
    return Build.VERSION.SDK_INT < 33
        || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
  }
}
