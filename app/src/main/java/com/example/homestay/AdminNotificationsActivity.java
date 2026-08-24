package com.example.homestay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.data.entity.AppNotification;
import com.example.homestay.data.entity.User;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.ui.adapter.NotificationAdapter;
import com.example.homestay.utils.AdminAuth;
import com.example.homestay.utils.AppExecutors;
import com.example.homestay.utils.SystemBarUtils;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;

/** Danh sách thông báo riêng dành cho quản trị viên. */
public class AdminNotificationsActivity extends AppCompatActivity {
  private HomestayRepository repository;
  private NotificationAdapter adapter;
  private long adminId = -1;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    if (!getSharedPreferences("AdminSession", MODE_PRIVATE)
        .getBoolean("is_admin_logged_in", false)) {
      finish();
      return;
    }
    setContentView(R.layout.activity_notifications);
    SystemBarUtils.keepContentBelowStatusBar(this);
    repository = ((HomestayApplication) getApplication()).getRepository();
    MaterialToolbar toolbar = findViewById(R.id.toolbar_notifications);
    toolbar.setTitle("Thông báo quản trị");
    toolbar.setNavigationOnClickListener(view -> finish());
    adapter =
        new NotificationAdapter(
            notification -> {
              AppExecutors.io()
                  .execute(() -> repository.markNotificationRead(notification.getId()));
              openRelatedScreen(notification);
            });
    RecyclerView list = findViewById(R.id.recycler_notifications);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    findViewById(R.id.btn_mark_all_read)
        .setOnClickListener(
            view ->
                AppExecutors.io()
                    .execute(
                        () -> {
                          repository.markAllAdminNotificationsRead(adminId);
                          runOnUiThread(this::load);
                        }));
    load();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (repository != null) load();
  }

  private void load() {
    AppExecutors.io()
        .execute(
            () -> {
              User admin = repository.getUserByEmail(AdminAuth.EMAIL);
              if (admin == null) return;
              adminId = admin.getId();
              List<AppNotification> values = repository.syncAdminActivityNotifications();
              runOnUiThread(
                  () -> {
                    adapter.submitList(values);
                    boolean empty = values.isEmpty();
                    boolean unread = false;
                    for (AppNotification value : values)
                      if (!value.isRead()) {
                        unread = true;
                        break;
                      }
                    findViewById(R.id.layout_empty_notifications)
                        .setVisibility(empty ? View.VISIBLE : View.GONE);
                    findViewById(R.id.recycler_notifications)
                        .setVisibility(empty ? View.GONE : View.VISIBLE);
                    findViewById(R.id.btn_mark_all_read)
                        .setVisibility(unread ? View.VISIBLE : View.GONE);
                  });
            });
  }

  private void openRelatedScreen(AppNotification notification) {
    Class<?> target =
        "admin_user".equals(notification.getType())
            ? AdminUsersActivity.class
            : "admin_booking".equals(notification.getType())
                ? AdminBookingsActivity.class
                : AdminRoomsActivity.class;
    startActivity(new Intent(this, target));
  }
}
