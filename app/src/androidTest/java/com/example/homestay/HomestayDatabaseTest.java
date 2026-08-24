package com.example.homestay;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.homestay.data.database.HomestayDatabase;
import com.example.homestay.data.entity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomestayDatabaseTest {
  private HomestayDatabase database;

  @Before
  public void createDatabase() {
    Context context = ApplicationProvider.getApplicationContext();
    database =
        Room.inMemoryDatabaseBuilder(context, HomestayDatabase.class)
            .allowMainThreadQueries()
            .build();
  }

  @After
  public void closeDatabase() {
    database.close();
  }

  @Test
  public void userCanBeInsertedAndRead() {
    User user = new User(0, "roomgo@example.com", "0900000000", "hash", "RoomGo", 1L);
    long id = database.userDao().insertUser(user);
    assertEquals("roomgo@example.com", database.userDao().getUserById(id).getEmail());
  }
}
