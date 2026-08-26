# NỘI DUNG THUYẾT TRÌNH DỰ ÁN ROOMGO

## Slide 1 - Giới thiệu đề tài

**ROOMGO - Ứng dụng tìm kiếm và đặt phòng homestay trên Android**

- Ngôn ngữ: Java; giao diện: XML.
- Dữ liệu: Room/SQLite.
- Hai đối tượng sử dụng: khách hàng và quản trị viên.

**Lời nói:** RoomGo mô phỏng đầy đủ quá trình tìm kiếm, đặt và quản lý phòng homestay. Dữ liệu của phiên bản hiện tại được lưu cục bộ trên thiết bị.

## Slide 2 - Lý do chọn đề tài

- Nhu cầu đặt chỗ lưu trú ngày càng phổ biến.
- Quản lý thủ công khó kiểm soát phòng trống và booking.
- Khách hàng cần quy trình tìm, xem và đặt phòng thuận tiện.
- Người quản lý cần theo dõi phòng, khách hàng và doanh thu tập trung.

## Slide 3 - Mục tiêu và phạm vi

- Xây dựng ứng dụng đặt homestay hoàn chỉnh trên Android.
- Hỗ trợ hai vai trò: `CUSTOMER` và `ADMIN`.
- Quản lý toàn bộ vòng đời booking.
- Đảm bảo quy tắc về giá, sức chứa, phòng trống và hoàn tiền.
- Phạm vi hiện tại: ứng dụng demo ngoại tuyến, chưa có backend.

## Slide 4 - Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 11 |
| Giao diện | XML, Material Components |
| Cơ sở dữ liệu | Room 2.6.1, SQLite |
| Danh sách và ảnh | RecyclerView, ViewPager2 |
| Tác vụ nền | WorkManager |
| Bảo mật mật khẩu | BCrypt |
| Android | minSdk 24, targetSdk 36 |

## Slide 5 - Kiến trúc hệ thống

```text
Activity, Adapter, XML
          ↓
Repository và Domain
          ↓
DAO
          ↓
Room Database / SQLite
```

- Dự án có 72 tệp Java và 35 layout XML.
- Mã nguồn chia thành `ui`, `data`, `domain`, `utils`, `worker`.
- Nghiệp vụ được tách khỏi giao diện để dễ kiểm thử.

## Slide 6 - Cơ sở dữ liệu

RoomGo có 8 bảng chính:

- `users`: tài khoản và vai trò.
- `rooms`, `room_images`, `slots`: phòng, ảnh và số lượng.
- `bookings`: thông tin đặt chỗ và thanh toán.
- `favorites`, `reviews`, `notifications`: tiện ích người dùng.

Database hiện ở phiên bản 18, có khóa ngoại, chỉ mục duy nhất và migration để giữ dữ liệu khi nâng cấp.

## Slide 7 - Chức năng khách hàng

- Đăng ký, đăng nhập, sửa hồ sơ và đổi mật khẩu.
- Tìm phòng theo tên, vị trí hoặc địa chỉ.
- Lọc theo ngày, số khách; sắp xếp theo giá hoặc đánh giá.
- Xem ảnh, mô tả, tiện nghi và nhận xét.
- Lưu phòng yêu thích.
- Đặt phòng, theo dõi hoặc hủy booking.
- Nhận thông báo và đánh giá sau lưu trú.

## Slide 8 - Chức năng quản trị viên

- Dashboard thống kê phòng, người dùng, booking và doanh thu.
- Quản lý phòng, hình ảnh, trạng thái mở và phòng nổi bật.
- Tìm kiếm, lọc, xác nhận, từ chối hoặc hủy booking.
- Xác nhận hoàn tiền.
- Xem, sửa, khóa hoặc xóa tài khoản khách.
- Nhận thông báo và chuyển sang giao diện khách.

## Slide 9 - Quy trình đặt phòng

```text
Chọn phòng và ngày
        ↓
Kiểm tra thời gian, sức chứa, phòng trống
        ↓
Chọn thanh toán → Chờ duyệt
        ↓
Đã xác nhận → Đang lưu trú → Hoàn thành → Đánh giá
```

- Đặt trước giờ nhận ít nhất một giờ.
- Nhận phòng 14:00, trả phòng 12:00.
- Kiểm tra phòng trống và tạo booking trong cùng transaction.
- Repository tính lại giá, không tin trực tiếp dữ liệu từ giao diện.

## Slide 10 - Trạng thái và hoàn tiền

- Trạng thái: chờ duyệt, đã xác nhận, đang lưu trú, hoàn thành, đã hủy, hết hạn.
- Chỉ cho phép các bước chuyển trạng thái hợp lệ.
- Booking chờ duyệt giữ phòng tối đa hai giờ.
- Hủy trước ít nhất 24 giờ: hoàn 100%.
- Hủy trong vòng 24 giờ trước check-in: hoàn 50%.
- Hủy sau check-in: không hoàn tiền.
- Trạng thái booking và thanh toán được lưu riêng.

## Slide 11 - Tự động hóa và thông báo

- WorkManager chạy khi mở ứng dụng và định kỳ khoảng 15 phút.
- Tự động chuyển sang đang lưu trú lúc 14:00 ngày nhận.
- Tự động hoàn thành sau 12:00 ngày trả.
- Tự động hết hạn booking chờ duyệt quá hai giờ.
- Tạo thông báo riêng cho khách và quản trị viên.
- Hỗ trợ thông báo hệ thống Android.

## Slide 12 - Bảo mật và toàn vẹn dữ liệu

- Mật khẩu được băm bằng BCrypt, work factor 12.
- Kiểm tra email, số điện thoại và mật khẩu mạnh.
- Khóa đăng nhập 15 phút sau năm lần nhập sai.
- Phân quyền `ADMIN` và `CUSTOMER`.
- Chỉ mục duy nhất hạn chế dữ liệu trùng.
- Booking lịch sử không bị xóa cứng trong luồng quản trị.
- Xóa dữ liệu phiên khi đăng xuất.

## Slide 13 - Kiểm thử và kết quả

- 20 unit test cho nghiệp vụ Java.
- 3 instrumented test cho Room Database.
- Kiểm thử tìm phòng, tính giá, quy tắc booking, trạng thái và hoàn tiền.
- Đã chạy `testDebugUnitTest`: **BUILD SUCCESSFUL**.
- Instrumented test cần chạy trên máy ảo hoặc thiết bị Android.

## Slide 14 - Hạn chế và hướng phát triển

- Xây dựng REST API và đồng bộ nhiều thiết bị.
- Tích hợp cổng thanh toán, bản đồ và lưu trữ ảnh trực tuyến.
- Mã hóa database và thông tin phiên.
- Dùng HTTPS, token có thời hạn và phân quyền phía máy chủ.
- Hoàn thiện MVVM, dependency injection và UI test.
- Bổ sung khuyến mãi, chat hỗ trợ và đa ngôn ngữ.

## Slide 15 - Kết luận

- RoomGo mô phỏng trọn vẹn hệ thống đặt homestay trên Android.
- Kết hợp giao diện, Room Database, nghiệp vụ và tác vụ nền.
- Điểm nổi bật: hai vai trò, kiểm tra phòng theo ngày, quản lý trạng thái, hoàn tiền và thông báo.
- Là nền tảng phù hợp để phát triển thành ứng dụng client-server.

**Lời kết:** RoomGo không chỉ là giao diện đặt phòng mà còn là hệ thống quản lý booking có quy tắc, trạng thái và dữ liệu nhất quán.

**Xin cảm ơn thầy cô và các bạn đã lắng nghe!**

## Gợi ý trình bày

- Thời lượng: 10-12 phút, khoảng 40-50 giây mỗi slide.
- Dùng ảnh chụp ứng dụng tại slide 1, 7, 8 và 9.
- Slide 5 dùng sơ đồ kiến trúc; slide 6 dùng sơ đồ quan hệ dữ liệu.
- Khi demo: tạo booking bằng tài khoản khách, sau đó đăng nhập admin để duyệt.
