package com.originisle.android.ui

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight bilingual (VI/EN) string table. Default language follows the device locale;
 * user can override via prefs key "app_language" ("vi" / "en").
 */
object Lang {
    private const val PREF_KEY = "app_language"

    fun currentLang(context: Context): String {
        val prefs = context.getSharedPreferences(com.originisle.android.ui.PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY, null)
        if (saved == "vi" || saved == "en") return saved
        return if (java.util.Locale.getDefault().language == "vi") "vi" else "en"
    }

    fun isVi(context: Context): Boolean = currentLang(context) == "vi"

    fun s(context: Context, key: String): String = table[key]?.get(currentLang(context)) ?: table[key]?.get("en") ?: key

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(com.originisle.android.ui.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, lang).apply()
    }

    private val table: Map<String, Map<String, String>> = mapOf(
        "app_name" to mapOf("vi" to "LTMori", "en" to "LTMori"),
        "tab_island" to mapOf("vi" to "Đảo", "en" to "Island"),
        "tab_notifications" to mapOf("vi" to "Thông báo", "en" to "Notifications"),
        "tab_customize" to mapOf("vi" to "Tuỳ chỉnh", "en" to "Customize"),
        "tab_settings" to mapOf("vi" to "Cài đặt", "en" to "Settings"),
        "donate" to mapOf("vi" to "Ủng hộ tác giả", "en" to "Support author"),
        "bank" to mapOf("vi" to "Techcombank", "en" to "Techcombank"),
        "account_name" to mapOf("vi" to "Tran Minh Loc", "en" to "Tran Minh Loc"),
        "account_number" to mapOf("vi" to "4444444790", "en" to "4444444790"),
        "copy" to mapOf("vi" to "Sao chép", "en" to "Copy"),
        "copied" to mapOf("vi" to "Đã copy số tài khoản", "en" to "Account number copied"),
        "contact_author" to mapOf("vi" to "Liên hệ tác giả", "en" to "Contact author"),
        "version" to mapOf("vi" to "Phiên bản", "en" to "Version"),
        "copyright" to mapOf("vi" to "© LocTM", "en" to "© LocTM"),
        "island_on" to mapOf("vi" to "Bật Đảo", "en" to "Enable Island"),
        "restart_island" to mapOf("vi" to "Khởi động lại Đảo", "en" to "Restart Island"),
        "send_to_island" to mapOf("vi" to "Chuyển tiếp lên đảo", "en" to "Send to island"),
        "notifications" to mapOf("vi" to "Thông báo", "en" to "Notifications"),
        "media" to mapOf("vi" to "Nhạc & Media", "en" to "Media"),
        "live_only" to mapOf("vi" to "Chỉ trực tiếp", "en" to "Live only"),
        "everything" to mapOf("vi" to "Tất cả", "en" to "Everything"),
        "language" to mapOf("vi" to "Ngôn ngữ", "en" to "Language"),
        "apps" to mapOf("vi" to "Danh sách ứng dụng", "en" to "Apps"),
        "activity" to mapOf("vi" to "Nhật ký hoạt động", "en" to "Activity"),
        "start" to mapOf("vi" to "Bắt đầu", "en" to "Start"),
        "privacy_rule" to mapOf("vi" to "Tôi đồng ý với quy tắc bảo mật", "en" to "I agree to the privacy policy"),
        "continue_btn" to mapOf("vi" to "Tiếp tục", "en" to "Continue"),
        "permissions" to mapOf("vi" to "Quyền hệ thống", "en" to "Permissions"),
        "copied_toast" to mapOf("vi" to "Đã sao chép", "en" to "Copied"),
        "listening" to mapOf("vi" to "Đang hoạt động", "en" to "Listening"),
        "not_listening" to mapOf("vi" to "Chưa hoạt động", "en" to "Not listening"),
        "check_updates" to mapOf("vi" to "Kiểm tra cập nhật", "en" to "Check for updates"),
        "checking" to mapOf("vi" to "Đang kiểm tra…", "en" to "Checking…"),
        "more" to mapOf("vi" to "Thêm", "en" to "More"),
        "recast_all" to mapOf("vi" to "Phát lại tất cả", "en" to "Recast all"),
        "stop_all" to mapOf("vi" to "Dừng tất cả", "en" to "Stop all"),
        "sent" to mapOf("vi" to "Đã gửi", "en" to "Sent"),
        "send_button" to mapOf("vi" to "Gửi lên đảo", "en" to "Send to island"),
        "collapse" to mapOf("vi" to "Thu gọn", "en" to "Collapse"),
        "expand" to mapOf("vi" to "Mở rộng", "en" to "Expand"),
        "cast_notifications_subtitle" to mapOf("vi" to "Chuyển tiếp thông báo từ các ứng dụng lên đảo", "en" to "Cast notifications to the island"),
        "cast_media_subtitle" to mapOf("vi" to "Trình phát nhạc trên đảo cho ứng dụng nghe nhạc", "en" to "Island media player for music apps"),
        "every_notif_desc" to mapOf("vi" to "Mọi thông báo, bao gồm tin nhắn thường.", "en" to "Every notification, plain messages included."),
        "only_live_desc" to mapOf("vi" to "Chỉ thẻ trực tiếp — bóng đá, điều hướng, thanh toán...", "en" to "Only live cards — score, navigation, payment, etc."),
        "hide_after" to mapOf("vi" to "Tự động ẩn sau", "en" to "Hide after"),
        "non_live_cards" to mapOf("vi" to "Áp dụng cho thẻ không trực tiếp", "en" to "Non live cards"),
        "never" to mapOf("vi" to "Không bao giờ", "en" to "Never"),
        "earbuds" to mapOf("vi" to "Tai nghe Bluetooth", "en" to "Earbuds"),
        "earbuds_subtitle" to mapOf("vi" to "Hiển thị pin khi tai nghe kết nối", "en" to "Battery when they connect"),
        "skip_silent" to mapOf("vi" to "Bỏ qua thông báo im lặng", "en" to "Skip silent"),
        "skip_silent_subtitle" to mapOf("vi" to "Không hiện thông báo không có âm thanh/rung", "en" to "Ignore silenced notifications"),
        "expand_on_notif" to mapOf("vi" to "Bật sáng đảo khi có thông báo", "en" to "Expand island on notification"),
        "expand_on_notif_subtitle" to mapOf("vi" to "Tự động bung rộng đảo khi có thông báo mới", "en" to "Pop the island when a notification arrives"),
        "banner_style" to mapOf("vi" to "Kiểu bung khi có thông báo", "en" to "Notification banner style"),
        "banner_style_subtitle" to mapOf("vi" to "Giao diện hiển thị khi thông báo xuất hiện", "en" to "Banner appearance when notifications arrive"),
        "style_liquid_glass" to mapOf("vi" to "Kính lỏng iOS (Mặc định)", "en" to "Liquid Glass iOS (Default)"),
        "style_liquid_glass_sub" to mapOf("vi" to "Hộp kính lỏng iPhone bo cong, giữ nguyên đảo con nhộng của máy", "en" to "Apple-style glass banner, preserves native capsule"),
        "style_superx" to mapOf("vi" to "Đảo mặc định Vivo", "en" to "Vivo native island"),
        "style_superx_sub" to mapOf("vi" to "Bung mở rộng thẻ đảo mặc định của OriginOS", "en" to "Expand native OriginOS SuperX island card"),
        "style_capsule_only" to mapOf("vi" to "Chỉ hiện con nhộng", "en" to "Capsule only"),
        "style_capsule_only_sub" to mapOf("vi" to "Không bung hộp lớn, chỉ hiện đảo nhỏ quanh camera", "en" to "No popup banner, only compact camera pill"),
        "dedup_same_app" to mapOf("vi" to "Chống lặp cùng ứng dụng", "en" to "Dedup same app"),
        "dedup_same_app_subtitle" to mapOf("vi" to "Không lặp lại hiệu ứng mở rộng liên tục từ cùng 1 app", "en" to "Don't re-expand for repeated notifications from one app"),
        "hide_from_shade" to mapOf("vi" to "Ẩn khỏi bảng thông báo", "en" to "Hide from notification shade"),
        "hide_from_shade_subtitle" to mapOf("vi" to "Chỉ hiển thị trên đảo, không tạo thông báo trùng lặp ở thanh kéo xuống", "en" to "Show island card only, no duplicate in shade"),
        "lockscreen" to mapOf("vi" to "Màn hình khóa", "en" to "Lock screen"),
        "lockscreen_subtitle" to mapOf("vi" to "Hiển thị đảo và thẻ trực tiếp trên màn hình khóa", "en" to "Show island & live card on lock screen"),
        "idle_island_toggle" to mapOf("vi" to "Hiển thị Đảo chờ", "en" to "Show Idle Island"),
        "idle_island_toggle_subtitle" to mapOf("vi" to "Luôn hiện viên Đảo khi không có thông báo (Tắt để chỉ hiện khi có thông báo mới)", "en" to "Always show island when idle (Turn off to show only on new notifications)"),
        "cards_section" to mapOf("vi" to "Các loại thẻ", "en" to "Cards"),
        "otp_codes" to mapOf("vi" to "Mã xác thực OTP", "en" to "Login codes"),
        "otp_codes_subtitle" to mapOf("vi" to "Tự động nhận diện mã và nhấn để sao chép", "en" to "Show the code, tap to copy"),
        "actions" to mapOf("vi" to "Nút thao tác nhanh", "en" to "Action buttons"),
        "actions_subtitle" to mapOf("vi" to "Hiển thị nút hành động của ứng dụng ngay trên thẻ", "en" to "The app's own buttons on the card"),
        "live_lyrics" to mapOf("vi" to "Lời bài hát trực tiếp", "en" to "Live lyrics"),
        "live_lyrics_subtitle" to mapOf("vi" to "Đồng bộ lời bài hát từ LRCLIB lên thẻ nhạc", "en" to "Sync lyrics from LRCLIB onto the media card"),
        "charging" to mapOf("vi" to "Hiển thị khi sạc pin", "en" to "Charging"),
        "charging_subtitle" to mapOf("vi" to "Hiện thẻ trạng thái pin khi cắm sạc", "en" to "Battery card when you plug in"),
        "briefly" to mapOf("vi" to "Trong giây lát", "en" to "Briefly"),
        "while_charging" to mapOf("vi" to "Suốt quá trình sạc", "en" to "While charging"),
        "live_updates_charging" to mapOf("vi" to "Live Updates khi sạc", "en" to "Live Updates when charging"),
        "live_updates_charging_subtitle" to mapOf("vi" to "Thử nghiệm thông báo sạc ưu tiên của Android", "en" to "Try Android's priority charging notification"),
        "privacy_dot" to mapOf("vi" to "Bảo mật Camera & Mic", "en" to "Camera & mic"),
        "privacy_dot_subtitle" to mapOf("vi" to "Hiện chấm cảnh báo khi camera hoặc micro đang bật", "en" to "Dot while they're in use"),
        "idle_gemini" to mapOf("vi" to "Thẻ chờ & Trợ lý Gemini", "en" to "Idle & Gemini"),
        "gemini_tap" to mapOf("vi" to "Chạm đảo mở Gemini", "en" to "Tap island to open Gemini"),
        "gemini_tap_subtitle" to mapOf("vi" to "Chạm vào đảo hoặc thẻ chờ để kích hoạt Gemini", "en" to "Tap the island or idle card to launch Gemini"),
        "open_gemini" to mapOf("vi" to "Mở Gemini", "en" to "Open Gemini"),
        "circle_to_search" to mapOf("vi" to "Circle to Search", "en" to "Circle to Search"),
        "quiet_hours" to mapOf("vi" to "Giờ yên tĩnh", "en" to "Quiet hours"),
        "quiet_dnd" to mapOf("vi" to "Theo chế độ Không làm phiền (DND)", "en" to "Follow Do Not Disturb"),
        "quiet_dnd_subtitle" to mapOf("vi" to "Tự động tắt thông báo đảo khi đang bật DND", "en" to "Quiet while DND is on"),
        "schedule" to mapOf("vi" to "Hẹn giờ", "en" to "Schedule"),
        "quiet_hours_desc" to mapOf(
            "vi" to "Trong giờ yên tĩnh, chỉ có cuộc gọi, điều hướng, hẹn giờ, mã OTP, nhạc và sạc pin được hiển thị.",
            "en" to "While quiet, only calls, navigation, timers, login codes, media and charging show."
        ),
        "apps_all_on" to mapOf("vi" to "Tất cả đang bật", "en" to "All on"),
        "apps_off" to mapOf("vi" to "đã tắt", "en" to "off"),
        "idle_preset" to mapOf("vi" to "Kiểu thẻ chờ trên đảo", "en" to "Idle preset"),
        "idle_card_text" to mapOf("vi" to "Nội dung thẻ chờ", "en" to "Idle card text"),
        "edit" to mapOf("vi" to "Sửa", "en" to "Edit"),
        "save" to mapOf("vi" to "Lưu", "en" to "Save"),
        "cancel" to mapOf("vi" to "Hủy", "en" to "Cancel"),
        "close" to mapOf("vi" to "Đóng", "en" to "Close"),
        "default_title" to mapOf("vi" to "Khôi phục mặc định", "en" to "Restore defaults"),
        "title" to mapOf("vi" to "Tiêu đề", "en" to "Title"),
        "content" to mapOf("vi" to "Nội dung", "en" to "Content"),
        "hi_jovi_bridge" to mapOf("vi" to "Cầu nối Gemini (Hi Jovi)", "en" to "Gemini Bridge (Hi Jovi)"),
        "setup_hi_jovi" to mapOf("vi" to "Thiết lập phím gọi AI / Jovi", "en" to "Setup AI Key / Jovi"),
        "open" to mapOf("vi" to "Mở", "en" to "Open"),
        "ram_monitor" to mapOf("vi" to "Bộ nhớ RAM hệ thống", "en" to "System RAM Monitor"),
        "ram_realtime" to mapOf("vi" to "Thời gian thực", "en" to "Real-time"),
        "ram_used" to mapOf("vi" to "Đã dùng", "en" to "Used"),
        "ram_free" to mapOf("vi" to "Còn trống", "en" to "Free"),
        "ram_total" to mapOf("vi" to "Tổng RAM", "en" to "Total RAM"),
        "ram_status_smooth" to mapOf("vi" to "Rất mượt", "en" to "Smooth"),
        "ram_status_normal" to mapOf("vi" to "Bình thường", "en" to "Normal"),
        "ram_status_high" to mapOf("vi" to "Tải cao", "en" to "High"),
        "sample_tester_title" to mapOf("vi" to "Thử nghiệm hiển thị trên đảo", "en" to "Island Sample Tester"),
        "about_app" to mapOf("vi" to "Giới thiệu LTMori", "en" to "About LTMori"),
        "about_app_desc" to mapOf("vi" to "Tính năng, kế thừa nền tảng & bản quyền", "en" to "Features, foundation & copyright"),
        "ai_assistant_title" to mapOf("vi" to "Trợ lý AI & Chạm đảo", "en" to "AI Assistant & Island Tap"),
        "select_assistant_mode" to mapOf("vi" to "HÀNH VI KHI KÍCH HOẠT TRỢ LÝ", "en" to "ASSISTANT ACTIVATION ACTION"),
        "gemini_voice_title" to mapOf("vi" to "Gemini Voice", "en" to "Gemini Voice"),
        "gemini_voice_desc" to mapOf("vi" to "Mở lớp phủ Gemini và tự động kích hoạt micro", "en" to "Launch Gemini overlay and enable mic"),
        "cts_title" to mapOf("vi" to "Circle to Search", "en" to "Circle to Search"),
        "cts_desc" to mapOf("vi" to "Khoanh tròn tìm kiếm hoặc dịch nội dung màn hình", "en" to "Circle to Search or translate screen content"),
        "test_assistant" to mapOf("vi" to "Thử kích hoạt ngay", "en" to "Test activate now"),
        "test_gemini" to mapOf("vi" to "Thử mở Gemini Voice", "en" to "Test Gemini Voice"),
        "test_cts" to mapOf("vi" to "Thử mở Circle to Search", "en" to "Test Circle to Search"),
        "call_island_only" to mapOf("vi" to "Cuộc gọi đến trên đảo", "en" to "Calls on island"),
        "call_island_only_subtitle" to mapOf(
            "vi" to "Hiện cuộc gọi trên đảo, không bung toàn màn hình khi đang dùng máy",
            "en" to "Show call on island, prevent full-screen interruption while using phone"
        ),
        "vivo_optimizer" to mapOf("vi" to "Tối ưu hóa Vivo & iQOO", "en" to "Vivo & iQOO Optimizer"),
        "vivo_optimizer_desc" to mapOf(
            "vi" to "Giữ đảo vĩnh viễn & Fix trễ thông báo máy nội địa",
            "en" to "Permanent keep-alive & Fix notification delay"
        ),
    )
}
