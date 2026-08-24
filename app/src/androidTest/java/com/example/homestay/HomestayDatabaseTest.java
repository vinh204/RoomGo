package com.example.homestay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.homestay.data.database.HomestayDatabase;
import com.example.homestay.data.entity.User;
import com.example.homestay.data.entity.Booking;
import com.example.homestay.data.entity.Room;
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

  @Test
  public void permanentAccountLockIsStoredInDatabase() {
    User user =
        new User(
            0, "locked@example.com", "0900000001", "hash", "Locked User", 1L, true, "CUSTOMER");
    long id = database.userDao().insertUser(user);
    assertTrue(database.userDao().getUserById(id).isLocked());
    assertEquals("CUSTOMER", database.userDao().getUserById(id).getRole());
  }

  @Test
  public void bookingCapacityIsCheckedInsideTransaction() {
    Room room =
        new Room(
            0, null, "Demo", "Demo", 500_000, "", "Đà Nẵng", "", 5f, 0, "WiFi", 2,
            "Phòng", 20, 1, true, false);
    long roomId = database.roomDao().insertRoom(room);
    Booking first = booking(roomId, 1_000L, 2_000L);
    Booking second = booking(roomId, 1_500L, 2_500L);
    assertTrue(database.bookingDao().insertIfCapacityAvailable(first, 1) > 0);
    assertEquals(-1L, database.bookingDao().insertIfCapacityAvailable(second, 1));
  }

  private static Booking booking(long roomId, long checkIn, long checkOut) {
    return new Booking(
        0, null, roomId, null, null, null, 1, null, checkIn, checkOut, 1, 500_000,
        "pending", "pay_on_site", 1L, null, 0, 0);
  }
}
