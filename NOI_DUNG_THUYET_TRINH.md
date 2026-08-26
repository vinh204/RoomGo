# THUYẾT TRÌNH DỰ ÁN ROOMGO - TẬP TRUNG CHỨC NĂNG CHÍNH

> Bộ nội dung gồm 15 slide, phù hợp thuyết trình trong 12-15 phút. Phần **Nội dung trên slide** dùng cho PowerPoint; phần **Lời thuyết trình** dùng làm ghi chú.

---

## Slide 1 - Giới thiệu RoomGo

### Nội dung trên slide

**ROOMGO - Ứng dụng tìm kiếm và đặt phòng homestay**

- Ứng dụng Android viết bằng Java.
- Hỗ trợ khách hàng và quản trị viên.
- Quản lý từ lúc tìm phòng đến khi hoàn thành lưu trú.
- Dữ liệu demo được lưu cục bộ bằng Room/SQLite.

### Lời thuyết trình

RoomGo là ứng dụng mô phỏng toàn bộ quy trình đặt homestay trên Android. Khách hàng có thể tìm kiếm, xem chi tiết, đặt và theo dõi phòng. Quản trị viên có thể quản lý phòng, booking, người dùng và doanh thu.

### Hình minh họa

Logo RoomGo, màn hình Splash hoặc đăng nhập.

---

## Slide 2 - Người dùng và nhóm chức năng

### Nội dung trên slide

| Khách hàng | Quản trị viên |
|---|---|
| Đăng ký, đăng nhập | Xem dashboard |
| Tìm kiếm và lọc phòng | Quản lý phòng |
| Xem chi tiết, yêu thích | Xử lý booking |
| Đặt và hủy phòng | Quản lý người dùng |
| Theo dõi trạng thái | Theo dõi doanh thu |
| Nhận thông báo, đánh giá | Nhận thông báo quản trị |

### Lời thuyết trình

Hệ thống phân biệt hai vai trò là `CUSTOMER` và `ADMIN`. Mỗi vai trò có giao diện và quyền thao tác riêng. Admin còn có thể chuyển sang giao diện khách để kiểm tra trải nghiệm sử dụng.

---

## Slide 3 - Đăng ký, đăng nhập và quản lý tài khoản

### Nội dung trên slide

- Đăng ký bằng họ tên, email, số điện thoại và mật khẩu.
- Kiểm tra định dạng và không cho trùng email, số điện thoại.
- Mật khẩu phải có ít nhất 8 ký tự và đủ độ mạnh.
- Mật khẩu được băm bằng BCrypt trước khi lưu.
- Khóa đăng nhập 15 phút sau 5 lần nhập sai.
- Khách có thể sửa hồ sơ, đổi mật khẩu và đăng xuất.
- Admin có thể khóa hoặc mở khóa tài khoản khách.

### Lời thuyết trình

Khi đăng nhập thành công, SessionManager lưu phiên người dùng và điều hướng theo vai trò. Nếu tài khoản bị admin khóa, ứng dụng kiểm tra lại trạng thái trong quá trình sử dụng. Khi đăng xuất, dữ liệu phiên được xóa để không thể quay lại màn hình đã đăng nhập.

### Hình minh họa

Màn hình đăng ký, đăng nhập và tài khoản.

---

## Slide 4 - Trang chủ và tìm kiếm phòng

### Nội dung trên slide

- Hiển thị lời chào, phòng nổi bật và danh sách phòng.
- Tìm theo tên phòng, vị trí hoặc địa chỉ.
- Tìm kiếm không phân biệt chữ hoa và chữ thường.
- Lọc theo ngày nhận, ngày trả và số khách.
- Sắp xếp theo giá thấp, giá cao hoặc đánh giá.
- Chỉ hiển thị phòng đang mở, đủ sức chứa và còn chỗ.

### Luồng xử lý

```text
Nhập từ khóa / chọn bộ lọc
            ↓
Đọc danh sách phòng và booking
            ↓
RoomSearchEngine lọc kết quả
            ↓
RoomAdapter cập nhật RecyclerView
```

### Lời thuyết trình

Khi người dùng nhập từ khóa, TextWatcher gọi lại chức năng tìm kiếm. RoomSearchEngine ghép tên, vị trí và địa chỉ rồi kiểm tra chuỗi con. Nếu người dùng đã chọn ngày, hệ thống còn loại những phòng đã hết số lượng trong khoảng thời gian đó.

### Hình minh họa

Trang chủ có ô tìm kiếm, bộ lọc và danh sách kết quả.

---

## Slide 5 - Xem chi tiết phòng

### Nội dung trên slide

- Trình chiếu nhiều ảnh bằng ViewPager2.
- Hiển thị tên, địa chỉ, vị trí và mô tả.
- Hiển thị giá mỗi đêm và số phòng còn lại.
- Hiển thị sức chứa và các tiện nghi.
- Hiển thị điểm trung bình và đánh giá của khách trước.
- Cho phép lưu phòng yêu thích hoặc bắt đầu đặt phòng.

### Lời thuyết trình

Màn hình chi tiết nhận `room_id` thông qua Intent, sau đó lấy dữ liệu phòng, ảnh, slot và đánh giá từ Repository. Các ảnh được hiển thị bằng RoomImagePagerAdapter. Đây là màn hình giúp khách ra quyết định trước khi đặt.

### Hình minh họa

Ảnh màn hình chi tiết phòng, bộ ảnh và phần đánh giá.

---

## Slide 6 - Phòng yêu thích

### Nội dung trên slide

- Khách nhấn biểu tượng trái tim để lưu phòng.
- Mỗi quan hệ yêu thích gắn với một người dùng và một phòng.
- Có thể bỏ lưu ngay tại danh sách hoặc màn hình chi tiết.
- Tab “Đã lưu” hiển thị toàn bộ phòng yêu thích.
- Trạng thái yêu thích được đồng bộ khi tải lại danh sách.
- Người chưa đăng nhập được yêu cầu đăng nhập trước khi lưu.

### Lời thuyết trình

Chức năng yêu thích dùng bảng `favorites`. Cặp `userId-roomId` giúp xác định đúng phòng của từng người dùng và tránh lưu trùng. Danh sách được hiển thị bằng RecyclerView và RoomAdapter giống Trang chủ.

---

## Slide 7 - Chọn ngày, số khách và loại phòng

### Nội dung trên slide

- Chọn ngày nhận và ngày trả bằng bộ chọn ngày.
- Ngày nhận phải cách thời điểm đặt ít nhất một giờ.
- Ngày trả phải sau ngày nhận.
- Quy ước nhận phòng lúc 14:00, trả phòng lúc 12:00.
- Số khách không được vượt sức chứa tối đa.
- Có thể chọn slot hoặc loại phòng với mức giá riêng.
- Tổng tiền cập nhật theo lựa chọn của khách.

### Công thức

```text
Số đêm = ngày trả - ngày nhận
Giá mỗi đêm = giá slot nếu có, ngược lại dùng giá phòng
Tổng tiền = số đêm × giá mỗi đêm
```

### Lời thuyết trình

`BookingRules` kiểm tra ngày, số khách và sức chứa. `BookingCalculator` tính số đêm và tổng tiền. Việc tách hai lớp này khỏi Activity giúp quy tắc được dùng thống nhất và có thể kiểm thử độc lập.

---

## Slide 8 - Kiểm tra phòng trống và tạo booking

### Nội dung trên slide

- Đếm booking cùng phòng có khoảng ngày giao nhau.
- Chỉ tính các trạng thái đang giữ chỗ:
  - `pending`
  - `confirmed`
  - `checked_in`
- Số còn trống = `maxSlots - occupied`.
- Repository kiểm tra lại phòng, ngày, slot và giá.
- Kiểm tra số lượng và ghi booking trong cùng `@Transaction`.

### Luồng xử lý

```text
Kiểm tra dữ liệu ở giao diện
             ↓
Repository tính lại giá và quy tắc
             ↓
DAO đếm booking trùng ngày
             ↓
Còn phòng → ghi booking
Hết phòng → trả thông báo lỗi
```

### Lời thuyết trình

Hệ thống không chỉ tin kết quả hiển thị trên màn hình. Ngay trước khi lưu, Repository và DAO kiểm tra lại dữ liệu. Việc đếm và thêm booking trong cùng transaction giúp hạn chế tạo booking vượt quá `maxSlots`.

---

## Slide 9 - Thanh toán và xử lý hai người cùng đặt

### Nội dung trên slide

**Thanh toán hiện tại**

- Hiển thị lại phòng, ngày, số khách và tổng tiền.
- Phương thức: thanh toán khi đến nơi, `pay_on_site`.
- Booking mới có `paymentStatus = UNPAID`.
- Chưa tích hợp giao dịch tiền thật.

**Khi chỉ còn một slot**

```text
A và B cùng mở thanh toán
        ↓
Mở dialog chưa giữ phòng
        ↓
A xác nhận và ghi trước → thành công
B được kiểm tra lại → báo hết phòng
```

### Lời thuyết trình

Slot chỉ được giữ khi booking được ghi thành công, không phải lúc mở màn hình thanh toán. Trong cùng một SQLite database, transaction bảo đảm yêu cầu ghi trước giữ chỗ và yêu cầu sau bị từ chối. Tuy nhiên, hai điện thoại dùng hai database riêng nên sản phẩm thực tế cần backend và database trung tâm.

---

## Slide 10 - Theo dõi vòng đời booking

### Nội dung trên slide

```text
pending ──Admin xác nhận──> confirmed
   │                           │
   ├──quá 2 giờ──> expired    ├──đến giờ nhận──> checked_in
   │                           │                     │
   └──hủy──> cancelled         └──đến giờ trả──────> completed
```

- Khách theo dõi booking trong tab “Đặt chỗ”.
- Hiển thị mã, phòng, ngày, tổng tiền và trạng thái.
- Booking chờ duyệt giữ phòng tối đa hai giờ.
- Chỉ cho phép các bước chuyển trạng thái hợp lệ.
- Booking lịch sử được giữ lại để tra cứu.

### Lời thuyết trình

`BookingStatusPolicy` ngăn các bước chuyển sai, ví dụ booking hoàn thành không thể quay về chờ duyệt. Trạng thái booking được tách khỏi trạng thái thanh toán vì hoàn thành kỳ lưu trú không đồng nghĩa với đã thu tiền.

### Hình minh họa

Danh sách booking và dialog chi tiết booking.

---

## Slide 11 - Hủy phòng và hoàn tiền

### Nội dung trên slide

- Chỉ booking `pending` hoặc `confirmed` mới được hủy trước check-in.
- Khách nhập lý do hủy.
- Hệ thống tự tính tiền hoàn:

| Thời điểm hủy | Tỷ lệ hoàn |
|---|---:|
| Trước check-in từ 24 giờ | 100% |
| Trong vòng 24 giờ trước check-in | 50% |
| Sau giờ check-in | 0% |

- Có tiền hoàn: chuyển sang `REFUND_PENDING`.
- Admin xác nhận sau khi đã hoàn tiền.

### Lời thuyết trình

`CancellationPolicy` được dùng chung cho khách và quản trị viên nên cùng một booking luôn có kết quả hoàn tiền giống nhau. Lý do hủy, thời điểm hủy và số tiền hoàn đều được lưu trong booking để tra cứu.

---

## Slide 12 - Thông báo và tác vụ tự động

### Nội dung trên slide

**Thông báo cho khách**

- Đã gửi yêu cầu đặt phòng.
- Booking được xác nhận, bị hủy hoặc hết hạn.
- Đến giờ nhận phòng.
- Chuyến đi hoàn thành và mời đánh giá.

**Thông báo cho admin**

- Có tài khoản mới.
- Có yêu cầu đặt phòng mới.
- Khách hủy booking.
- Khách bắt đầu hoặc hoàn thành lưu trú.

**Tác vụ nền**

- WorkManager chạy khi mở ứng dụng và định kỳ 15 phút.
- Tự động cập nhật `expired`, `checked_in`, `completed`.

### Lời thuyết trình

Thông báo được lưu trong bảng `notifications`, có khóa sự kiện duy nhất để tránh tạo trùng. Người dùng có thể đánh dấu từng thông báo hoặc tất cả là đã đọc. SystemNotificationHelper đưa thông báo mới lên thanh trạng thái Android nếu đã được cấp quyền.

### Hình minh họa

Danh sách thông báo và notification trên Android.

---

## Slide 13 - Đánh giá sau lưu trú

### Nội dung trên slide

- Chỉ booking `completed` mới được đánh giá.
- Mỗi booking chỉ được gửi một đánh giá.
- Khách chọn số sao và nhập nhận xét.
- Đánh giá liên kết với người dùng, phòng và booking.
- Điểm đánh giá được hiển thị ở danh sách và chi tiết phòng.
- Đánh giá hỗ trợ khách sau đưa ra quyết định.

### Lời thuyết trình

Điều kiện booking hoàn thành giúp hạn chế đánh giá từ người chưa thực sự lưu trú. ReviewAdapter hiển thị danh sách đánh giá trên màn hình chi tiết. Đây cũng là dữ liệu được dùng khi sắp xếp phòng theo đánh giá tốt.

---

## Slide 14 - Các chức năng quản trị

### Nội dung trên slide

**Dashboard**

- Phòng đang mở, tổng người dùng, booking chờ duyệt.
- Doanh thu dự kiến và doanh thu thực thu.
- Thống kê hôm nay, 7 ngày hoặc tháng; so sánh kỳ trước.

**Quản lý**

- Phòng: thêm, sửa, ẩn, xóa, nhiều ảnh, phòng nổi bật.
- Booking: tìm, lọc, xác nhận, từ chối, hủy, hoàn tiền.
- Người dùng: xem, sửa, đổi mật khẩu, khóa hoặc xóa.
- Thông báo: mở nhanh màn hình có sự kiện liên quan.

### Lời thuyết trình

Admin có bốn khu vực chính: tổng quan, phòng, đặt chỗ và người dùng. Chỉ một phòng được đánh dấu nổi bật tại một thời điểm. Booking đang lưu trú hoặc đã kết thúc không cho thay đổi thủ công tùy ý để bảo vệ lịch sử.

### Hình minh họa

Dashboard, quản lý phòng, booking và người dùng.

---

## Slide 15 - Kết quả, hạn chế và hướng phát triển

### Nội dung trên slide

**Kết quả**

- Hoàn thành đầy đủ chức năng cho khách và admin.
- Quản lý tìm kiếm, đặt phòng, trạng thái và hoàn tiền.
- Có transaction, tác vụ nền, thông báo và đánh giá.
- 20 unit test đã chạy thành công; có 3 database test.

**Hạn chế**

- Dữ liệu chỉ nằm trên từng thiết bị.
- Thanh toán mới là mô phỏng tại chỗ.
- Chưa đồng bộ và chống đặt trùng giữa nhiều điện thoại.

**Hướng phát triển**

- Backend và database trung tâm.
- Giữ slot tạm thời khi thanh toán.
- VNPay/MoMo, bản đồ và lưu ảnh trực tuyến.
- HTTPS, token và mã hóa dữ liệu.

### Lời thuyết trình

RoomGo đã hoàn thành mục tiêu của một ứng dụng Android demo có nghiệp vụ rõ ràng. Điểm nổi bật là kiểm tra phòng theo khoảng ngày, quản lý vòng đời booking và thông báo tự động. Bước phát triển quan trọng nhất là xây dựng backend để nhiều thiết bị sử dụng chung dữ liệu và tích hợp thanh toán thật.

**Xin cảm ơn thầy cô và các bạn đã lắng nghe!**

---

## Kịch bản demo chức năng

1. Đăng nhập tài khoản khách.
2. Tìm phòng theo địa điểm, chọn ngày và số khách.
3. Mở chi tiết, lưu yêu thích và đặt phòng.
4. Xác nhận thanh toán tại chỗ.
5. Mở tab Đặt chỗ để xem trạng thái chờ duyệt.
6. Đăng nhập admin và xác nhận booking.
7. Quay lại tài khoản khách để xem trạng thái và thông báo.

## Phân bổ thời gian

| Nội dung | Slide | Thời gian |
|---|---:|---:|
| Giới thiệu và người dùng | 1-2 | 1,5 phút |
| Chức năng khách hàng | 3-7 | 4,5 phút |
| Đặt phòng và vòng đời | 8-11 | 4 phút |
| Thông báo, đánh giá, admin | 12-14 | 3 phút |
| Kết luận | 15 | 1 phút |
