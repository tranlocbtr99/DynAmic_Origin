# Bàn giao kỹ thuật LTMori

Mốc bàn giao 11/11/2026. LTMori đưa thông báo, nhạc và hoạt động trực tiếp lên Origin Island của vivo/iQOO tương thích. Điện thoại tham chiếu của chủ dự án là iQOO Neo 9. Người dùng đã xác nhận bản 2.0.4 hoạt động tốt trên máy của họ. Báo cáo từ máy khác vẫn cần tái hiện.

- [Repo DynAmic_Origin](https://github.com/tranlocbtr99/DynAmic_Origin)
- [Bản chính 2.0.4 và APK đã ký](https://github.com/tranlocbtr99/DynAmic_Origin/releases/tag/v2.0.4)
- [Quyền sử dụng mã](https://github.com/tranlocbtr99/DynAmic_Origin/blob/main/LICENSE.md)

## Chọn đúng bản để sửa

**APK phát hành 2.0.4 là mốc đối chiếu hành vi.** Bản này có versionCode 54 và SHA-256 `5aafcf8bcefe5d78dd54c980df4dd1ec03f7d440339baaca651cc70bd975e2f4`. Nội dung chạy của 2.0.4 lấy từ Liquid Glass Beta 3. Lần phát hành chỉ đổi phiên bản trong manifest và ký lại. Bắt đầu với [hướng dẫn dựng 2.0.4](README.md). Các thư mục và script trong hướng dẫn là đường dẫn dùng được sau khi clone. GitHub Release 2.0.4 cung cấp APK đầu vào. Bản vá trong repo ghi lại các khác biệt Smali so với 2.0.1 để tra cứu.

## Những luồng phải giữ

1. **Thông báo và Island.** Luồng nhận thông báo → áp dụng bộ lọc → tạo thẻ SuperX → OriginOS hiển thị. Xem `NotificationCastListener`, các lớp trong `cards/`, `OriginIslandBuilder` và `PlaygroundService`. Giữ trạng thái bộ lọc theo app, chế độ Tất cả/Đang diễn ra, thông báo im lặng và giờ yên lặng. Với thẻ chuyển tiếp, giữ thao tác mở nội dung, tiến trình và nút của ứng dụng gửi.
2. **Chấm xanh và thẻ chờ.** Thẻ ID `0xee84` tạo chấm xanh và có luồng trả về chờ sau khi thẻ hoạt động cuối cùng kết thúc hoặc bị gạt. Thao tác tắt trong app phải dừng chế độ chờ. Thẻ chờ và thông báo foreground service “Island đang bật” có vai trò riêng. Kiểm tra cả lúc gạt thẻ chờ, gạt thẻ thường, nhiều thẻ nối tiếp và sau khi khóa/mở màn hình.
3. **Chạm Island.** Chạm chấm xanh mở dải ứng dụng gần đây qua `RecentAppsActivity`, cần quyền truy cập dữ liệu sử dụng. Chạm thẻ đang hoạt động giữ hành động tương ứng với thông báo đó. Thao tác nhấn giữ chưa được xác minh có thể gán riêng trên OriginOS.
4. **Tự bung tin nhắn.** Chỉ yêu cầu bung cho lượt tin nhắn mới khi máy đang mở khóa, sau đó OriginOS điều khiển việc thu về viên nhỏ. Giữ lọc lượt cập nhật lặp và thử thêm trường hợp bong bóng Messenger. Tránh làm toàn bộ thông báo tự bung khi chỉ sửa một kiểu thẻ.
5. **Nhạc, icon và giao diện.** Thẻ media giữ tiêu đề, dữ liệu phiên phát và thao tác phát/tạm dừng. Đổi nhãn nguồn thành icon khi ô đó chỉ chứa tên app hoặc mã gói. Kiểm tra cả thẻ bung và viên nhỏ. Giữ giao diện Liquid Glass kiểu Cài đặt, nút Island và công tắc trên màn chính, tùy chỉnh tiêu đề/nội dung/sticker, nút ủng hộ, quyền riêng tư và lựa chọn Tiếng Việt/English.
6. **Cập nhật và dữ liệu.** Giữ application ID thực tế của APK 2.0.4 và cùng chứng thư ký để cài đè. Kiểm tra phiên bản trong manifest, các cài đặt đã lưu và quyền sau khi cập nhật. Chỉ chủ dự án hoặc người được ủy quyền ký bản phát hành. Giữ khóa và mật khẩu ngoài Git, tài liệu bàn giao và log.

## Quy trình làm một thay đổi

1. Ghi phiên bản, model, bản OriginOS, tên app nguồn và các bước tái hiện. So sánh với APK 2.0.4 trước khi sửa. Đối với lỗi hiển thị, chụp trạng thái viên nhỏ, thẻ bung và màn hình khóa nếu có liên quan.
2. Sửa tại lớp chịu trách nhiệm và dùng lại luồng đang chạy. Đối với APK vá, dùng Apktool để đọc và lắp Smali, ECJ/D8 cho Java bổ sung. Kiểm tra tham chiếu phương thức, lớp trùng giữa các DEX, manifest, chữ ký và những thành phần APK thay đổi so với bản nền.
3. Đóng APK thử có nhãn riêng. Kiểm tra cài đè từ 2.0.4 và kiểm tra hành vi trên iQOO. Ghi rõ test cấu trúc APK và test máy thật. Khi thử lỗi tương thích, ghi kết quả theo từng model và bản OriginOS.
4. Khi phát hành chính thức, dùng versionCode lớn hơn 54 nếu 2.0.4 vẫn là bản chính. Đồng bộ versionName, tag và tên asset `LTMori-<version>-Origin-signed.apk`. Updater đọc GitHub Releases `/releases/latest`, đối chiếu phiên bản, mã gói và chữ ký trước khi mở trình cài Android. Xác minh bản mới xuất hiện trên kênh này, digest tải về khớp và ghi chú cập nhật hiển thị đúng.

## Kiểm tra hồi quy trước khi phát hành

| Luồng | Kết quả cần thấy |
|---|---|
| Cài đè từ 2.0.4 | Mở app được, dữ liệu cài đặt và ngôn ngữ còn đúng. |
| Bật/tắt và gạt Island | Bật có chấm xanh. Có thông báo thì hiển thị thẻ. Gạt thẻ cuối thì về chấm xanh. Tắt chủ động thì chấm xanh dừng. |
| Tương tác | Chạm chấm xanh mở app gần đây. Chạm thẻ tin nhắn mở đúng nguồn. Nút phát/tạm dừng thực sự điều khiển phiên phát. |
| Tin nhắn và bộ lọc | Tin mới hợp lệ tự bung rồi thu nhỏ. Mở/đóng bong bóng khi không có tin mới không bung lặp. Các app bị tắt chuyển tiếp vẫn bị lọc. |
| Thẻ nhạc và icon | Thẻ bung có nội dung với cả app có và thiếu ảnh bìa. Icon nguồn không làm mất nội dung, tiến trình hoặc nút. |
| UI và cập nhật | Kiểm tra trang con, quay lại, tùy chỉnh thẻ chờ, đổi ngôn ngữ, ghi chú phát hành và cài đè qua updater. |

## Việc đang mở

- **Hạn chế cài đặt/hộp cách ly.** Phân biệt Android chặn quyền truy cập thông báo với vivo đưa APK vào Hộp cách ly. Ảnh hiện có ghi Origin Isle 1.3.7. Cần tái hiện bằng APK LTMori chính thức và ghi đúng thông báo hệ thống.
- **Thẻ nhạc bị đen khi bung.** So sánh các app trên cùng bản OriginOS. Kiểm tra metadata và ảnh bìa trước, rồi thử icon ứng dụng hoặc thẻ dự phòng có tiêu đề và nút nếu mẫu mở rộng vẫn lỗi.
- **Messenger mở bong bóng gây hiện liên tục.** Xác định đó là thẻ Island hay hộp thoại hệ thống. Thử mở/đóng không có tin mới rồi nhận một tin thật. Chỉ lọc lượt đăng lại do thay đổi trạng thái bong bóng.
- **Control Center và Circle to Search.** Đây là hướng thử. Ô Cài đặt nhanh có thể dùng để bật/khôi phục Island, còn hiển thị chấm xanh vẫn cần kiểm tra qua luồng thẻ chờ. Với Circle to Search, thử gọi trợ lý Google qua cơ chế Android và xác nhận trên iQOO. Mở được Assistant chỉ là kết quả trung gian. Kiểm tra giấy phép GPLv3 nếu cân nhắc nhúng mã MiCTS vào APK.

