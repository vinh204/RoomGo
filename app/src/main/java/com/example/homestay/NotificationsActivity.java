package com.example.homestay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.homestay.data.entity.AppNotification;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.ui.adapter.NotificationAdapter;
import com.example.homestay.utils.*;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.*;

public class NotificationsActivity extends AppCompatActivity {
  private HomestayRepository repository;
  private long userId;
  private NotificationAdapter adapter;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    setContentView(R.layout.activity_notifications);
    SystemBarUtils.keepContentBelowStatusBar(this);
    repository = ((HomestayApplication) getApplication()).getRepository();
    userId = new SessionManager(this).getUserId();
    if (userId == -1) {
      finish();
      return;
    }
    ((MaterialToolbar) findViewById(R.id.toolbar_notifications))
        .setNavigationOnClickListener(v -> finish());
    adapter =
        new NotificationAdapter(
            n -> {
              AppExecutors.io().execute(() -> repository.markNotificationRead(n.getId()));
              startActivity(new Intent(this, MainActivity.class).putExtra("open_tab", "bookings"));
            });
    RecyclerView list = findViewById(R.id.recycler_notifications);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    findViewById(R.id.btn_mark_all_read)
        .setOnClickListener(
            v ->
                AppExecutors.io()
                    .execute(
                        () -> {
                          repository.markAllCustomerNotificationsRead(userId);
                          runOnUiThread(this::load);
                        }));
    load();
  }

  private void load() {
    AppExecutors.io()
        .execute(
            () -> {
              repository.syncBookingNotifications(userId);
              List<AppNotification> values = repository.getCustomerNotificationsNow(userId);
              runOnUiThread(
                  () -> {
                    adapter.submitList(values);
                    boolean empty = values.isEmpty(), unread = false;
                    for (AppNotification n : values)
                      if (!n.isRead()) {
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
}
