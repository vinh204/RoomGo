# RoomGo

RoomGo là ứng dụng Android viết bằng **Java**, mô phỏng quy trình tìm kiếm và đặt phòng homestay. Ứng dụng có hai giao diện dành cho khách hàng và quản trị viên; toàn bộ dữ liệu demo được lưu cục bộ bằng **Room/SQLite**.

## Chức năng

### Khách hàng

- Đăng ký, đăng nhập và chỉnh sửa thông tin cá nhân.
- Tìm kiếm, lọc và xem danh sách phòng đang mở.
- Xem chi tiết, tiện nghi và nhiều ảnh phòng dạng slide.
- Lưu hoặc bỏ lưu phòng yêu thích.
- Chọn ngày nhận/trả phòng, số khách và phương thức thanh toán.
- Theo dõi trạng thái đặt chỗ: chờ duyệt, đã xác nhận, đang lưu trú, hoàn thành, hết hạn hoặc đã hủy.
- Xem số lượng còn trống dạng **Còn X/Y phòng**, được tính theo khoảng ngày đã chọn.
- Hủy đặt phòng kèm lý do và thông tin hoàn tiền.
- Đánh giá phòng sau khi hoàn thành lưu trú.
- Nhận thông báo trong ứng dụng và trên thanh thông báo Android.

### Quản trị viên

- Xem tổng quan phòng, người dùng, booking và doanh thu.
- Quản lý phòng: thêm, sửa, ẩn, xóa, chọn nhiều ảnh và đánh dấu phòng nổi bật.
- Chỉ cho phép một phòng được đánh dấu nổi bật tại một thời điểm.
- Tìm kiếm, lọc và xử lý booking theo trạng thái: xác nhận hoặc từ chối yêu cầu, hủy booking và xác nhận hoàn tiền.
- Xem, chỉnh sửa, khóa hoặc xóa tài khoản khách hàng.
- Nhận thông báo riêng khi có hoạt động mới.
- Chuyển sang giao diện khách mà vẫn sử dụng được các chức năng khách hàng.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 11 |
| Giao diện | XML, Material Components |
| Cơ sở dữ liệu | Android Room 2.6.1 / SQLite |
| Kiến trúc dữ liệu | DAO, Repository, Entity, Model |
| Danh sách | RecyclerView |
| Slide ảnh | ViewPager2 |
| Mật khẩu demo | jBCrypt |
| Build system | Gradle 8.13, Android Gradle Plugin 8.13.1 |
| Android SDK | compileSdk/targetSdk 36, minSdk 24 |
| Tác vụ nền | AndroidX WorkManager |

## Cơ chế an toàn và toàn vẹn dữ liệu

### Tài khoản và đăng nhập

- Mật khẩu không lưu dạng văn bản thuần. Ứng dụng băm mật khẩu bằng **BCrypt** với work factor 12 trước khi ghi vào SQLite.
- Mật khẩu mới phải dài tối thiểu 8 ký tự và có chữ hoa, chữ thường, chữ số cùng ký tự đặc biệt.
- Email, số điện thoại và họ tên được kiểm tra định dạng; số điện thoại được chuẩn hóa trước khi lưu.
- Sau 5 lần đăng nhập sai liên tiếp, tài khoản bị khóa tạm thời 15 phút. Đăng nhập thành công sẽ xóa bộ đếm thất bại.
- Quản trị viên có thể khóa hoặc mở khóa tài khoản khách. Trạng thái khóa được lưu trong bảng `users` và được kiểm tra lại khi người dùng đang sử dụng ứng dụng.
- Vai trò `ADMIN` và `CUSTOMER` được lưu riêng; email quản trị hệ thống không thể bị chuyển thành tài khoản khách thông qua luồng chỉnh sửa thông thường.
- Khi đăng xuất, dữ liệu phiên khách và phiên quản trị trong `SharedPreferences` được xóa.

### Đặt phòng và thanh toán

- Giá booking được tính lại từ dữ liệu phòng/slot trong repository, không tin trực tiếp giá trị hiển thị từ giao diện.
- Ngày nhận phòng phải được đặt trước ít nhất 1 giờ; ngày trả phải sau ngày nhận; số khách không được vượt sức chứa.
- Kiểm tra số phòng còn trống và ghi booking được thực hiện trong cùng một `@Transaction`, hạn chế tạo booking vượt quá `maxSlots` khi nhiều thao tác xảy ra gần nhau.
- Booking chờ duyệt giữ phòng tối đa 2 giờ rồi chuyển sang `expired`, giải phóng số lượng phòng.
- `BookingStatusPolicy` chỉ cho phép các bước chuyển trạng thái hợp lệ; booking đang lưu trú chỉ có thể đi đến hoàn thành.
- Chính sách hủy được dùng chung cho khách và quản trị: hoàn 100% trước giờ nhận ít nhất 24 giờ, 50% trong vòng 24 giờ và 0% sau giờ nhận.
- Trạng thái booking và trạng thái thanh toán được lưu riêng (`status`, `paymentStatus`) để booking hoàn thành không tự động đồng nghĩa với đã thu tiền.
- Booking là dữ liệu lịch sử và không bị xóa cứng trong luồng quản trị thông thường.

### SQLite, thông báo và quyền hệ thống

- Email, số điện thoại và khóa sự kiện thông báo có unique index để hạn chế dữ liệu trùng.
- Room Database sử dụng migration từ các phiên bản cũ đến phiên bản 18; dự án không dùng `fallbackToDestructiveMigration`, tránh âm thầm xóa dữ liệu khi nâng schema.
- Tác vụ WorkManager và các câu lệnh cập nhật có điều kiện được thiết kế để chạy lặp lại mà không chuyển sai trạng thái booking.
- Thông báo khách và quản trị được phân biệt theo loại sự kiện và tài khoản nhận.
- Trên Android 13 trở lên, ứng dụng chỉ gửi thông báo hệ thống sau khi người dùng cấp quyền `POST_NOTIFICATIONS`.

### Giới hạn của bản cục bộ

- Database SQLite hiện chưa được mã hóa bằng SQLCipher.
- Session và bộ đếm đăng nhập được lưu bằng `SharedPreferences` thường, chưa dùng `EncryptedSharedPreferences`.
- Xác thực và phân quyền chạy trên thiết bị, chưa có backend làm nguồn dữ liệu tin cậy. Thiết bị đã root hoặc bản APK bị chỉnh sửa vẫn có thể can thiệp dữ liệu cục bộ.
- Tài khoản demo không nên được sử dụng cho dữ liệu thật. Khi triển khai thực tế cần backend, token có thời hạn, HTTPS, quản lý secret và kiểm tra quyền ở phía máy chủ.

## Yêu cầu môi trường

- Android Studio phiên bản mới, có hỗ trợ Android SDK 36.
- JDK 17 để chạy Gradle/Android Gradle Plugin. Mã nguồn ứng dụng biên dịch ở mức Java 11.
- Android SDK Platform 36 và Android SDK Build-Tools.
- Thiết bị thật hoặc máy ảo Android 7.0 (API 24) trở lên.
- Git nếu tải dự án từ GitHub.
- Internet trong lần build đầu tiên để Gradle tải dependency.

## Cài đặt dự án

### 1. Tải mã nguồn

```bash
git clone https://github.com/vinh204/RoomGo.git
cd RoomGo
```

Hoặc tải file ZIP từ GitHub, giải nén rồi mở thư mục `RoomGo`.

### 2. Mở bằng Android Studio

1. Chọn **Open** trong Android Studio.
2. Chọn thư mục gốc `RoomGo` có file `settings.gradle`.
3. Chờ Android Studio hoàn tất **Gradle Sync**.
4. Nếu được hỏi JDK, chọn **Gradle JDK 17** hoặc JDK đi kèm Android Studio.
5. Mở **SDK Manager** và cài Android SDK Platform 36 nếu máy chưa có.

File `local.properties` chứa đường dẫn Android SDK được Android Studio tự tạo theo máy. Không sao chép file này từ máy khác và không commit lên Git.

### 3. Tạo thiết bị chạy thử

- Thiết bị thật: bật **Developer options** và **USB debugging**, sau đó kết nối bằng USB.
- Máy ảo: mở **Device Manager**, tạo Android Virtual Device API 24 trở lên. Nên dùng API 35 hoặc 36 để kiểm tra quyền thông báo.

### 4. Chạy ứng dụng

Chọn thiết bị trên thanh công cụ Android Studio rồi nhấn **Run app**.

Build bằng terminal tại thư mục dự án:

```powershell
# Windows
.\gradlew.bat assembleDebug
```

```bash
# macOS/Linux
./gradlew assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Cài APK qua ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tài khoản quản trị mặc định

```text
Email:    admin@gmail.com
Mật khẩu: Admin@123
```

Ở lần đăng nhập đầu tiên, ứng dụng tạo tài khoản quản trị cục bộ nếu tài khoản này chưa tồn tại. Quản trị viên có thể đổi mật khẩu trong ứng dụng.

> Không sử dụng thông tin đăng nhập hard-code theo cách này trong ứng dụng thực tế. Production cần backend, xác thực an toàn và quản lý secret phù hợp.

## Tài khoản khách mẫu

Ở lần cài mới, ứng dụng tự tạo:

```text
Email:    demo@roomgo.vn
Mật khẩu: Demo@123
```

Tài khoản này có các booking mẫu ở trạng thái chờ duyệt, đã xác nhận, đang lưu trú, hoàn thành và đã hủy.

## Hướng dẫn sử dụng

### Luồng khách hàng

1. Mở ứng dụng và chọn **Đăng ký ngay** nếu chưa có tài khoản.
2. Nhập họ tên, email, số điện thoại và mật khẩu.
3. Tại **Trang chủ**, tìm phòng hoặc chọn phòng nổi bật.
4. Mở chi tiết để lướt ảnh, xem mô tả, tiện nghi và đánh giá.
5. Nhấn **Đặt ngay**, chọn ngày, số khách và phương thức thanh toán.
6. Mở tab **Đặt chỗ** để theo dõi hoặc hủy yêu cầu.
7. Booking tự chuyển sang **Đang lưu trú** từ 14:00 ngày nhận phòng và sang **Hoàn thành** sau 12:00 ngày trả phòng.
8. Khi booking đã hoàn thành, mở booking để gửi đánh giá.
9. Mở tab **Tài khoản** để chỉnh hồ sơ, đổi mật khẩu hoặc đăng xuất.

Giờ quy ước:

- Nhận phòng: **14:00**.
- Trả phòng: **12:00**.

### Luồng quản trị viên

1. Đăng nhập bằng tài khoản quản trị mặc định.
2. Tab **Tổng quan** hiển thị số phòng, người dùng, booking chờ duyệt, doanh thu và các booking gần nhất.
3. Tab **Phòng** dùng để thêm/sửa phòng, quản lý ảnh, trạng thái mở và phòng nổi bật.
4. Tab **Đặt chỗ** dùng để tìm và xử lý booking. Booking chờ duyệt có hai lựa chọn **Xác nhận/Từ chối**; booking đã xác nhận có thể **Hủy booking**; booking đang lưu trú hoặc đã kết thúc không cho thay đổi thủ công.
5. Tab **Người dùng** dùng để xem chi tiết, sửa thông tin, đổi mật khẩu hoặc khóa khách hàng.
6. Nhấn avatar quản trị để mở menu **Xem giao diện khách** hoặc **Đăng xuất**.
7. Khi đang ở giao diện khách, mở tab **Tài khoản** để quay lại trang quản trị.

## Dữ liệu SQLite

RoomGo sử dụng database `homestay_database`, gồm các bảng chính:

- `rooms`: thông tin phòng.
- `room_images`: danh sách ảnh của từng phòng.
- `slots`: số lượng phòng còn có thể đặt.
- `bookings`: dữ liệu đặt chỗ.
- `users`: tài khoản người dùng.
- `favorites`: phòng yêu thích.
- `reviews`: đánh giá phòng.
- `notifications`: thông báo khách và quản trị.

### Sơ đồ quan hệ dữ liệu

```mermaid
erDiagram
    USERS ||--o{ BOOKINGS : creates
    USERS ||--o{ FAVORITES : saves
    USERS ||--o{ REVIEWS : writes
    USERS ||--o{ NOTIFICATIONS : receives
    ROOMS ||--o{ BOOKINGS : reserved_for
    ROOMS ||--o{ SLOTS : contains
    ROOMS ||--o{ ROOM_IMAGES : has
    ROOMS ||--o{ FAVORITES : saved_as
    ROOMS ||--o{ REVIEWS : receives
    BOOKINGS ||--o| REVIEWS : reviewed_after

    USERS {
        long id PK
        string email UK
        string phone UK
        string role
        boolean locked
    }
    ROOMS {
        long id PK
        string name
        double price
        int maxSlots
        boolean available
    }
    BOOKINGS {
        long id PK
        long userId FK
        long roomId FK
        long checkInDate
        long checkOutDate
        string status
        string paymentStatus
    }
    ROOM_IMAGES {
        long id PK
        long roomId FK
        string imageUri
    }
```

`maxSlots` là tổng số phòng cùng loại có thể bán. Số còn trống được tính bằng `maxSlots` trừ các booking chờ duyệt, đã xác nhận hoặc đang lưu trú bị trùng khoảng ngày.

### Luồng đặt phòng

```mermaid
flowchart LR
    A[Chọn phòng và ngày] --> B[Kiểm tra sức chứa và số phòng trống]
    B -->|Còn phòng| C[Chọn thanh toán]
    B -->|Hết phòng| X[Dừng và báo hết phòng]
    C --> D[Chờ duyệt]
    D -->|Admin duyệt| E[Đã xác nhận]
    D -->|Quá hạn giữ phòng| Y[Hết hạn]
    D -->|Hủy| Z[Đã hủy]
    E -->|14:00 ngày nhận| F[Đang lưu trú]
    E -->|Hủy trước nhận phòng| Z
    F -->|12:00 ngày trả| G[Hoàn thành]
    G --> H[Khách đánh giá]
```

WorkManager kiểm tra vòng đời booking khi mở ứng dụng và định kỳ khoảng 15 phút. Android có thể trì hoãn tác vụ nền để tiết kiệm pin, vì vậy đây là cơ chế phù hợp cho bản demo chứ không thay thế backend trong sản phẩm thực tế.

Vị trí database trên thiết bị/emulator:

```text
/data/data/com.example.homestay/databases/homestay_database
```

Xem database trong Android Studio:

1. Chạy ứng dụng trên emulator hoặc thiết bị debuggable.
2. Chọn **View > Tool Windows > App Inspection**.
3. Mở **Database Inspector**.
4. Chọn tiến trình `com.example.homestay` và database `homestay_database`.

Database hiện ở phiên bản 18. Phiên bản này lưu thêm vai trò tài khoản, trạng thái thanh toán và thời hạn giữ phòng. Khi thay đổi Entity, cần tăng version và thêm `Migration` tương ứng trong `HomestayDatabase` để giữ dữ liệu cũ.

## Cấu trúc mã nguồn

```text
RoomGo/
├── app/src/main/
│   ├── java/com/example/homestay/
│   │   ├── HomestayApplication.java
│   │   ├── data/
│   │   │   ├── dao/             Truy vấn Room/SQLite
│   │   │   ├── database/        Database và migration
│   │   │   ├── entity/          Định nghĩa các bảng
│   │   │   ├── model/           Model hiển thị và dữ liệu kết hợp
│   │   │   └── repository/      Nguồn và nghiệp vụ dữ liệu
│   │   ├── domain/              Quy tắc đặt phòng, tìm kiếm, tính tiền
│   │   ├── ui/
│   │   │   ├── auth/            Splash, đăng nhập, đăng ký
│   │   │   ├── customer/        Trang khách và thông báo khách
│   │   │   ├── room/            Chi tiết và đặt phòng
│   │   │   ├── admin/           Các màn hình quản trị
│   │   │   └── adapter/         Adapter dùng chung
│   │   ├── utils/               Tiện ích, session và formatter
│   │   └── worker/              Tự động cập nhật vòng đời booking
│   ├── res/                      Layout, drawable, menu, màu và theme
│   └── AndroidManifest.xml
├── gradle/                       Gradle wrapper và version catalog
├── app/build.gradle              Cấu hình module ứng dụng
├── build.gradle                  Cấu hình project
└── settings.gradle               Khai báo project/module
```

## Kiểm tra dự án

```powershell
# Unit test và build APK
.\gradlew.bat testDebugUnitTest assembleDebug

# Android Lint
.\gradlew.bat lintDebug
```

Trên macOS/Linux, thay `.\gradlew.bat` bằng `./gradlew`.

Nếu máy có ít RAM:

```powershell
.\gradlew.bat --no-daemon "-Dorg.gradle.jvmargs=-Xmx768m -XX:+UseSerialGC -Dfile.encoding=UTF-8" assembleDebug
```

## Xử lý lỗi thường gặp

### Gradle Sync thất bại

- Kiểm tra kết nối Internet.
- Chọn Gradle JDK 17 tại **Settings > Build, Execution, Deployment > Build Tools > Gradle**.
- Cài Android SDK 36 trong SDK Manager.
- Nhấn **Sync Project with Gradle Files**.

### Không nhận được thông báo

- Trên Android 13 trở lên, cho phép quyền thông báo khi ứng dụng hỏi.
- Kiểm tra **Settings > Apps > RoomGo > Notifications**.
- Thông báo quản trị và khách được lưu riêng theo loại tài khoản.

### Ứng dụng lỗi sau khi thay đổi database

- Thêm migration đúng phiên bản trong quá trình phát triển.
- Nếu không cần giữ dữ liệu cũ, xóa dữ liệu ứng dụng rồi chạy lại.

### Không đăng nhập được tài khoản quản trị

- Kiểm tra đúng email `admin@gmail.com`.
- Mật khẩu mặc định chỉ áp dụng khi quản trị viên chưa đổi mật khẩu.
- Nếu quên mật khẩu ở môi trường demo, xóa dữ liệu ứng dụng để tạo lại database.

### Ảnh phòng không còn hiển thị

Ảnh chọn từ thiết bị được lưu bằng URI cục bộ. Không xóa ảnh nguồn, thu hồi quyền hoặc chuyển file sau khi thêm. Khi chuyển dữ liệu sang máy khác, cần chọn lại ảnh.

## Lưu ý phát triển

- Không truy cập Room Database trên main thread; sử dụng executor/repository hiện có.
- Khi thêm cột hoặc bảng, luôn cập nhật version và migration database.
- Chuỗi hiển thị mới nên đặt trong `res/values/strings.xml` để dễ bảo trì.
- Giữ code Java trong đúng package theo chức năng.
- Không commit `local.properties`, file build, database thiết bị hoặc thông tin bí mật.
