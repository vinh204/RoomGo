package com.example.homestay.data.repository;

import android.util.Log;
import com.example.homestay.data.dao.*;
import com.example.homestay.data.entity.*;
import com.example.homestay.utils.PasswordHasher;
import com.example.homestay.utils.AdminAuth;
import java.util.*;

public class HomestayRepository {
  private final RoomDao rooms;
  private final SlotDao slots;
  private final BookingDao bookings;
  private final UserDao users;
  private final FavoriteDao favorites;
  private final NotificationDao notifications;
  private final ReviewDao reviews;
  private final RoomImageDao roomImages;

  public HomestayRepository(
      RoomDao r,
      SlotDao s,
      BookingDao b,
      UserDao u,
      FavoriteDao f,
      NotificationDao n,
      ReviewDao v,
      RoomImageDao i) {
    rooms = r;
    slots = s;
    bookings = b;
    users = u;
    favorites = f;
    notifications = n;
    reviews = v;
    roomImages = i;
  }

  public List<Room> getAllRoomsNow() {
    return rooms.getAllRoomsNow();
  }

  public Room getRoomById(long id) {
    return rooms.getRoomById(id);
  }

  public long insertRoom(Room r) {
    return rooms.insertExclusiveFeatured(r);
  }

  public void insertRooms(List<Room> r) {
    rooms.insertRooms(r);
  }

  public void updateRoom(Room r) {
    rooms.updateExclusiveFeatured(r);
  }

  public void clearFeaturedRooms() {
    rooms.clearFeaturedRooms();
  }

  public boolean deleteRoom(Room r) {
    if (bookings.countByRoomId(r.getId()) > 0) return false;
    favorites.deleteByRoomId(r.getId());
    slots.deleteByRoomId(r.getId());
    rooms.deleteRoom(r);
    return true;
  }

  public List<RoomImage> getRoomImages(long roomId) {
    return roomImages.getByRoomId(roomId);
  }

  public void replaceRoomImages(long roomId, List<String> imageUris) {
    roomImages.deleteByRoomId(roomId);
    List<RoomImage> values = new ArrayList<>();
    for (int position = 0; position < imageUris.size(); position++)
      values.add(new RoomImage(0, roomId, imageUris.get(position), position));
    if (!values.isEmpty()) roomImages.insertAll(values);
  }

  public List<Slot> getSlotsByRoomIdNow(long id) {
    return slots.getSlotsByRoomIdNow(id);
  }

  public List<Slot> getAvailableSlotsByRoomIdNow(long id) {
    return slots.getAvailableSlotsByRoomIdNow(id);
  }

  public Slot getSlotById(long id) {
    return slots.getSlotById(id);
  }

  public long insertSlot(Slot s) {
    return slots.insertSlot(s);
  }

  public void insertSlots(List<Slot> s) {
    slots.insertSlots(s);
  }

  public void updateSlot(Slot s) {
    slots.updateSlot(s);
  }

  public void deleteSlot(Slot s) {
    slots.deleteSlot(s);
  }

  public List<Booking> getAllBookingsNow() {
    bookings.expirePendingBookings(System.currentTimeMillis());
    updateDueBookingLifecycle(System.currentTimeMillis());
    return bookings.getAllBookingsNow();
  }

  public List<Booking> getDatabaseBookingsForUser(long id) {
    bookings.expirePendingBookings(System.currentTimeMillis());
    updateDueBookingLifecycle(System.currentTimeMillis());
    return bookings.getBookingsByUserIdNow(id);
  }

  public Booking getBookingById(long id) {
    return bookings.getBookingById(id);
  }

  public void updateBooking(Booking b) {
    bookings.expirePendingBookings(System.currentTimeMillis());
    Booking current = bookings.getBookingById(b.getId());
    if (current != null
        && !com.example.homestay.domain.BookingStatusPolicy.canTransition(
            current.getStatus(), b.getStatus()))
      throw new IllegalArgumentException("Chuyển trạng thái booking không hợp lệ");
    bookings.updateBooking(b);
  }

  /** Cập nhật vòng đời lưu trú theo giờ nhận/trả phòng, an toàn khi được gọi lặp lại. */
  public List<Booking> updateDueBookingLifecycle(long now) {
    List<Booking> changed = new ArrayList<>();
    for (Booking booking : bookings.getBookingsNeedingLifecycleUpdate(now)) {
      int rows =
          booking.getCheckOutDate() <= now
              ? bookings.completeConfirmedBookingIfDue(booking.getId(), now)
              : bookings.checkInConfirmedBookingIfDue(booking.getId(), now);
      if (rows > 0) {
        Booking updated = bookings.getBookingById(booking.getId());
        if (updated != null) changed.add(updated);
      }
    }
    return changed;
  }

  public boolean deleteBooking(Booking b) {
    // Booking là chứng từ lịch sử; không xóa cứng trong luồng quản trị.
    return false;
  }

  public int countOverlappingBookings(long room, long in, long out) {
    return bookings.countOverlappingBookings(room, in, out);
  }

  public List<User> getAllUsersNow() {
    return users.getAllUsersNow();
  }

  public User login(String email, String password) {
    User u = users.getUserByEmailForLogin(email);
    return u != null && !u.isLocked() && PasswordHasher.verify(password, u.getPassword()) ? u : null;
  }

  public User getUserByEmail(String e) {
    return users.getUserByEmail(e);
  }

  public User getUserByPhone(String p) {
    return users.getUserByPhone(p);
  }

  public User getUserById(long id) {
    return users.getUserById(id);
  }

  public long insertUser(User u) {
    return users.insertUser(u.withPassword(PasswordHasher.hash(u.getPassword())));
  }

  public void ensureAdminAccount() {
    if (users.getUserByEmail(AdminAuth.EMAIL) == null)
      insertUser(
          new User(
              0,
              AdminAuth.EMAIL,
              "admin-local",
              "Admin@123",
              "Administrator",
              System.currentTimeMillis(),
              false,
              "ADMIN"));
  }

  public void updateUser(User u) {
    if (AdminAuth.EMAIL.equalsIgnoreCase(u.getEmail()) && !u.isAdmin())
      throw new IllegalArgumentException("Email này được dành riêng cho quản trị viên");
    User old = users.getUserById(u.getId());
    if (old != null
        && !u.getPassword().equals(old.getPassword())
        && !PasswordHasher.isValidHash(u.getPassword()))
      u = u.withPassword(PasswordHasher.hash(u.getPassword()));
    users.updateUser(u);
  }

  public boolean deleteUser(User u) {
    if (bookings.countByUserId(u.getId()) > 0) return false;
    favorites.deleteByUserId(u.getId());
    users.deleteUser(u);
    return true;
  }

  public List<Long> getFavoriteRoomIdsNow(long id) {
    return favorites.getFavoriteRoomIdsNow(id);
  }

  public Favorite getFavorite(long u, long r) {
    return favorites.getFavorite(u, r);
  }

  public long insertFavorite(Favorite f) {
    return favorites.insertFavorite(f);
  }

  public void deleteFavorite(Favorite f) {
    favorites.deleteFavorite(f);
  }

  public void deleteFavorite(long u, long r) {
    favorites.deleteFavorite(u, r);
  }

  public boolean isFavorite(long u, long r) {
    return favorites.isFavorite(u, r) > 0;
  }

  public boolean roomHasBookings(long id) {
    return bookings.countByRoomId(id) > 0;
  }

  /** Số phòng tối thiểu cần giữ để không làm sai các booking đang còn hiệu lực. */
  public int requiredRoomQuantity(long roomId) {
    List<Booking> active = new ArrayList<>();
    for (Booking booking : bookings.getAllBookingsNow())
      if (booking.getRoomId() == roomId
          && ("pending".equals(booking.getStatus())
              || "confirmed".equals(booking.getStatus())
              || "checked_in".equals(booking.getStatus())))
        active.add(booking);
    int required = 0;
    for (Booking anchor : active) {
      int overlapping = 0;
      for (Booking candidate : active)
        if (candidate.getCheckInDate() < anchor.getCheckOutDate()
            && candidate.getCheckOutDate() > anchor.getCheckInDate()) overlapping++;
      required = Math.max(required, overlapping);
    }
    return required;
  }

  public boolean userHasBookings(long id) {
    return bookings.countByUserId(id) > 0;
  }

  public List<AppNotification> getCustomerNotificationsNow(long id) {
    return notifications.getCustomerByUserNow(id);
  }

  public long insertNotification(AppNotification n) {
    return notifications.insert(n);
  }

  public void markNotificationRead(long id) {
    notifications.markRead(id);
  }

  public void markAllCustomerNotificationsRead(long id) {
    notifications.markAllCustomerRead(id);
  }

  public void markAllAdminNotificationsRead(long id) {
    notifications.markAllAdminRead(id);
  }

  public void syncBookingNotifications(long userId) {
    updateDueBookingLifecycle(System.currentTimeMillis());
    Map<Long, Room> map = new HashMap<>();
    for (Room r : rooms.getAllRoomsNow()) map.put(r.getId(), r);
    for (Booking b : bookings.getBookingsByUserIdNow(userId)) {
      Room r = map.get(b.getRoomId());
      String room = r == null ? "phòng đã đặt" : r.getName(),
          status = b.getStatus().toLowerCase(Locale.ROOT),
          title,
          message;
      switch (status) {
        case "confirmed":
          title = "Đặt phòng đã được xác nhận";
          message = "Booking tại " + room + " đã được xác nhận.";
          break;
        case "checked_in":
          title = "Đã đến giờ nhận phòng";
          message = "Chúc bạn có kỳ lưu trú vui vẻ tại " + room + ".";
          break;
        case "cancelled":
          title = "Đặt phòng đã bị hủy";
          message =
              "Booking tại "
                  + room
                  + " đã bị hủy."
                  + (b.getCancellationReason() == null
                      ? ""
                      : " Lý do: " + b.getCancellationReason() + ".")
                  + (b.getRefundAmount() > 0
                      ? " Khoản hoàn dự kiến: "
                          + com.example.homestay.utils.DisplayFormatter.vnd(b.getRefundAmount())
                          + "."
                      : " Không phát sinh khoản hoàn." );
          break;
        case "completed":
          title = "Chuyến đi đã hoàn thành";
          message = "Cảm ơn bạn đã lưu trú tại " + room + ". Hãy chia sẻ đánh giá của bạn.";
          break;
        case "expired":
          title = "Yêu cầu đặt phòng đã hết hạn";
          message = "Yêu cầu đặt " + room + " đã hết thời gian giữ phòng do chưa được xác nhận.";
          break;
        default:
          title = "Đã gửi yêu cầu đặt phòng";
          message = "Yêu cầu đặt " + room + " đang chờ quản trị viên xác nhận.";
      }
      notifications.insert(
          new AppNotification(
              0,
              userId,
              "booking_" + b.getId() + "_" + status,
              title,
              message,
              status,
              b.getId(),
              b.getRoomId(),
              false,
              "pending".equals(status) ? b.getCreatedAt() : System.currentTimeMillis()));
    }
  }

  public List<AppNotification> syncAdminActivityNotifications() {
    updateDueBookingLifecycle(System.currentTimeMillis());
    User admin = users.getUserByEmail("admin@gmail.com");
    if (admin == null) return Collections.emptyList();
    long adminId = admin.getId();
    Map<Long, User> userMap = new HashMap<>();
    for (User user : users.getAllUsersNow()) {
      userMap.put(user.getId(), user);
      if (user.getId() == adminId) continue;
      notifications.insert(
          new AppNotification(
              0,
              adminId,
              "admin_user_" + user.getId(),
              "Có tài khoản mới",
              user.getFullName() + " đã đăng ký tài khoản RoomGo.",
              "admin_user",
              null,
              null,
              false,
              user.getCreatedAt()));
    }

    Map<Long, Room> roomMap = new HashMap<>();
    for (Room room : rooms.getAllRoomsNow()) roomMap.put(room.getId(), room);
    for (Booking booking : bookings.getAllBookingsNow()) {
      User user = userMap.get(booking.getUserId());
      Room room = roomMap.get(booking.getRoomId());
      notifications.insert(
          new AppNotification(
              0,
              adminId,
              "admin_booking_" + booking.getId(),
              "Có yêu cầu đặt phòng mới",
              (user == null ? "Khách hàng" : user.getFullName())
                  + " đã đặt "
                  + (room == null ? "một phòng" : room.getName())
                  + ".",
              "admin_booking",
              booking.getId(),
              booking.getRoomId(),
              false,
              booking.getCreatedAt()));
      if ("cancelled".equals(booking.getStatus())
          && booking.getCancelledAt() > 0
          && (booking.getCancellationReason() == null
              || !booking.getCancellationReason().startsWith("Quản trị viên:"))) {
        notifications.insert(
            new AppNotification(
                0,
                adminId,
                "admin_booking_cancelled_" + booking.getId(),
                "Khách đã hủy đặt phòng",
                (user == null ? "Khách hàng" : user.getFullName())
                    + " đã hủy "
                    + (room == null ? "một phòng" : room.getName())
                    + ". Lý do: "
                    + (booking.getCancellationReason() == null
                        ? "Không cung cấp"
                        : booking.getCancellationReason()),
                "admin_booking",
                booking.getId(),
                booking.getRoomId(),
                false,
                booking.getCancelledAt()));
      }
      if ("completed".equals(booking.getStatus())) {
        notifications.insert(
            new AppNotification(
                0,
                adminId,
                "admin_booking_completed_" + booking.getId(),
                "Lưu trú đã tự động hoàn thành",
                (room == null ? "Booking" : room.getName())
                    + " đã qua giờ trả phòng và được chuyển sang Hoàn thành.",
                "admin_booking",
                booking.getId(),
                booking.getRoomId(),
                false,
                Math.max(booking.getCheckOutDate(), booking.getCreatedAt())));
      }
      if ("checked_in".equals(booking.getStatus())) {
        notifications.insert(
            new AppNotification(
                0,
                adminId,
                "admin_booking_checked_in_" + booking.getId(),
                "Khách đang trong kỳ lưu trú",
                (user == null ? "Khách hàng" : user.getFullName())
                    + " đã đến giờ nhận "
                    + (room == null ? "phòng" : room.getName())
                    + ".",
                "admin_booking",
                booking.getId(),
                booking.getRoomId(),
                false,
                Math.max(booking.getCheckInDate(), booking.getCreatedAt())));
      }
    }

    for (Review review : reviews.getAllNow()) {
      User user = userMap.get(review.getUserId());
      Room room = roomMap.get(review.getRoomId());
      notifications.insert(
          new AppNotification(
              0,
              adminId,
              "admin_review_" + review.getId() + "_" + review.getUpdatedAt(),
              "Có đánh giá phòng mới",
              (user == null ? "Khách hàng" : user.getFullName())
                  + " đã đánh giá "
                  + review.getRating()
                  + " sao cho "
                  + (room == null ? "phòng" : room.getName())
                  + ".",
              "admin_review",
              review.getBookingId(),
              review.getRoomId(),
              false,
              review.getUpdatedAt()));
    }
    return notifications.getAdminByUserNow(adminId);
  }

  public List<Review> getReviewsByRoomNow(long id) {
    return reviews.getByRoomNow(id);
  }

  public Review getReviewByBooking(long id) {
    return reviews.getByBooking(id);
  }

  public Review submitReview(long bookingId, long userId, int rating, String comment) {
    if (rating < 1 || rating > 5)
      throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao");
    Booking b = bookings.getBookingById(bookingId);
    if (b == null) throw new IllegalStateException("Không tìm thấy booking");
    if (b.getUserId() != userId)
      throw new IllegalArgumentException("Bạn không có quyền đánh giá booking này");
    if (!"completed".equals(b.getStatus()))
      throw new IllegalArgumentException("Chỉ có thể đánh giá sau khi hoàn thành lưu trú");
    Review current = reviews.getByBooking(bookingId), saved;
    long now = System.currentTimeMillis();
    if (current == null) {
      Review value =
          new Review(0, bookingId, b.getRoomId(), userId, rating, comment.trim(), true, now, now);
      saved = value.withId(reviews.insert(value));
    } else {
      saved = current.edited(rating, comment.trim(), now);
      reviews.update(saved);
    }
    Room room = rooms.getRoomById(b.getRoomId());
    if (room != null) {
      Double avg = reviews.getAverage(room.getId());
      rooms.updateRoom(
          room.withRating(avg == null ? 5f : avg.floatValue(), reviews.getCount(room.getId())));
    }
    return saved;
  }

  public void cleanupDuplicateRooms() {
    try {
      Map<String, List<Room>> groups = new HashMap<>();
      for (Room r : rooms.getAllRoomsNow())
        if (r.getMongoId() != null)
          groups.computeIfAbsent(r.getMongoId(), x -> new ArrayList<>()).add(r);
      for (List<Room> group : groups.values()) {
        group.sort(Comparator.comparingLong(Room::getId));
        for (int i = 1; i < group.size(); i++) deleteRoom(group.get(i));
      }
    } catch (Exception e) {
      Log.e("HomestayRepository", "Cleanup failed", e);
    }
  }

  public boolean refreshLocalRooms() {
    return true;
  }

  public void seedLocalRoomsIfNeeded() {
    if (!rooms.getAllRoomsNow().isEmpty()) return;
    rooms.insertRooms(
        Arrays.asList(
            seed(
                "local-room-1",
                "Homestay Đà Lạt View",
                "Phòng ấm cúng, ban công nhìn ra đồi thông.",
                650000,
                "room_dalat",
                "Đà Lạt",
                "12 Trần Hưng Đạo, Đà Lạt",
                "WiFi, Bếp, Ban công",
                2,
                "Phòng đôi",
                28,
                2),
            seed(
                "local-room-2",
                "Nhà Gỗ Bên Hồ",
                "Căn nhà gỗ yên tĩnh phù hợp cho gia đình.",
                1200000,
                "room_dalat",
                "Bảo Lộc",
                "Hồ Nam Phương, Bảo Lộc",
                "WiFi, Bếp, Bãi đỗ xe",
                5,
                "Nhà nguyên căn",
                65,
                1),
            seed(
                "local-room-3",
                "Studio Trung Tâm",
                "Studio hiện đại, gần chợ và khu ăn uống.",
                520000,
                "room_studio",
                "Đà Nẵng",
                "45 Nguyễn Văn Linh, Đà Nẵng",
                "WiFi, Điều hòa, TV",
                2,
                "Studio",
                24,
                3),
            seed(
                "local-room-4",
                "Villa Biển Xanh",
                "Villa rộng rãi gần biển, có hồ bơi riêng.",
                2800000,
                "room_beach",
                "Vũng Tàu",
                "18 Hạ Long, Vũng Tàu",
                "Hồ bơi, WiFi, BBQ",
                10,
                "Villa",
                180,
                1),
            seed(
                "local-room-5",
                "Phòng Phố Cổ",
                "Không gian nhỏ gọn ngay trung tâm phố cổ.",
                780000,
                "room_studio",
                "Hà Nội",
                "26 Hàng Bạc, Hoàn Kiếm",
                "WiFi, Điều hòa, Máy sấy",
                3,
                "Phòng gia đình",
                32,
                2)));
  }

  /** Tạo dữ liệu có đủ trạng thái để dùng khi thuyết trình/demo. */
  public void seedDemoData() {
    seedLocalRoomsIfNeeded();
    User customer = users.getUserByEmail("demo@roomgo.vn");
    if (customer == null) {
      long userId =
          insertUser(
              new User(
                  0,
                  "demo@roomgo.vn",
                  "0900000000",
                  "Demo@123",
                  "Khách hàng Demo",
                  System.currentTimeMillis() - 30L * 86400000L));
      customer = users.getUserById(userId);
    }
    if (customer == null || !bookings.getAllBookingsNow().isEmpty()) return;

    List<Room> demoRooms = rooms.getAllRoomsNow();
    if (demoRooms.isEmpty()) return;
    long now = System.currentTimeMillis();
    long created = now - 3600000L;
    Room first = demoRooms.get(0);
    Room second = demoRooms.get(Math.min(1, demoRooms.size() - 1));
    Room third = demoRooms.get(Math.min(2, demoRooms.size() - 1));
    Room fourth = demoRooms.get(Math.min(3, demoRooms.size() - 1));
    Room fifth = demoRooms.get(Math.min(4, demoRooms.size() - 1));

    bookings.insertBooking(
        demoBooking(customer.getId(), first, dayAt(now, 2, 14), dayAt(now, 4, 12),
            "pending", "UNPAID", created, created + 7200000L));
    bookings.insertBooking(
        demoBooking(customer.getId(), second, dayAt(now, 3, 14), dayAt(now, 5, 12),
            "confirmed", "UNPAID", created - 3600000L, 0));
    bookings.insertBooking(
        demoBooking(customer.getId(), third, dayAt(now, -1, 14), dayAt(now, 1, 12),
            "checked_in", "PAID", created - 7200000L, 0));
    bookings.insertBooking(
        demoBooking(customer.getId(), fourth, dayAt(now, -6, 14), dayAt(now, -4, 12),
            "completed", "PAID", created - 10800000L, 0));
    Booking cancelled =
        demoBooking(customer.getId(), fifth, dayAt(now, 6, 14), dayAt(now, 8, 12),
            "confirmed", "PAID", created - 14400000L, 0)
            .cancelled("Khách thay đổi kế hoạch", 0, now - 1800000L);
    bookings.insertBooking(cancelled);
  }

  private static Booking demoBooking(
      long userId,
      Room room,
      long checkIn,
      long checkOut,
      String status,
      String paymentStatus,
      long createdAt,
      long expiresAt) {
    long nights = Math.max(1, com.example.homestay.domain.BookingCalculator.nights(checkIn, checkOut));
    return new Booking(
        0, null, room.getId(), null, null, null, userId, null,
        checkIn, checkOut, 1, room.getPrice() * nights, status,
        "Thanh toán khi nhận phòng", createdAt, null, 0, 0, paymentStatus, expiresAt);
  }

  private static long dayAt(long base, int dayOffset, int hour) {
    Calendar value = Calendar.getInstance();
    value.setTimeInMillis(base);
    value.add(Calendar.DAY_OF_MONTH, dayOffset);
    value.set(Calendar.HOUR_OF_DAY, hour);
    value.set(Calendar.MINUTE, 0);
    value.set(Calendar.SECOND, 0);
    value.set(Calendar.MILLISECOND, 0);
    return value.getTimeInMillis();
  }

  private static Room seed(
      String mongo,
      String name,
      String desc,
      double price,
      String drawable,
      String location,
      String address,
      String amenities,
      int guests,
      String type,
      int area,
      int slots) {
    return new Room(
        0,
        mongo,
        name,
        desc,
        price,
        "android.resource://com.example.homestay/drawable/" + drawable,
        location,
        address,
        5f,
        0,
        amenities,
        guests,
        type,
        area,
        slots,
        true,
        false);
  }
}
