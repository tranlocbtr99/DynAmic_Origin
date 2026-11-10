# LTMori

**Designed by LocTM · 3.0.0-beta**  
[Tiếng Việt](#tiếng-việt) · [English](#english)

## Tiếng Việt

LTMori là bản tùy chỉnh độc lập dựa trên Origin Isle, dùng để đưa thông báo và hoạt động trực tiếp lên Origin Island trên vivo/iQOO chạy OriginOS tương thích.

- Hiển thị thẻ thông báo, đa phương tiện và hoạt động trực tiếp. Khi chờ, Island hiện chấm xanh. Chạm chấm để mở dải ứng dụng gần đây nếu đã cấp quyền sử dụng.
- Màn hình lần đầu yêu cầu đọc và đồng ý chính sách quyền riêng tư. Nội dung thông báo và lịch sử ứng dụng gần đây được xử lý trên thiết bị. GitHub phục vụ cập nhật và thẻ thể thao có thể tra cứu biểu trưng qua TheSportsDB.
- Từ 1.3.5, mục **Kiểm tra cập nhật** có thể tải APK từ GitHub Releases, hiển thị tiến độ và kiểm tra mã gói, phiên bản, chữ ký trước khi mở trình cài đặt Android.

- Từ 2.0.1, app tự kiểm tra bản mới và hiển thị ghi chú trước khi tải. Chế độ thông báo yêu cầu xác nhận trước khi áp dụng.
- Từ 3.0.0-beta, hỗ trợ kênh cập nhật Beta (tùy chọn trong Giới thiệu), Island state detection và nghiên cứu widget.

Nhánh `main` là nền phát triển ổn định 2.0.3. Nhánh `beta/v3-island-widgets` là nền phát triển beta 3.0.0. Repo lưu mã nguồn bản vá và công cụ đóng gói lại từ APK 1.3.2. Khả năng hiển thị Island phụ thuộc vào tích hợp SuperX trên thiết bị. [Hướng dẫn build và phát hành](repair/README-build.md). [Phân tích app tham chiếu](research/REFERENCE_APP_ANALYSIS.md).

Việc tái sử dụng phần gốc của LTMori để tạo sản phẩm tương tự cần được LocTM cho phép trước bằng văn bản. [Quyền sử dụng](LICENSE.md#tiếng-việt).

## English

LTMori is an independent customization based on Origin Isle. It displays notifications and live activities on Origin Island on compatible vivo and iQOO devices running OriginOS.

- Displays notification, media, and live activity cards. A green dot marks the idle state. Tap it for the recent-app strip when Usage Access is granted.
- First launch requires reading and accepting the privacy policy. Notification content and recent-app history are processed on-device. GitHub serves updates, and sports cards may fetch team artwork from TheSportsDB.
- Starting with 1.3.5, **Check for updates** can download a GitHub Releases APK, show progress, and verify the package, version, and signature before opening the Android installer.

- Starting with 2.0.1, LTMori checks for releases automatically and displays release notes before downloading. Notification mode changes require confirmation.
- Starting with 3.0.0-beta, optional beta release channel (toggle in About), Island state detection, and widget research foundation.

The `main` branch is the stable 2.0.3 baseline. The `beta/v3-island-widgets` branch is the 3.0.0-beta development baseline. This repository contains the patch source and tooling to rebuild from the 1.3.2 APK. Island rendering depends on device support for SuperX. [Build and release guide](repair/README-build.md). [Reference app analysis](research/REFERENCE_APP_ANALYSIS.md).

Reusing original LTMori material to create a similar product requires LocTM's prior written permission. [Reuse policy](LICENSE.md#english).

[Origin Isle](https://github.com/fvhde/origin-isle) · [GitHub Releases](https://github.com/tranlocbtr99/DynAmic_Origin/releases)
