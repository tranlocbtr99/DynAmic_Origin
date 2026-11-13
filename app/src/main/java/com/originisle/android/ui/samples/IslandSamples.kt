package com.originisle.android.ui.samples

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.cards.PaymentCard
import com.originisle.android.cards.SportsCard
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ui.LtmIslandHelper
import com.originisle.android.ui.PREFS_NAME

/**
 * The unified island tester & preset catalog: combines both idle card styles and
 * sample notification cards. Tapping "Send to island" on an idle style sets it as the
 * active idle preset and renders it live on OriginIsland.
 */
object IslandSamples {

    val all: List<OriginSample> = listOf(
        // --- 1. IDLE & LIVE WIDGET PRESETS ---
        OriginSample(
            name = "Chấm tròn (Dot)",
            summary = "Chấm tròn nhỏ gọn thanh lịch trên đảo",
            preview = IslandPreview(R.drawable.ic_transparent, "", IslandPreview.Right.None),
        ) { ctx ->
            setIdlePreset(ctx, "dot")
        },
        OriginSample(
            name = "Trái Đất",
            summary = "Biểu tượng Trái Đất tĩnh tối giản",
            preview = IslandPreview(R.drawable.ic_launcher_foreground, "", IslandPreview.Right.None),
        ) { ctx ->
            setIdlePreset(ctx, "earth_only")
        },
        OriginSample(
            name = "Ngày + Trái Đất",
            summary = "Trái Đất bên trái, thứ ngày và giờ bên phải",
            preview = IslandPreview(R.drawable.ic_launcher_foreground, "", IslandPreview.Right.Text("T6 13/11 · 17:00")),
        ) { ctx ->
            setIdlePreset(ctx, "date_earth")
        },
        OriginSample(
            name = "RAM thời gian thực",
            summary = "Biểu tượng RAM + % sử dụng RAM cập nhật liên tục",
            preview = IslandPreview(R.drawable.ic_alert, "RAM", IslandPreview.Right.Text("68%")),
        ) { ctx ->
            setIdlePreset(ctx, "ram")
        },
        OriginSample(
            name = "Tốc độ mạng",
            summary = "Đo tốc độ download/upload thời gian thực",
            preview = IslandPreview(R.drawable.ic_alert, "⇅", IslandPreview.Right.Text("↓1.2M ↑150K")),
        ) { ctx ->
            setIdlePreset(ctx, "network")
        },
        OriginSample(
            name = "Thời tiết",
            summary = "Nhiệt độ, tình trạng thời tiết động",
            preview = IslandPreview(R.drawable.ic_alert, "⛅", IslandPreview.Right.Text("26°C · Nhiều mây")),
        ) { ctx ->
            setIdlePreset(ctx, "weather")
        },
        OriginSample(
            name = "Thời tiết + Sóng",
            summary = "Biểu tượng thời tiết kèm dải sóng màu",
            preview = IslandPreview(R.drawable.ic_alert, "⛈", IslandPreview.Right.Wave),
        ) { ctx ->
            setIdlePreset(ctx, "weather_wave")
        },
        OriginSample(
            name = "Sóng nhạc (Wave)",
            summary = "Dải sóng động phong cách âm nhạc",
            preview = IslandPreview(R.drawable.ic_media_play, "", IslandPreview.Right.Wave),
        ) { ctx ->
            setIdlePreset(ctx, "wave")
        },
        OriginSample(
            name = "Gemini Live",
            summary = "Biểu tượng trợ lý AI Gemini sắc nét",
            preview = IslandPreview(R.drawable.ic_alert, "✨", IslandPreview.Right.Text("Gemini Live")),
        ) { ctx ->
            setIdlePreset(ctx, "gemini")
        },
        OriginSample(
            name = "Tùy chỉnh biểu tượng",
            summary = "Biểu tượng và văn bản người dùng tự đặt",
            preview = IslandPreview(R.drawable.ic_alert, "🚀", IslandPreview.Right.Text("LocTM")),
        ) { ctx ->
            setIdlePreset(ctx, "custom")
        },

        // --- 2. SAMPLE NOTIFICATION CARDS ---
        OriginSample(
            name = "Tỷ số bóng đá",
            summary = "Bảng tỷ số trực tiếp · ARS 1 – 1 MAN · 65'",
            preview = IslandPreview(R.drawable.ic_soccer, "", IslandPreview.Right.Score(1, 1)),
        ) { ctx ->
            val ball = Icon.createWithResource(ctx, R.drawable.ic_soccer)
            SportsCard.post(
                ctx, home = "ARS", homeScore = 1, away = "MAN", awayScore = 1, clock = "65'",
                homeLogo = ball, awayLogo = ball,
            )
        },
        OriginSample(
            name = "Tải ứng dụng",
            summary = "Tiến trình tải về + vòng tròn phần trăm · 75%",
            preview = IslandPreview(R.drawable.ic_download, "Tải về", IslandPreview.Right.Progress(0.75f)),
        ) { ctx ->
            start(ctx, 40001) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
                putExtra("title", "Đang tải xuống")
                putExtra("text", "app-release.apk")
                putExtra("oi_left_content", "Tải về")
                putExtra("show_progress", true)
                putExtra("progress", 75)
                putExtra("progress_max", 100)
                putExtra("status_chip_text", "75%")
                putExtra("icon_res", R.drawable.ic_download)
            }
        },
        OriginSample(
            name = "Thanh toán",
            summary = "Kiểu Apple Pay: Đang xử lý… → Đã thanh toán ✓",
            preview = IslandPreview(R.drawable.ic_payment, "420.000₫", IslandPreview.Right.Success),
        ) { ctx ->
            PaymentCard.post(
                context = ctx, id = 40002, appIcon = null,
                amount = "420.000₫", merchant = "Café de Flore", appLabel = "Google Wallet",
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            name = "Giao đồ ăn",
            summary = "Đơn hàng đang giao · Đến sau 12 phút",
            preview = IslandPreview(R.drawable.ic_alert, "Giao hàng", IslandPreview.Right.Capsule("12 phút")),
        ) { ctx ->
            start(ctx, 40003) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Tài xế đang giao đơn")
                putExtra("text", "Đơn hàng sắp đến điểm giao")
                putExtra("oi_left_content", "Giao hàng")
                putExtra("status_chip_text", "12 phút")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            name = "Đặt xe",
            summary = "Tài xế đang đến · Toyota Prius · 3 phút",
            preview = IslandPreview(R.drawable.ic_alert, "Đón xe", IslandPreview.Right.Capsule("3 phút")),
        ) { ctx ->
            start(ctx, 40004) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Tài xế đang đến")
                putExtra("text", "Toyota Prius · Trắng · 29A-123.45")
                putExtra("oi_left_content", "Đón xe")
                putExtra("status_chip_text", "3 phút")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            name = "Chuyến bay",
            summary = "HAN → SGN · Cửa ra tàu bay K21",
            preview = IslandPreview(R.drawable.ic_alert, "HAN → SGN", IslandPreview.Right.Capsule("Cửa K21")),
        ) { ctx ->
            start(ctx, 40005) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "VN1980")
                putExtra("text", "Đang lên máy bay")
                putExtra("oi_extra1", "HAN"); putExtra("oi_extra2", "12:40")
                putExtra("oi_extra3", "SGN"); putExtra("oi_extra4", "14:55")
                putExtra("oi_left_content", "HAN → SGN")
                putExtra("status_chip_text", "Cửa K21")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            name = "Hẹn giờ",
            summary = "Đồng hồ bấm giờ · Còn lại 6:00 (40%)",
            preview = IslandPreview(R.drawable.ic_timer, "Hẹn giờ", IslandPreview.Right.Progress(0.4f)),
        ) { ctx ->
            start(ctx, 40006) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
                putExtra("title", "Đồng hồ bấm giờ")
                putExtra("text", "Còn lại 6:00")
                putExtra("oi_left_content", "Hẹn giờ")
                putExtra("show_progress", true)
                putExtra("progress", 40)
                putExtra("progress_max", 100)
                putExtra("status_chip_text", "6:00")
                putExtra("icon_res", R.drawable.ic_timer)
            }
        },
        OriginSample(
            name = "Đang xử lý",
            summary = "Dấu ba chấm động bên phải đảo",
            preview = IslandPreview(R.drawable.ic_alert, "Xử lý", IslandPreview.Right.Loading),
        ) { ctx ->
            start(ctx, 40007) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING)
                putExtra("title", "Đang xử lý…")
                putExtra("text", "Vui lòng đợi giây lát")
                putExtra("oi_left_content", "Xử lý")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            name = "Phát nhạc",
            summary = "Sóng âm thanh · Thẻ phát media",
            preview = IslandPreview(R.drawable.ic_media_play, "Midnight City", IslandPreview.Right.Wave),
        ) { ctx ->
            start(ctx, 40008) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("title", "Midnight City")
                putExtra("text", "M83")
                putExtra("oi_left_content", "Midnight City")
                putExtra("oi_wave_state", 1)
                putExtra("status_chip_text", "♪")
                putExtra("icon_res", R.drawable.ic_media_play)
            }
        },
        OriginSample(
            name = "Tin nhắn chat",
            summary = "Avatar + tên người gửi trái, tin nhắn phải",
            preview = IslandPreview(R.drawable.ic_avatar, "Sarah", IslandPreview.Right.Text("Tối nay gặp nhau nhé? 🍕")),
        ) { ctx ->
            start(ctx, 40013) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("title", "Sarah")
                putExtra("text", "Tối nay gặp nhau nhé? 🍕")
                putExtra("subtext", "Zalo · 2 tin nhắn mới")
                putExtra("oi_left_content", "Sarah")
                Icon.createWithResource(ctx, R.drawable.ic_avatar).let {
                    putExtra("oi_left_icon", it)
                    putExtra("oi_large_icon", it)
                }
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("oi_right_content", "Tối nay gặp nhau nhé? 🍕")
                putExtra("status_chip_text", "Tối nay gặp nhau nhé? 🍕")
                putExtra("icon_res", R.drawable.ic_chat)
            }
        },
        OriginSample(
            name = "Cuộc gọi đến",
            summary = "Cuộc gọi VoIP · Nút Từ chối / Trả lời",
            preview = IslandPreview(R.drawable.ic_call, "", IslandPreview.Right.Text("John")),
        ) { ctx ->
            start(ctx, 40009) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("title", "John")
                putExtra("text", "Cuộc gọi thoại Zalo")
                putExtra("oi_left_content", "")
                putExtra("oi_right_content", "John")
                putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Trả lời"))
                putExtra("click_resp", openApp(ctx))
                putExtra("icon_res", R.drawable.ic_call)
            }
        },
        OriginSample(
            name = "Điều hướng lái xe",
            summary = "Chỉ dẫn rẽ + tên đường + thời gian",
            preview = IslandPreview(R.drawable.ic_navigation, "350 m", IslandPreview.Right.Capsule("12 phút")),
        ) { ctx ->
            start(ctx, 40011) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_DRIVING_NAVI)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "350 m")
                putExtra("text", "Đường Nguyễn Trãi")
                putExtra("oi_nav_msg", "12 phút · 4,5 km · 17:30")
                putExtra("oi_nav_assist", "Đi làn bên trái")
                putExtra("oi_left_content", "350 m")
                putExtra("status_chip_text", "12 phút")
                putExtra("icon_res", R.drawable.ic_navigation)
            }
        },
        OriginSample(
            name = "Nút thao tác",
            summary = "Chia sẻ lân cận · Nút Từ chối / Nhận",
            preview = IslandPreview(R.drawable.ic_alert, "Chia sẻ", IslandPreview.Right.None),
        ) { ctx ->
            start(ctx, 40010) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("title", "Quick Share")
                putExtra("text", "Thiết bị muốn gửi một tệp")
                putExtra("oi_left_content", "Chia sẻ")
                putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Nhận"))
                putExtra("click_resp", openApp(ctx))
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
    )

    private fun setIdlePreset(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("ltm_idle_preset", key).apply()
        LtmIslandHelper.updateIdlePreset(context)
        LtmIslandHelper.syncIdleTicker(context)
    }

    private fun start(context: Context, id: Int, configure: Intent.() -> Unit) {
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("click_resp", openApp(context))
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            configure()
        }
        context.startService(intent)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
