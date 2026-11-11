# Dựng và bảo trì LTMori 2.0.4

Mốc phát triển là APK phát hành 2.0.4 (versionCode 54). Bản này được phát hành theo quy trình vá APK. Quy trình dưới đây giải mã phần chạy của APK thành Smali và dựng lại các DEX đã sửa, giữ nguyên byte của những DEX còn lại. APK đầu vào cung cấp manifest, tài nguyên và thư viện native. Dự án Gradle/Kotlin hoàn chỉnh tương ứng với APK chưa được khôi phục.

Cần Python 3.10+, Java và Apktool 2.12.1. Từ thư mục gốc repo, dùng thư mục làm việc ngoài Git.

```sh
python3 maintenance/v2.0.4/build.py prepare --apktool /path/to/apktool.jar --work /tmp/ltmori-204
python3 maintenance/v2.0.4/build.py build --apktool /path/to/apktool.jar --work /tmp/ltmori-204
```

Lệnh đầu tự tải APK từ GitHub Releases và đối chiếu SHA-256. Với repo cần đăng nhập, tải asset từ Release và đặt vào thư mục làm việc dưới tên `baseline.apk`. Sửa Smali trong `decoded/smali*` rồi chạy lại `build`. Kết quả là `unsigned.apk`. Script hiện dựng thay đổi ở DEX. Thay đổi manifest, tài nguyên hoặc phiên bản phát hành cần bước đóng gói riêng.

- [changes-from-2.0.1.patch](changes-from-2.0.1.patch) ghi lại thay đổi Smali giữa APK 2.0.1 và 2.0.4. Dùng để tra cứu hoặc đối chiếu. Lệnh `prepare` giải mã trực tiếp 2.0.4 nên đã có các thay đổi này.
- `src/` lưu Java gốc của hai lớp giao diện Liquid Glass. `stubs/` dùng khi biên dịch các lớp này với ECJ và Android SDK, sau đó chuyển riêng các lớp sinh từ `src/` qua D8. Phần chạy của chúng nằm trong `decoded/smali_classes4`. Nếu sửa Java, đồng bộ lại Smali và giữ nguyên tập lớp gốc của DEX. Stubs chỉ phục vụ biên dịch.
- [HANDOFF.md](HANDOFF.md) ghi hành vi phải giữ, cách kiểm thử trên máy thật và các lỗi đang mở.

Trước khi cài thử, căn chỉnh bằng `zipalign -P 16 -f 4 unsigned.apk aligned.apk` rồi ký bằng `apksigner` và xác minh bằng `apksigner verify --verbose --print-certs`. Để cài đè lên bản chính, dùng chứng thư phát hành hiện hành qua người quản lý khóa. Lưu khóa và mật khẩu ngoài repo. Bản chính kế tiếp cần versionCode lớn hơn 54. Giữ mã gói hiện tại và thử cài đè trên vivo/iQOO trước khi phát hành.
