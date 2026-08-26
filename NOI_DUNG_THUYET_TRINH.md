# NỘI DUNG THUYẾT TRÌNH CHI TIẾT DỰ ÁN ROOMGO

> Quy mô: 15 slide, thời lượng đề xuất 12-15 phút. Phần **Nội dung trên slide** dùng để đưa vào PowerPoint; phần **Lời thuyết trình** dùng làm ghi chú cho người trình bày.

---

## Slide 1 - Giới thiệu đề tài

### Nội dung trên slide

**ROOMGO - Ứng dụng tìm kiếm và đặt phòng homestay trên Android**

- Nền tảng: Android 7.0 trở lên.
- Ngôn ngữ: Java; giao diện XML.
- Cơ sở dữ liệu: Room Database trên SQLite.
- Hai nhóm người dùng: khách hàng và quản trị viên.
- Mô hình hiện tại: ứng dụng demo lưu dữ liệu cục bộ.

### Lời thuyết trình

RoomGo là ứng dụng Android mô phỏng trọn vẹn quy trình tìm kiếm, đặt và quản lý phòng homestay. Khách hàng có thể tìm phòng, chọn ngày, đặt chỗ, theo dõi trạng thái và đánh giá. Quản trị viên quản lý phòng, booking, tài khoản và doanh thu. Phiên bản hiện tại tập trung vào nghiệp vụ và lưu dữ liệu cục bộ bằng Room/SQLite.

### Hình minh họa

Logo RoomGo, màn hình Splash và màn hình đăng nhập.

---

## Slide 2 - Bài toán, lý do chọn đề tài và mục tiêu

### Nội dung trên slide

**Bài toán**

- Khách khó biết chính xác phòng còn trống theo từng khoảng ngày.
- Quản lý thủ công dễ trùng lịch, sai giá và khó theo dõi trạng thái.
- Dữ liệu phòng, khách, booking và doanh thu bị phân tán.

**Mục tiêu**

- Số hóa quy trình tìm và đặt homestay.
- Quản lý thống nhất vòng đời booking.
- Kiểm tra sức chứa, số phòng trống, giá và hoàn tiền.
- Tạo trải nghiệm riêng cho khách hàng và quản trị viên.

### Lời thuyết trình

Điểm chính của bài toán không chỉ là hiển thị danh sách phòng mà còn phải bảo đảm một booking hợp lệ: đúng ngày, đủ sức chứa, còn phòng, đúng giá và chuyển trạng thái đúng quy tắc. RoomGo giải quyết cả trải nghiệm khách hàng lẫn công việc quản trị trong một ứng dụng.

---

## Slide 3 - Phạm vi chức năng và các tác nhân

### Nội dung trên slide

| Khách hàng | Quản trị viên |
|---|---|
| Đăng ký, đăng nhập | Xem dashboard |
| Tìm, lọc, xem phòng | Quản lý phòng và ảnh |
| Lưu phòng yêu thích | Duyệt và xử lý booking |
| Đặt hoặc hủy phòng | Quản lý người dùng |
| Theo dõi trạng thái | Theo dõi doanh thu |
| Nhận thông báo | Nhận thông báo quản trị |
| Đánh giá sau lưu trú | Xem giao diện khách |

**Ngoài phạm vi hiện tại:** backend, đồng bộ đa thiết bị, cổng thanh toán thật và bản đồ trực tuyến.

### Lời thuyết trình

Hệ thống có hai vai trò được lưu riêng là `CUSTOMER` và `ADMIN`. Khách hàng tập trung vào quá trình đặt phòng; admin tập trung vào vận hành. Vì đây là bản demo cục bộ nên các tính năng cần hạ tầng bên ngoài như thanh toán thật và đồng bộ nhiều thiết bị chưa được triển khai.

---

## Slide 4 - Công nghệ sử dụng

### Nội dung trên slide

| Thành phần | Công nghệ và vai trò |
|---|---|
| Java 11 | Xử lý giao diện và nghiệp vụ |
| XML, Material Components | Thiết kế màn hình và dialog |
| Room 2.6.1 / SQLite | Lưu trữ dữ liệu cục bộ |
| RecyclerView | Hiển thị danh sách hiệu quả |
| Adapter, ViewHolder | Gắn dữ liệu vào từng phần tử |
| ViewPager2 | Trình chiếu nhiều ảnh phòng |
| WorkManager | Cập nhật booking định kỳ |
| BCrypt | Băm mật khẩu |
| Gradle 8.13, AGP 8.13.1 | Biên dịch và đóng gói |
| minSdk 24, targetSdk 36 | Phạm vi Android hỗ trợ |

### Lời thuyết trình

RecyclerView tái sử dụng ViewHolder khi cuộn, giúp danh sách phòng và booking hoạt động hiệu quả. Adapter là cầu nối giữa dữ liệu và giao diện từng dòng. WorkManager đảm nhiệm công việc nền. BCrypt được dùng để không lưu mật khẩu dạng văn bản thuần.

---

## Slide 5 - Kiến trúc và luồng dữ liệu

### Nội dung trên slide

```text
Activity / Dialog / RecyclerView
              ↓
         Adapter, Model
              ↓
           Repository
        ↙             ↘
 Domain Rules         DAO
                        ↓
              Room Database
                        ↓
                     SQLite
```

- 72 tệp Java, 35 layout XML.
- Nhóm mã chính: `ui`, `data`, `domain`, `utils`, `worker`.
- Intent chuyển màn hình và truyền ID phòng/booking.
- Executor chạy truy vấn ngoài UI thread.

### Lời thuyết trình

Activity tiếp nhận thao tác người dùng nhưng không trực tiếp viết SQL. Repository điều phối nghiệp vụ và gọi DAO. Domain chứa các quy tắc thuần Java như tính tiền, tìm kiếm, trạng thái và hoàn tiền. DAO là Data Access Object, chịu trách nhiệm truy cập dữ liệu. Cách chia này giảm phụ thuộc và giúp nghiệp vụ dễ kiểm thử.

### Hình minh họa

Vẽ lại sơ đồ trên bằng các khối và mũi tên.

---

## Slide 6 - Room Database hoạt động như thế nào?

### Nội dung trên slide

```text
Entity mô tả bảng
       ↓
DAO khai báo truy vấn
       ↓
HomestayDatabase cấu hình Room
       ↓
Room sinh mã triển khai
       ↓
SQLite lưu dữ liệu thực tế
```

- `@Entity`: ánh xạ lớp Java thành bảng.
- `@Dao`: khai báo `@Query`, `@Insert`, `@Update`, `@Delete`.
- `@Database`: khai báo Entity, DAO và phiên bản schema.
- `@Transaction`: nhóm nhiều thao tác thành một giao dịch.
- Migration nâng schema mà không xóa dữ liệu cũ.
- Singleton giúp toàn ứng dụng dùng chung một database.

### Lời thuyết trình

Room không thay thế SQLite mà là lớp trung gian chính thức của Android Jetpack. SQLite vẫn lưu dữ liệu thực tế; Room kiểm tra truy vấn khi biên dịch, ánh xạ kết quả SQL thành đối tượng Java và sinh phần mã lặp lại. Vì RoomGo hoạt động ngoại tuyến và dữ liệu demo không quá lớn, Room/SQLite là lựa chọn phù hợp.

---

## Slide 7 - Thiết kế cơ sở dữ liệu

### Nội dung trên slide

RoomGo dùng database `homestay_database`, phiên bản 18, gồm 8 bảng:

| Bảng | Dữ liệu chính |
|---|---|
| `users` | Tài khoản, vai trò, trạng thái khóa |
| `rooms` | Tên, vị trí, giá, sức chứa, `maxSlots` |
| `room_images` | Nhiều ảnh của từng phòng |
| `slots` | Lựa chọn phòng và giá riêng |
| `bookings` | Ngày ở, khách, giá, trạng thái |
| `favorites` | Phòng yêu thích theo người dùng |
| `reviews` | Điểm và nhận xét sau lưu trú |
| `notifications` | Thông báo khách và admin |

**Quan hệ:** User tạo nhiều Booking; Room có nhiều Booking, ảnh, slot và đánh giá; Booking có tối đa một Review.

### Lời thuyết trình

`maxSlots` biểu diễn tổng số phòng cùng loại có thể bán. Số còn trống không lưu cố định mà được tính bằng `maxSlots` trừ số booking đang giữ chỗ và trùng ngày. Database có index cho các khóa tra cứu, unique index để hạn chế dữ liệu trùng và migration từ các phiên bản cũ.

### Hình minh họa

Sử dụng sơ đồ ER trong README.

---

## Slide 8 - Giao diện và chức năng khách hàng

### Nội dung trên slide

- **Xác thực:** đăng ký, đăng nhập, sửa hồ sơ, đổi mật khẩu.
- **Trang chủ:** tìm kiếm, lọc ngày, số khách và sắp xếp.
- **Chi tiết phòng:** ảnh, giá, tiện nghi, mô tả và đánh giá.
- **Yêu thích:** lưu hoặc bỏ lưu phòng.
- **Đặt chỗ:** chọn ngày, số khách, slot và phương thức thanh toán.
- **Booking:** xem trạng thái, chi tiết, hủy và tiền hoàn.
- **Thông báo:** trong ứng dụng và notification Android.
- **Đánh giá:** chỉ sau khi booking hoàn thành.

### Lời thuyết trình

Các danh sách dùng RecyclerView và Adapter. `RoomAdapter` hiển thị phòng, `BookingAdapter` hiển thị booking, `ReviewAdapter` hiển thị đánh giá và `NotificationAdapter` hiển thị thông báo. ViewPager2 cùng `RoomImagePagerAdapter` cho phép vuốt qua nhiều ảnh phòng.

### Hình minh họa

Trang chủ, chi tiết phòng, yêu thích và danh sách đặt chỗ.

---

## Slide 9 - Tìm kiếm và kiểm tra phòng trống

### Nội dung trên slide

```text
Nhập từ khóa / chọn bộ lọc
            ↓
TextWatcher gọi reload
            ↓
Repository đọc Room + Booking
            ↓
RoomSearchEngine.filter()
            ↓
RoomAdapter.submitList()
```

- Tìm không phân biệt hoa thường.
- So khớp chuỗi con trong tên, vị trí và địa chỉ.
- Loại phòng đang ẩn hoặc không đủ sức chứa.
- Nếu đã chọn ngày, loại phòng đã hết `maxSlots`.
- Sắp xếp theo giá tăng, giá giảm hoặc đánh giá.

### Lời thuyết trình

Phiên bản hiện tại lấy danh sách phòng và booking từ Room, sau đó lọc trong bộ nhớ Java bằng `RoomSearchEngine`. Booking `pending`, `confirmed` và `checked_in` được tính là chiếm chỗ nếu khoảng ngày giao nhau. Dữ liệu demo nhỏ nên cách này đơn giản; dữ liệu lớn nên dùng truy vấn DAO, phân trang và debounce.

---

## Slide 10 - Luồng đặt một phòng

### Nội dung trên slide

```text
Nhấn “Đặt ngay”
       ↓
Kiểm tra đăng nhập và tài khoản
       ↓
Chọn ngày, số khách, slot
       ↓
BookingRules kiểm tra điều kiện
       ↓
BookingCalculator tính tổng tiền
       ↓
Xác nhận thanh toán tại chỗ
       ↓
BookingRepository kiểm tra lại
       ↓
BookingDao ghi trong Transaction
```

**Quy tắc:** đặt trước ít nhất 1 giờ; ngày trả sau ngày nhận; không vượt sức chứa; còn phòng trong khoảng ngày; check-in 14:00, check-out 12:00.

### Lời thuyết trình

Giao diện kiểm tra sớm để phản hồi cho người dùng, nhưng Repository vẫn kiểm tra lại phòng, ngày, slot và giá. Giá được tính lại từ Room hoặc Slot thay vì tin trực tiếp giá gửi từ giao diện. Booking chỉ được lưu nếu DAO kiểm tra lần cuối rằng vẫn còn chỗ.

---

## Slide 11 - Tạo booking, thanh toán và cạnh tranh slot

### Nội dung trên slide

Booking mới được lưu với:

- `status = pending`.
- `paymentMethod = pay_on_site`.
- `paymentStatus = UNPAID`.
- `expiresAt = createdAt + 2 giờ`.

```text
Còn 1 slot
A và B cùng mở thanh toán
        ↓
Mở dialog chưa giữ phòng
        ↓
Cả hai cùng xác nhận
        ↓
Transaction A ghi trước → thành công
Transaction B đếm lại → hết phòng
```

### Lời thuyết trình

Màn hình thanh toán hiện chỉ xác nhận “thanh toán khi đến nơi”, chưa chuyển tiền thật. Slot chỉ được giữ khi booking `pending` được ghi thành công. Trong cùng một SQLite database, `insertIfCapacityAvailable()` vừa đếm booking trùng ngày vừa thêm booking trong `@Transaction`; yêu cầu đến sau nhận lỗi hết phòng.

**Giới hạn:** hai điện thoại có hai SQLite riêng nên chưa ngăn được đặt trùng giữa nhiều thiết bị. Sản phẩm thực tế cần API và database trung tâm.

---

## Slide 12 - Vòng đời booking, hủy và hoàn tiền

### Nội dung trên slide

```text
pending ──Admin duyệt──> confirmed
   │                         │
   ├──quá 2 giờ──> expired  ├──đến giờ nhận──> checked_in
   │                         │                     │
   └──hủy──> cancelled       └──đến giờ trả──────> completed
```

- Chỉ cho phép chuyển trạng thái hợp lệ.
- Hủy trước check-in từ 24 giờ: hoàn 100%.
- Hủy trong 24 giờ trước check-in: hoàn 50%.
- Hủy sau check-in: hoàn 0%.
- Booking và thanh toán có trạng thái độc lập.
- Có hoàn tiền: `paymentStatus = REFUND_PENDING`.

### Lời thuyết trình

`BookingStatusPolicy` kiểm soát hướng chuyển trạng thái. `CancellationPolicy` được dùng chung cho khách và admin để kết quả hoàn tiền nhất quán. Dữ liệu booking được giữ làm lịch sử, không xóa cứng trong thao tác quản trị thông thường.

---

## Slide 13 - Quản trị, tự động hóa và thông báo

### Nội dung trên slide

**Quản trị**

- Dashboard: phòng đang mở, người dùng, chờ duyệt và doanh thu.
- Lọc theo hôm nay, 7 ngày hoặc tháng.
- Quản lý phòng, nhiều ảnh và duy nhất một phòng nổi bật.
- Duyệt, từ chối, hủy booking và xác nhận hoàn tiền.
- Xem, sửa, đổi mật khẩu, khóa hoặc xóa tài khoản khách.

**Tự động hóa**

- WorkManager chạy khi mở ứng dụng và định kỳ 15 phút.
- `confirmed → checked_in` khi đến giờ nhận.
- `checked_in → completed` khi đến giờ trả.
- Đồng bộ và phát thông báo cho khách lẫn admin.

### Lời thuyết trình

Dashboard phân biệt doanh thu dự kiến với doanh thu thực thu dựa trên trạng thái thanh toán. Worker sử dụng các câu UPDATE có điều kiện nên có thể chạy lặp mà không chuyển sai trạng thái. Android 13 trở lên chỉ phát notification sau khi người dùng cấp quyền.

---

## Slide 14 - Bảo mật, kiểm thử và hạn chế

### Nội dung trên slide

**Bảo mật và toàn vẹn**

- BCrypt work factor 12; không lưu mật khẩu thuần.
- Kiểm tra email, số điện thoại và mật khẩu mạnh.
- Khóa 15 phút sau 5 lần đăng nhập sai.
- Phân quyền `ADMIN`/`CUSTOMER`.
- Unique index, foreign key, transaction và migration.

**Kiểm thử**

- 20 unit test: tìm kiếm, tính giá, booking, trạng thái, hoàn tiền.
- 3 instrumented test cho Room Database.
- `testDebugUnitTest`: **BUILD SUCCESSFUL**.

**Hạn chế**

- SQLite và SharedPreferences chưa được mã hóa.
- Chưa có backend, token, HTTPS và thanh toán thật.

### Lời thuyết trình

Các quy tắc nghiệp vụ được tách thành lớp Java thuần nên có thể kiểm thử nhanh mà không khởi chạy Android. Ba instrumented test cần emulator hoặc thiết bị. Bảo mật hiện phù hợp mức demo; thiết bị root hoặc APK bị chỉnh sửa vẫn có thể can thiệp dữ liệu cục bộ.

---

## Slide 15 - Kết quả, hướng phát triển và kết luận

### Nội dung trên slide

**Kết quả đạt được**

- Hoàn thành hai luồng khách hàng và quản trị.
- Quản lý đầy đủ vòng đời booking.
- Kiểm tra phòng trống theo khoảng ngày và transaction.
- Có thông báo, tác vụ nền, migration và kiểm thử.

**Hướng phát triển**

- Backend REST API và database trung tâm.
- Cơ chế giữ slot 5-10 phút khi thanh toán.
- VNPay/MoMo, bản đồ và lưu trữ ảnh trực tuyến.
- Mã hóa dữ liệu, HTTPS và token có thời hạn.
- MVVM, dependency injection, Paging và UI test.

**Kết luận:** RoomGo không chỉ là giao diện đặt phòng mà là hệ thống quản lý booking có quy tắc, trạng thái và dữ liệu nhất quán.

### Lời thuyết trình

Dự án đã đáp ứng mục tiêu của một ứng dụng Android demo hoàn chỉnh. Giá trị nổi bật nằm ở việc tách nghiệp vụ, kiểm tra lại dữ liệu trước khi ghi và quản lý vòng đời booking. Bước phát triển quan trọng nhất là chuyển quyền quyết định booking lên backend để hỗ trợ nhiều thiết bị và thanh toán thực tế.

**Xin cảm ơn thầy cô và các bạn đã lắng nghe!**

---

## Kịch bản demo đề xuất

1. Đăng nhập tài khoản khách.
2. Tìm phòng theo vị trí, chọn ngày và số khách.
3. Mở chi tiết, đặt phòng và xác nhận thanh toán tại chỗ.
4. Mở tab Đặt chỗ để xem booking `pending`.
5. Đăng nhập admin và xác nhận booking.
6. Quay lại giao diện khách để xem trạng thái và thông báo.

## Phân bổ thời gian

| Nhóm slide | Thời gian |
|---|---:|
| Slide 1-3: Bài toán và phạm vi | 2 phút |
| Slide 4-7: Công nghệ, kiến trúc, dữ liệu | 4 phút |
| Slide 8-13: Chức năng và nghiệp vụ | 6 phút |
| Slide 14-15: Đánh giá và kết luận | 2-3 phút |
