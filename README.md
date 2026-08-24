# RoomGo

RoomGo là ứng dụng Android Java minh họa quy trình tìm phòng, đặt chỗ, đánh giá và quản trị homestay. Dữ liệu demo được lưu cục bộ bằng Room/SQLite.

## Cấu trúc mã nguồn

```text
com.example.homestay
├── HomestayApplication.java     Khởi tạo database và repository
├── data
│   ├── dao                      Truy vấn Room/SQLite
│   ├── database                 Database và migration
│   ├── entity                   Các bảng dữ liệu
│   ├── model                    Dữ liệu dùng giữa các màn hình
│   └── repository               Xử lý và cung cấp dữ liệu
├── domain                       Quy tắc đặt phòng, tính tiền, tìm kiếm
├── ui
│   ├── auth                     Đăng nhập, đăng ký, splash
│   ├── customer                 Trang chính và thông báo khách
│   ├── room                     Chi tiết và đặt phòng
│   ├── admin                    Các màn hình quản trị
│   └── adapter                  RecyclerView/ViewPager adapters
└── utils                        Tiện ích dùng chung
```

## Chức năng chính

- Tìm kiếm, lọc, lưu yêu thích và xem chi tiết phòng.
- Đặt phòng, hủy booking, tính hoàn tiền và đánh giá sau lưu trú.
- Nhiều ảnh phòng dạng slide.
- Thông báo khách và quản trị tách biệt.
- Quản lý phòng, booking, người dùng và báo cáo tổng quan.
- Khóa tài khoản và chuyển đổi giao diện admin/khách.

## Kiểm tra dự án

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat lintDebug
```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.
