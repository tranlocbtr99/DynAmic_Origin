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
        "copy" to mapOf("vi" to "Copy", "en" to "Copy"),
        "copied" to mapOf("vi" to "Đã copy số tài khoản", "en" to "Account number copied"),
        "contact_author" to mapOf("vi" to "Liên hệ tác giả", "en" to "Contact author"),
        "version" to mapOf("vi" to "Phiên bản", "en" to "Version"),
        "copyright" to mapOf("vi" to "© LocTM", "en" to "© LocTM"),
        "island_on" to mapOf("vi" to "Bật Đảo", "en" to "Enable Island"),
        "restart_island" to mapOf("vi" to "Khởi động lại Đảo", "en" to "Restart Island"),
        "send_to_island" to mapOf("vi" to "Send to island", "en" to "Send to island"),
        "notifications" to mapOf("vi" to "Thông báo", "en" to "Notifications"),
        "media" to mapOf("vi" to "Nhạc", "en" to "Media"),
        "live_only" to mapOf("vi" to "Chỉ live", "en" to "Live only"),
        "everything" to mapOf("vi" to "Tất cả", "en" to "Everything"),
        "language" to mapOf("vi" to "Ngôn ngữ", "en" to "Language"),
        "apps" to mapOf("vi" to "Danh sách app", "en" to "App list"),
        "activity" to mapOf("vi" to "Activity", "en" to "Activity"),
        "start" to mapOf("vi" to "Bắt đầu", "en" to "Start"),
        "privacy_rule" to mapOf("vi" to "Tôi đồng ý với quy tắc bảo mật", "en" to "I agree to the privacy policy"),
        "continue_btn" to mapOf("vi" to "Tiếp tục", "en" to "Continue"),
        "permissions" to mapOf("vi" to "Quyền", "en" to "Permissions"),
        "copied_toast" to mapOf("vi" to "Đã copy", "en" to "Copied"),
    )
}
