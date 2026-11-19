package com.originisle.android.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.R
import com.originisle.android.cards.ChargingCard
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.cards.PrivacyCard
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.service.QuietHours
import com.originisle.android.service.LtmVivoOptimizer
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The main screen: listener status, compact real-time RAM monitor, unified island tester & presets,
 * casting settings, card customizations, and system controls.
 */
@Composable
fun HomeScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", false)) }
    var selectedTab by remember { mutableStateOf(0) }
    var donateExpanded by remember { mutableStateOf(true) }
    var sendToIslandExpanded by remember { mutableStateOf(true) }
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", true)) }
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var expandOnNotification by remember { mutableStateOf(prefs.getBoolean("cast_expand_on_notification", true)) }
    var notifBannerStyle by remember { mutableStateOf(prefs.getString("notif_banner_style", "liquid_glass") ?: "liquid_glass") }
    var liquidGlassTransparency by remember { mutableStateOf(prefs.getInt("liquid_glass_transparency", 65)) }
    var hideFromShade by remember { mutableStateOf(prefs.getBoolean("cast_hide_from_shade", false)) }
    var dedupSameApp by remember { mutableStateOf(prefs.getBoolean("cast_dedup_same_app", true)) }
    var dedupCooldown by remember { mutableStateOf(prefs.getInt("cast_dedup_cooldown_sec", 10)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", true)) }
    var autoDismissSec by remember { mutableStateOf(prefs.getInt("cast_auto_dismiss_seconds", 0)) }
    var otpOn by remember { mutableStateOf(prefs.getBoolean("cast_otp", true)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }
    var liveLyricsOn by remember { mutableStateOf(prefs.getBoolean("cast_live_lyrics_opt_in", false)) }
    var idleIslandOn by remember { mutableStateOf(prefs.getBoolean("ltm_idle_card_enabled", true)) }
    var geminiTap by remember { mutableStateOf(prefs.getBoolean("ltm_idle_gemini_tap", false)) }
    val bridgePrefs = remember { context.getSharedPreferences("ai_key_bridge_settings", Context.MODE_PRIVATE) }
    var voiceAction by remember { mutableStateOf(bridgePrefs.getString("voice_action", "GEMINI_VOICE").orEmpty()) }
    var appLang by remember { mutableStateOf(Lang.currentLang(context)) }
    var idleCardEditOpen by remember { mutableStateOf(false) }
    var privacyPolicyOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var optimizerOpen by remember { mutableStateOf(false) }
    var bankGuardOn by remember { mutableStateOf(com.aikeybridge.guard.BankGuard.isEnabled(context)) }
    var callBlockDialogOpen by remember { mutableStateOf(false) }
    var callIslandOnly by remember { mutableStateOf(prefs.getBoolean("cast_call_island_only", false)) }
    var idleTitle by remember { mutableStateOf(prefs.getString("ltm_idle_card_title", "LTMori").orEmpty()) }
    var idleText by remember { mutableStateOf(prefs.getString("ltm_idle_card_text", "Đang chờ").orEmpty()) }
    var chargingLiveUpdates by remember { mutableStateOf(prefs.getBoolean("cast_charging_live_updates", false)) }
    var chargingOn by remember { mutableStateOf(prefs.getBoolean("cast_charging", true)) }
    var chargingKeep by remember { mutableStateOf(prefs.getBoolean("cast_charging_keep", false)) }
    var privacyDotOn by remember { mutableStateOf(prefs.getBoolean("cast_privacy_dot", true)) }
    var earbudsOn by remember {
        mutableStateOf(prefs.getBoolean("cast_earbuds", false) && EarbudsCard.hasPermission(context))
    }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        earbudsOn = granted
        if (granted) {
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        } else {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
    var quietDnd by remember { mutableStateOf(prefs.getBoolean("cast_quiet_dnd", false)) }
    var quietSchedule by remember { mutableStateOf(prefs.getBoolean("cast_quiet_schedule", false)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt("cast_quiet_start", QuietHours.DEFAULT_START)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt("cast_quiet_end", QuietHours.DEFAULT_END)) }
    val appsOff = remember { prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty().size }
    val tick = rememberResumeTick()

    LaunchedEffect(tick.intValue) {
        val now = prefs.getBoolean("cast_earbuds", false) && EarbudsCard.hasPermission(context)
        if (now != earbudsOn) {
            earbudsOn = now
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        }
    }

    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updateDialogOpen by remember { mutableStateOf(false) }
    var updateAvailableData by remember { mutableStateOf<UpdateChecker.Result.UpdateAvailable?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    LaunchedEffect(Unit) {
        while (true) { connected = NotificationCastListener.instance != null; delay(1000) }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    if (privacyPolicyOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { privacyPolicyOpen = false },
            title = { Text(Lang.s(context, "privacy_rule"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ứng dụng LTMori tôn trọng tối đa quyền riêng tư của bạn:\n\n" +
                        "• Toàn bộ quá trình quét và hiển thị thông báo diễn ra hoàn toàn trên thiết bị (Offline).\n" +
                        "• Ứng dụng không thu thập, không lưu trữ và không gửi bất kỳ nội dung thông báo, tin nhắn hay thông tin cá nhân nào ra ngoài.\n" +
                        "• Quyền truy cập thông báo chỉ dùng để xây dựng các thẻ hiển thị trên thanh đảo Dynamic/Atomic Island của Vivo OriginOS.\n" +
                        "• LTMori v3.0.1 phát triển bởi LocTM.\n" +
                        "• Chân thành biết ơn và kế thừa nền tảng từ tác giả Origin Isle & cộng đồng nguồn mở.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = IsleColors.Text,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { privacyPolicyOpen = false }) { Text(Lang.s(context, "close")) }
            },
        )
    }

    if (aboutOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { aboutOpen = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(IsleIcons.Heart, contentDescription = null, tint = IsleColors.TilePurple, modifier = Modifier.size(24.dp))
                    Text(Lang.s(context, "about_app"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "LTMori v3.0.1",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = IsleColors.Accent,
                    )
                    Text(
                        "Ứng dụng tối ưu hoá trải nghiệm Đảo Động (Dynamic Island / SuperX) và trợ lý thông minh độc quyền cho các dòng điện thoại Vivo / iQOO chạy OriginOS.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = IsleColors.TextSecondary,
                    )
                    Text(
                        "Các tính năng nổi bật:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = IsleColors.Text,
                    )
                    Text(
                        "1. Chuyển tiếp Đảo thông minh (SuperX):\n" +
                        "Bắt và truyền phát thông báo từ hệ thống và bên thứ ba (tin nhắn, cuộc gọi, OTP, media âm nhạc, trạng thái sạc...) lên đảo mượt mà.\n\n" +
                        "2. Cuộc gọi đến thu nhỏ trên Đảo:\n" +
                        "Hiển thị cuộc gọi nhỏ gọn trên đảo kèm nút Trả lời / Từ chối, không bung chiếm toàn màn hình khi đang dùng máy.\n\n" +
                        "3. Giám sát RAM thời gian thực:\n" +
                        "Đo lường bộ nhớ RAM thực tế của máy theo thời gian thực và cập nhật liên tục trên giao diện cũng như trên đảo.\n\n" +
                        "4. Thẻ chờ đa dạng (Idle Cards):\n" +
                        "Nhiều kiểu hiển thị khi không có thông báo: Chấm tròn (Dot), Địa cầu (Earth), RAM thời gian thực, Tốc độ mạng, Dự báo thời tiết, Sóng nhạc, Trợ lý ảo.\n\n" +
                        "5. Cầu nối Trợ lý AI (Hi Jovi & Phím AI):\n" +
                        "Bắt tín hiệu phím AI hoặc giọng nói \"Hi Jovi\" để kích hoạt nhanh Gemini Voice hoặc Circle to Search một chạm.\n\n" +
                        "6. Công cụ tối ưu hóa Vivo & iQOO:\n" +
                        "Cấp quyền Doze Whitelist & AppOps qua Shizuku/ADB để giữ đảo chạy ngầm vĩnh viễn (thoát hẳn đa nhiệm vẫn chạy) và fix trễ thông báo triệt để cho dòng máy nội địa.\n\n" +
                        "7. Tương thích toàn diện OriginOS:\n" +
                        "Tương thích ổn định từ OriginOS 4, OriginOS 5 đến OriginOS 6 (Android 16) và sẵn sàng cho OriginOS 7.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = IsleColors.Text,
                    )
                    Box(
                        Modifier.fillMaxWidth().height(1.dp).background(IsleColors.SurfaceHigh)
                    )
                    Text(
                        "Lời tri ân & Kế thừa:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = IsleColors.Good,
                    )
                    Text(
                        "Chúng tôi chân thành biết ơn và kế thừa nền tảng từ tác giả Origin Isle & cộng đồng nguồn mở.\n" +
                        "Bản quyền phát triển © LocTM.\n" +
                        "Website: https://ltmprofile.pages.dev/",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = IsleColors.TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { aboutOpen = false }) { Text(Lang.s(context, "close")) }
            },
        )
    }

    if (updateDialogOpen && updateAvailableData != null) {
        val data = updateAvailableData!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (!isDownloadingUpdate) updateDialogOpen = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(IsleIcons.Heart, contentDescription = null, tint = IsleColors.Accent, modifier = Modifier.size(24.dp))
                    Text(
                        if (appLang == "vi") "Bản cập nhật mới: v${data.latest}" else "New Update: v${data.latest}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (appLang == "vi") "Phiên bản hiện tại: v${data.current}  ➔  Mới nhất: v${data.latest}"
                        else "Current: v${data.current}  ➔  Latest: v${data.latest}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IsleColors.Accent
                    )
                    Text(
                        if (appLang == "vi") "Ghi chú phát hành:" else "Release Notes:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = IsleColors.Text
                    )
                    Text(
                        data.releaseNotes,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = IsleColors.TextSecondary
                    )
                    if (isDownloadingUpdate) {
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = IsleColors.Accent,
                            trackColor = IsleColors.SurfaceHigh,
                        )
                        Text(
                            if (appLang == "vi") "Đang tải xuống bản cập nhật... $downloadProgress%"
                            else "Downloading update... $downloadProgress%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = IsleColors.Accent
                        )
                    }
                    downloadError?.let {
                        Text(it, fontSize = 12.sp, color = IsleColors.Danger)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isDownloadingUpdate) return@TextButton
                        isDownloadingUpdate = true
                        downloadError = null
                        coroutineScope.launch {
                            val res = UpdateChecker.downloadAndInstall(
                                context = context,
                                downloadUrl = data.downloadUrl,
                                expectedSize = data.apkSize,
                                onProgress = { downloadProgress = it }
                            )
                            if (res.isFailure) {
                                isDownloadingUpdate = false
                                downloadError = if (appLang == "vi") "Lỗi khi tải hoặc xác minh: ${res.exceptionOrNull()?.localizedMessage}"
                                               else "Download failed: ${res.exceptionOrNull()?.localizedMessage}"
                            }
                        }
                    },
                    enabled = !isDownloadingUpdate
                ) {
                    Text(
                        if (isDownloadingUpdate) (if (appLang == "vi") "Đang tải $downloadProgress%" else "Downloading...")
                        else (if (appLang == "vi") "Tải & Cài đặt ngay" else "Download & Install"),
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.Accent
                    )
                }
            },
            dismissButton = {
                if (!isDownloadingUpdate) {
                    TextButton(onClick = { updateDialogOpen = false }) {
                        Text(if (appLang == "vi") "Để sau" else "Later", color = IsleColors.TextTertiary)
                    }
                }
            }
        )
    }

    if (optimizerOpen) {
        VivoOptimizerDialog(context = context, onDismiss = { optimizerOpen = false })
    }

    if (callBlockDialogOpen) {
        BlockCallFullScreenDialog(context = context, onDismiss = { callBlockDialogOpen = false })
    }

    if (idleCardEditOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { idleCardEditOpen = false },
            title = { Text(Lang.s(context, "idle_card_text"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = idleTitle,
                        onValueChange = { idleTitle = it.take(32) },
                        label = { Text(Lang.s(context, "title")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = idleText,
                        onValueChange = { idleText = it.take(64) },
                        label = { Text(Lang.s(context, "content")) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { idleTitle = "LTMori"; idleText = "Đang chờ" }) {
                        Text(Lang.s(context, "default_title"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit()
                        .putString("ltm_idle_card_title", idleTitle.trim().ifEmpty { "LTMori" })
                        .putString("ltm_idle_card_text", idleText.trim())
                        .apply()
                    LtmIslandHelper.syncIdleTicker(context)
                    LtmIslandHelper.updateIdlePreset(context)
                    idleCardEditOpen = false
                }) { Text(Lang.s(context, "save")) }
            },
            dismissButton = {
                TextButton(onClick = { idleCardEditOpen = false }) { Text(Lang.s(context, "cancel")) }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // =========================================================================
            // --- TAB 0: ĐẢO (ISLAND MAIN, DONATE TOP, COMPACT RAM, TESTER, FOOTER) ---
            // =========================================================================
            if (selectedTab == 0) {
                item {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "LTMori",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.7).sp,
                                lineHeight = 36.sp,
                                color = IsleColors.Text,
                            )
                            StatusPill(context, connected, onClick = onOpenActivity)
                        }
                        Box {
                            CircleIconButton(IsleIcons.More, Lang.s(context, "more"), onClick = { menuOpen = true })
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                offset = DpOffset(0.dp, 8.dp),
                                shape = RoundedCornerShape(18.dp),
                                containerColor = IsleColors.Menu,
                            ) {
                                MenuItem(Lang.s(context, "check_updates")) {
                                    menuOpen = false
                                    updateStatus = if (appLang == "vi") "Đang kiểm tra máy chủ GitHub..." else "Checking GitHub releases..."
                                    coroutineScope.launch {
                                        when (val res = UpdateChecker.check(context)) {
                                            is UpdateChecker.Result.UpToDate -> {
                                                updateStatus = if (appLang == "vi") "Bạn đang ở phiên bản LTMori mới nhất (${res.current})."
                                                               else "You're on the latest version (${res.current})."
                                            }
                                            is UpdateChecker.Result.UpdateAvailable -> {
                                                updateStatus = null
                                                updateAvailableData = res
                                                downloadProgress = 0
                                                downloadError = null
                                                isDownloadingUpdate = false
                                                updateDialogOpen = true
                                            }
                                            is UpdateChecker.Result.Error -> {
                                                updateStatus = if (appLang == "vi") "Kiểm tra thất bại: ${res.message}"
                                                               else "Update check failed: ${res.message}"
                                            }
                                        }
                                    }
                                }
                                MenuItem(Lang.s(context, "vivo_optimizer")) {
                                    menuOpen = false
                                    optimizerOpen = true
                                }
                                MenuItem(Lang.s(context, "about_app")) {
                                    menuOpen = false
                                    aboutOpen = true
                                }
                                MenuItem(Lang.s(context, "permissions")) {
                                    menuOpen = false
                                    permissionsOpen = true
                                }
                            }
                        }
                    }
                    updateStatus?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                            fontSize = 12.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                }

                if (!listenerOk || !batteryOk) {
                    item { SetupAttentionBanner(onClick = { permissionsOpen = true }) }
                }

                // 0. THÔNG BÁO TỪ NHÀ PHÁT TRIỂN
                item {
                    GroupCard {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(IsleColors.TileAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        IsleIcons.Heart,
                                        contentDescription = null,
                                        tint = IsleColors.TileAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Thông báo từ Nhà Phát Triển",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IsleColors.Text
                                    )
                                    Text(
                                        "LocTM · tranlocbtr99@gmail.com",
                                        fontSize = 12.sp,
                                        color = IsleColors.TextSecondary
                                    )
                                }
                            }
                            Text(
                                "Cảm ơn các bạn đã đồng hành, do kinh phí hạn chế nên NPT sẽ cập nhật chậm hơn ở các phiên bản sau, mọi ý kiến đóng góp xin gửi về gmail: tranlocbtr99@gmail.com",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = IsleColors.Text
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(IsleColors.Accent.copy(alpha = 0.15f))
                                        .clickable {
                                            runCatching {
                                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:tranlocbtr99@gmail.com")
                                                    putExtra(Intent.EXTRA_SUBJECT, "Góp ý LTMori")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(emailIntent)
                                            }.onFailure {
                                                android.widget.Toast.makeText(context, "Không mở được ứng dụng Email", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Gửi phản hồi Email",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IsleColors.Accent
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(IsleColors.SurfaceHigh)
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Email NPT", "tranlocbtr99@gmail.com"))
                                            android.widget.Toast.makeText(context, "Đã sao chép: tranlocbtr99@gmail.com", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Sao chép Gmail",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IsleColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. ỦNG HỘ TÁC GIẢ (ĐƯA LÊN TRÊN CÙNG THEO YÊU CẦU)
                item {
                    GroupCard {
                        Column {
                            NavRow(
                                IsleIcons.Heart, IsleColors.TileAmber,
                                Lang.s(context, "donate"),
                                if (donateExpanded) Lang.s(context, "collapse") else Lang.s(context, "expand"),
                            ) {
                                donateExpanded = !donateExpanded
                            }
                            if (donateExpanded) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    // Glass Sub-card for Bank Transfer
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(IsleColors.SurfaceHigh.copy(alpha = 0.5f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    "Techcombank · Trần Minh Lộc",
                                                    fontSize = 12.sp,
                                                    color = IsleColors.TextSecondary
                                                )
                                                Text(
                                                    "4444444790",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IsleColors.Text
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(IsleColors.Accent.copy(alpha = 0.2f))
                                                    .clickable {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("STK", "4444444790"))
                                                        android.widget.Toast.makeText(context, Lang.s(context, "copied"), android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Sao chép", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.AccentText)
                                            }
                                        }
                                    }

                                    // iOS Liquid Glass Pill Button for Contact Author
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(IsleColors.SurfaceHigh.copy(alpha = 0.4f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ltmprofile.pages.dev/")))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                IsleIcons.Globe,
                                                contentDescription = null,
                                                tint = IsleColors.Accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "Trang cá nhân & Liên hệ tác giả",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = IsleColors.Text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            IsleIcons.ChevronRight,
                                            contentDescription = null,
                                            tint = IsleColors.Chevron,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. BỘ NHỚ RAM TRONG APP (LÀM NHỎ GỌN LẠI)
                item {
                    CompactRamCard(context)
                }

                // 3. CÔNG TẮC BẬT/TẮT TRUYỀN LÊN ĐẢO
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(Lang.s(context, "send_to_island"))
                        GroupCard {
                            ToggleRow(
                                IsleIcons.Bell, IsleColors.TileBlue,
                                Lang.s(context, "notifications"), castOn,
                                subtitle = Lang.s(context, "cast_notifications_subtitle"),
                            ) {
                                castOn = it
                                prefs.edit().putBoolean("cast_notifications", it).apply()
                                if (it) PlaygroundService.keepAlive(context)
                                PrivacyCard.refresh()
                                FlashlightCard.refresh()
                                EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Music, IsleColors.TileOrange,
                                Lang.s(context, "media"), mediaOn,
                                subtitle = Lang.s(context, "cast_media_subtitle"),
                            ) {
                                mediaOn = it
                                prefs.edit().putBoolean("cast_media_sessions", it).apply()
                                if (it) PlaygroundService.keepAlive(context)
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Send, IsleColors.TilePurple,
                                Lang.s(context, "idle_island_toggle"), idleIslandOn,
                                subtitle = Lang.s(context, "idle_island_toggle_subtitle"),
                            ) {
                                idleIslandOn = it
                                LtmIslandHelper.setIdleCardEnabled(context, it)
                            }
                        }
                    }
                }

                // 4. THỬ NGHIỆM HIỂN THỊ TRÊN ĐẢO (DANH SÁCH GỘP TOÀN BỘ THẺ MẪU & THẺ CHỜ)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(Lang.s(context, "sample_tester_title"))
                        SampleTester(context) {
                            idleIslandOn = prefs.getBoolean("ltm_idle_card_enabled", true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(
                                Lang.s(context, "recast_all"), onClick = { recastAll(context) },
                                modifier = Modifier.weight(1f), icon = IsleIcons.Refresh,
                                enabled = castOn || mediaOn,
                            )
                            PillButton(
                                Lang.s(context, "stop_all"), onClick = { stopAll(context) },
                                modifier = Modifier.weight(1f), icon = IsleIcons.Stop, iconSize = 16.dp,
                                content = IsleColors.Danger,
                            )
                        }
                    }
                }

                // 5. CUỐI CÙNG: TUYÊN BỐ BẢN QUYỀN & CHÍNH SÁCH BẢO MẬT
                item {
                    GroupCard {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "LTMori v3.1.8",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = IsleColors.Text,
                            )
                            Text(
                                "© LocTM · Tương thích Vivo OriginOS 4, 5, 6 & 7",
                                fontSize = 11.sp,
                                color = IsleColors.TextSecondary,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Quy tắc bảo mật",
                                    fontSize = 12.sp,
                                    color = IsleColors.AccentText,
                                    modifier = Modifier
                                        .clickable { privacyPolicyOpen = true }
                                        .padding(vertical = 4.dp)
                                )
                                Text("•", fontSize = 12.sp, color = IsleColors.TextTertiary)
                                Text(
                                    "Liên hệ tác giả",
                                    fontSize = 12.sp,
                                    color = IsleColors.AccentText,
                                    modifier = Modifier
                                        .clickable {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ltmprofile.pages.dev/")))
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // --- TAB 1: THÔNG BÁO (NOTIFICATION STREAM SETTINGS) ---
            // =========================================================================
            if (selectedTab == 1) {
                item {
                    Column(
                        Modifier.alpha(if (castOn) 1f else 0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel(Lang.s(context, "notifications"))
                        GroupCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SegmentedControl(
                                    listOf(Lang.s(context, "live_only"), Lang.s(context, "everything")),
                                    selected = if (includeMessages) 1 else 0,
                                    onSelect = {
                                        includeMessages = it == 1
                                        prefs.edit().putBoolean("cast_include_messages", includeMessages).apply()
                                    },
                                )
                                Text(
                                    if (includeMessages) Lang.s(context, "every_notif_desc")
                                    else Lang.s(context, "only_live_desc"),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    fontSize = 13.sp,
                                    color = IsleColors.TextSecondary,
                                )
                            }
                            if (includeMessages) {
                                GroupDivider()
                                SettingRow(
                                    IsleIcons.Clock, IsleColors.TileTeal,
                                    Lang.s(context, "hide_after"),
                                    subtitle = Lang.s(context, "non_live_cards"),
                                )
                                Row(
                                    Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf(
                                        0 to Lang.s(context, "never"),
                                        5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s"
                                    ).forEach { (sec, label) ->
                                        ChoiceChip(
                                            label,
                                            selected = autoDismissSec == sec,
                                            onClick = {
                                                autoDismissSec = sec
                                                prefs.edit().putInt("cast_auto_dismiss_seconds", sec).apply()
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Moon, IsleColors.TilePurple,
                                Lang.s(context, "skip_silent"), ignoreSilent,
                                subtitle = Lang.s(context, "skip_silent_subtitle"),
                            ) {
                                ignoreSilent = it
                                prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                            }
                            GroupDivider()
                            SettingRow(
                                IsleIcons.Send, IsleColors.TileBlue,
                                Lang.s(context, "banner_style"),
                                subtitle = Lang.s(context, "banner_style_subtitle"),
                            )
                            Row(
                                Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val isViLang = Lang.isVi(context)
                                listOf(
                                    "liquid_glass" to (if (isViLang) "Kính lỏng iOS" else "Liquid Glass"),
                                    "superx" to (if (isViLang) "Đảo mặc định" else "Vivo Native"),
                                    "capsule_only" to (if (isViLang) "Chỉ con nhộng" else "Capsule Only"),
                                ).forEach { (style, label) ->
                                    ChoiceChip(
                                        label,
                                        selected = notifBannerStyle == style,
                                        onClick = {
                                            notifBannerStyle = style
                                            prefs.edit().putString("notif_banner_style", style).apply()
                                            if (style == "liquid_glass" && !android.provider.Settings.canDrawOverlays(context)) {
                                                runCatching {
                                                    val overlayIntent = android.content.Intent(
                                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        android.net.Uri.parse("package:${context.packageName}")
                                                    )
                                                    context.startActivity(overlayIntent)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            if (notifBannerStyle == "liquid_glass") {
                                val isViLang = Lang.isVi(context)
                                GroupDivider()
                                SettingRow(
                                    IsleIcons.Sparkles, IsleColors.TilePurple,
                                    Lang.s(context, "liquid_glass_transparency"),
                                    subtitle = if (isViLang) "Kéo để chỉnh độ trong suốt: $liquidGlassTransparency%" else "Drag to adjust transparency: $liquidGlassTransparency%",
                                )
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 62.dp, end = 20.dp, bottom = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        if (isViLang) "Đậm 10%" else "10%",
                                        fontSize = 11.sp,
                                        color = IsleColors.TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Slider(
                                        value = liquidGlassTransparency.toFloat(),
                                        onValueChange = { newVal ->
                                            liquidGlassTransparency = kotlin.math.round(newVal).toInt()
                                            prefs.edit().putInt("liquid_glass_transparency", liquidGlassTransparency).apply()
                                        },
                                        valueRange = 10f..100f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = IsleColors.TilePurple,
                                            activeTrackColor = IsleColors.TilePurple,
                                            inactiveTrackColor = IsleColors.SurfaceHigher,
                                        ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isViLang) "100% Trong" else "100%",
                                        fontSize = 11.sp,
                                        color = IsleColors.TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            if (notifBannerStyle == "superx") {
                                GroupDivider()
                                ToggleRow(
                                    IsleIcons.Pulse, IsleColors.TileBlue,
                                    Lang.s(context, "expand_on_notif"), expandOnNotification,
                                    subtitle = Lang.s(context, "expand_on_notif_subtitle"),
                                ) {
                                    expandOnNotification = it
                                    prefs.edit().putBoolean("cast_expand_on_notification", it).apply()
                                }
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Pulse, IsleColors.TilePurple,
                                Lang.s(context, "dedup_same_app"), dedupSameApp,
                                subtitle = Lang.s(context, "dedup_same_app_subtitle"),
                            ) {
                                dedupSameApp = it
                                prefs.edit().putBoolean("cast_dedup_same_app", it).apply()
                            }
                            if (dedupSameApp) {
                                Row(
                                    Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf(5 to "5s", 10 to "10s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                                        ChoiceChip(
                                            label,
                                            selected = dedupCooldown == sec,
                                            onClick = {
                                                dedupCooldown = sec
                                                prefs.edit().putInt("cast_dedup_cooldown_sec", sec).apply()
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Send, IsleColors.TilePurple,
                                Lang.s(context, "hide_from_shade"), hideFromShade,
                                subtitle = Lang.s(context, "hide_from_shade_subtitle"),
                            ) {
                                hideFromShade = it
                                prefs.edit().putBoolean("cast_hide_from_shade", it)
                                    .putBoolean("ltmori_message_island_only", it).apply()
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Lock, IsleColors.TileGreen,
                                Lang.s(context, "lockscreen"), lockscreenLiveCard,
                                subtitle = Lang.s(context, "lockscreen_subtitle"),
                            ) {
                                lockscreenLiveCard = it
                                prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                                if (LtmIslandHelper.isIdleCardEnabled(context)) {
                                    LtmIslandHelper.postIdleCard(context)
                                }
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Phone, IsleColors.TileGreen,
                                Lang.s(context, "call_island_only"), callIslandOnly,
                                subtitle = Lang.s(context, "call_island_only_subtitle"),
                            ) {
                                callIslandOnly = it
                                prefs.edit().putBoolean("cast_call_island_only", it).apply()
                            }
                            if (callIslandOnly) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 62.dp, end = 16.dp, bottom = 12.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(IsleColors.TileGreen.copy(alpha = 0.15f))
                                        .border(0.5.dp, IsleColors.TileGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { callBlockDialogOpen = true }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "Cài đặt ứng dụng chặn toàn màn hình…",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IsleColors.GoodText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // --- TAB 2: TUỲ CHỈNH (CUSTOM CARDS, GEMINI & QUIET HOURS) ---
            // =========================================================================
            if (selectedTab == 2) {
                // ĐÃ XÓA MỤC "KIỂU THẺ CHỜ TRÊN ĐẢO" TRÙNG LẶP THEO YÊU CẦU
                // CHỈ GIỮ LẠI TÙY CHỈNH NỘI DUNG THẺ CHỜ & CÁC LOẠI THẺ
                item {
                    Column(
                        Modifier.alpha(if (castOn) 1f else 0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel(Lang.s(context, "idle_island_toggle"))
                        GroupCard {
                            ToggleRow(
                                IsleIcons.Send, IsleColors.TilePurple,
                                Lang.s(context, "idle_island_toggle"), idleIslandOn,
                                subtitle = Lang.s(context, "idle_island_toggle_subtitle"),
                            ) {
                                idleIslandOn = it
                                LtmIslandHelper.setIdleCardEnabled(context, it)
                            }
                            if (idleIslandOn) {
                                GroupDivider()
                                NavRow(
                                    IsleIcons.Send, IsleColors.TilePurple,
                                    Lang.s(context, "idle_card_text"),
                                    Lang.s(context, "edit"),
                                ) {
                                    idleTitle = prefs.getString("ltm_idle_card_title", "LTMori").orEmpty()
                                    idleText = prefs.getString("ltm_idle_card_text", "Đang chờ").orEmpty()
                                    idleCardEditOpen = true
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier.alpha(if (castOn) 1f else 0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel(Lang.s(context, "cards_section"))
                        GroupCard {
                            ToggleRow(
                                IsleIcons.Key, IsleColors.TileBlue,
                                Lang.s(context, "otp_codes"), otpOn,
                                subtitle = Lang.s(context, "otp_codes_subtitle"),
                            ) {
                                otpOn = it
                                prefs.edit().putBoolean("cast_otp", it).apply()
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Buttons, IsleColors.TileTeal,
                                Lang.s(context, "actions"), actionsOn,
                                subtitle = Lang.s(context, "actions_subtitle"),
                            ) {
                                actionsOn = it
                                prefs.edit().putBoolean("cast_show_actions", it).apply()
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Music, IsleColors.TileBlue,
                                Lang.s(context, "live_lyrics"), liveLyricsOn,
                                subtitle = Lang.s(context, "live_lyrics_subtitle"),
                            ) {
                                liveLyricsOn = it
                                prefs.edit().putBoolean("cast_live_lyrics_opt_in", it).apply()
                                if (!it) LtmIslandHelper.stopLiveLyrics()
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Bolt, IsleColors.TileGreen,
                                Lang.s(context, "charging"), chargingOn,
                                subtitle = Lang.s(context, "charging_subtitle"),
                            ) {
                                chargingOn = it
                                prefs.edit().putBoolean("cast_charging", it).apply()
                                ChargingCard.onSettingsChanged(context)
                            }
                            if (chargingOn) {
                                Row(
                                    Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf(false to Lang.s(context, "briefly"), true to Lang.s(context, "while_charging")).forEach { (keep, label) ->
                                        ChoiceChip(
                                            label,
                                            selected = chargingKeep == keep,
                                            onClick = {
                                                chargingKeep = keep
                                                prefs.edit().putBoolean("cast_charging_keep", keep).apply()
                                                ChargingCard.onSettingsChanged(context)
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                if (android.os.Build.VERSION.SDK_INT >= 36) {
                                    ToggleRow(
                                        IsleIcons.Bolt, IsleColors.TileGreen,
                                        Lang.s(context, "live_updates_charging"), chargingLiveUpdates,
                                        subtitle = Lang.s(context, "live_updates_charging_subtitle"),
                                    ) {
                                        chargingLiveUpdates = it
                                        prefs.edit().putBoolean("cast_charging_live_updates", it).apply()
                                        if (!it) LtmIslandHelper.dismissChargingLive(context)
                                    }
                                }
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Camera, IsleColors.TileAmber,
                                Lang.s(context, "privacy_dot"), privacyDotOn,
                                subtitle = Lang.s(context, "privacy_dot_subtitle"),
                            ) {
                                privacyDotOn = it
                                prefs.edit().putBoolean("cast_privacy_dot", it).apply()
                                PrivacyCard.refresh()
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Headphones, IsleColors.TilePurple,
                                Lang.s(context, "earbuds"), earbudsOn,
                                subtitle = Lang.s(context, "earbuds_subtitle"),
                            ) {
                                prefs.edit().putBoolean("cast_earbuds", it).apply()
                                if (it && !EarbudsCard.hasPermission(context)) {
                                    bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    earbudsOn = it
                                    EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier.alpha(if (castOn) 1f else 0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel(Lang.s(context, "ai_assistant_title"))
                        GroupCard {
                            ToggleRow(
                                IsleIcons.Check, IsleColors.TileBlue,
                                Lang.s(context, "gemini_tap"), geminiTap,
                                subtitle = Lang.s(context, "gemini_tap_subtitle"),
                            ) {
                                geminiTap = it
                                prefs.edit().putBoolean("ltm_idle_gemini_tap", it).apply()
                                LtmIslandHelper.postIdleCard(context)
                            }
                            GroupDivider()

                            Text(
                                Lang.s(context, "select_assistant_mode"),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsleColors.TextSecondary,
                            )

                            // Option 1: Gemini Voice
                            AssistantSelectRow(
                                title = "Gemini Voice",
                                subtitle = Lang.s(context, "gemini_voice_desc"),
                                selected = voiceAction != "GOOGLE_CTS",
                                onClick = {
                                    voiceAction = "GEMINI_VOICE"
                                    bridgePrefs.edit().putString("voice_action", "GEMINI_VOICE").apply()
                                    if (geminiTap) LtmIslandHelper.postIdleCard(context)
                                },
                            )

                            GroupDivider()

                            // Option 2: Circle to Search
                            AssistantSelectRow(
                                title = "Circle to Search",
                                subtitle = Lang.s(context, "cts_desc"),
                                selected = voiceAction == "GOOGLE_CTS",
                                onClick = {
                                    voiceAction = "GOOGLE_CTS"
                                    bridgePrefs.edit().putString("voice_action", "GOOGLE_CTS").apply()
                                    if (geminiTap) LtmIslandHelper.postIdleCard(context)
                                },
                            )

                            GroupDivider()

                            // Balanced full-width test button
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                PillButton(
                                    if (voiceAction == "GOOGLE_CTS") Lang.s(context, "test_cts")
                                    else Lang.s(context, "test_gemini"),
                                    onClick = {
                                        if (voiceAction == "GOOGLE_CTS") {
                                            LtmIslandHelper.launchCircleToSearch(context)
                                        } else {
                                            LtmIslandHelper.launchGemini(context)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = IsleIcons.Sparkles,
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier.alpha(if (castOn) 1f else 0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel(Lang.s(context, "quiet_hours"))
                        GroupCard {
                            ToggleRow(
                                IsleIcons.Moon, IsleColors.TilePurple,
                                Lang.s(context, "quiet_dnd"), quietDnd,
                                subtitle = Lang.s(context, "quiet_dnd_subtitle"),
                            ) {
                                quietDnd = it
                                prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                            }
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Clock, IsleColors.TileGray,
                                Lang.s(context, "schedule"), quietSchedule,
                                subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)}",
                            ) {
                                quietSchedule = it
                                prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                            }
                            if (quietSchedule) {
                                Row(
                                    Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    ChoiceChip(
                                        "${QuietHours.format(quietStart)}",
                                        selected = false,
                                        onClick = {
                                            pickTime(context, quietStart) {
                                                quietStart = it
                                                prefs.edit().putInt("cast_quiet_start", it).apply()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    ChoiceChip(
                                        "${QuietHours.format(quietEnd)}",
                                        selected = false,
                                        onClick = {
                                            pickTime(context, quietEnd) {
                                                quietEnd = it
                                                prefs.edit().putInt("cast_quiet_end", it).apply()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        Text(
                            Lang.s(context, "quiet_hours_desc"),
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                }
            }

            // =========================================================================
            // --- TAB 3: CÀI ĐẶT (SETTINGS & BRIDGE & APPS) ---
            // =========================================================================
            if (selectedTab == 3) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(Lang.s(context, "tab_settings"))
                        GroupCard {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(IsleColors.TileTeal.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(IsleIcons.More, contentDescription = null, tint = IsleColors.TileTeal, modifier = Modifier.size(16.dp))
                                    }
                                    Text(Lang.s(context, "language"), color = IsleColors.Text, fontWeight = FontWeight.Medium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("vi" to "VI", "en" to "EN").forEach { (code, label) ->
                                        FilterChip(
                                            selected = appLang == code,
                                            onClick = {
                                                appLang = code
                                                Lang.setLang(context, code)
                                            },
                                            label = { Text(label) },
                                        )
                                    }
                                }
                            }
                            GroupDivider()
                            NavRow(
                                IsleIcons.Send, IsleColors.TileBlue,
                                Lang.s(context, "hi_jovi_bridge"),
                                Lang.s(context, "open"),
                            ) {
                                runCatching {
                                    context.startActivity(
                                        Intent().setComponent(
                                            ComponentName("com.autonavi.minimap", "com.aikeybridge.MainActivity")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            GroupDivider()
                            NavRow(
                                IsleIcons.Grid, IsleColors.TileAmber,
                                Lang.s(context, "apps"),
                                value = if (appsOff == 0) Lang.s(context, "apps_all_on") else "$appsOff ${Lang.s(context, "apps_off")}",
                                onClick = onOpenApps,
                            )
                            GroupDivider()
                            NavRow(
                                IsleIcons.Pulse, IsleColors.TileGray,
                                Lang.s(context, "activity"),
                                onClick = onOpenActivity,
                            )
                            GroupDivider()
                            NavRow(
                                IsleIcons.More, IsleColors.TileTeal,
                                Lang.s(context, "permissions"),
                                onClick = { permissionsOpen = true },
                            )
                            GroupDivider()
                            NavRow(
                                IsleIcons.Bolt, IsleColors.TileAmber,
                                Lang.s(context, "vivo_optimizer"),
                                Lang.s(context, "open"),
                                onClick = { optimizerOpen = true },
                            )
                            GroupDivider()
                            ToggleRow(
                                IsleIcons.Lock, IsleColors.TileGreen,
                                Lang.s(context, "bank_guard_title"), bankGuardOn,
                                subtitle = Lang.s(context, "bank_guard_subtitle"),
                            ) {
                                bankGuardOn = it
                                com.aikeybridge.guard.BankGuard.setEnabled(context, it)
                            }
                            if (bankGuardOn) {
                                if (!com.aikeybridge.guard.BankGuard.hasUsageAccess(context)) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 62.dp, end = 16.dp, bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(IsleColors.TileAmber.copy(alpha = 0.15f))
                                                .border(0.5.dp, IsleColors.TileAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                .clickable { com.aikeybridge.guard.BankGuard.openUsageSettings(context) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                Lang.s(context, "bank_guard_grant_perm"),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = IsleColors.Warn
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 62.dp, end = 16.dp, bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            Lang.s(context, "bank_guard_list_desc"),
                                            fontSize = 11.sp,
                                            color = IsleColors.TileGreen
                                        )
                                    }
                                }
                            }
                            GroupDivider()
                            NavRow(
                                IsleIcons.Heart, IsleColors.TilePurple,
                                Lang.s(context, "about_app"),
                                Lang.s(context, "open"),
                                onClick = { aboutOpen = true },
                            )
                        }
                    }
                }
            }
        }

        // Bottom navigation bar (iOS-style glass pill)
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(IsleColors.Surface.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val icons = listOf(IsleIcons.Bolt, IsleIcons.Bell, IsleIcons.Grid, IsleIcons.More)
                val labels = listOf(
                    Lang.s(context, "tab_island"),
                    Lang.s(context, "tab_notifications"),
                    Lang.s(context, "tab_customize"),
                    Lang.s(context, "tab_settings"),
                )
                labels.forEachIndexed { i, label ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == i) IsleColors.Accent.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedTab = i }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            icons[i],
                            contentDescription = label,
                            tint = if (selectedTab == i) IsleColors.Accent else IsleColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == i) IsleColors.Accent else IsleColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** Compact real-time RAM monitor card (takes minimal vertical height) */
@Composable
private fun CompactRamCard(context: Context) {
    val actManager = remember { context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager }
    var totalMemMb by remember { mutableLongStateOf(0L) }
    var availMemMb by remember { mutableLongStateOf(0L) }
    var usedMemMb by remember { mutableLongStateOf(0L) }
    var percent by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        while (true) {
            actManager?.getMemoryInfo(memInfo)
            val total = memInfo.totalMem / (1024 * 1024)
            val avail = memInfo.availMem / (1024 * 1024)
            val used = (total - avail).coerceAtLeast(0L)
            val pct = if (total > 0) ((used * 100) / total).toInt() else 0
            totalMemMb = total
            availMemMb = avail
            usedMemMb = used
            percent = pct
            delay(1200L)
        }
    }

    val statusText = when {
        percent < 65 -> Lang.s(context, "ram_status_smooth")
        percent < 85 -> Lang.s(context, "ram_status_normal")
        else -> Lang.s(context, "ram_status_high")
    }
    val progressColor = when {
        percent < 65 -> IsleColors.Good
        percent < 85 -> IsleColors.TileAmber
        else -> IsleColors.Danger
    }

    GroupCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IsleColors.TileTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(IsleIcons.Pulse, contentDescription = null, tint = IsleColors.TileTeal, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        "RAM: ${String.format(java.util.Locale.US, "%.1f", usedMemMb / 1024.0)} / ${String.format(java.util.Locale.US, "%.1f GB", totalMemMb / 1024.0)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IsleColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$statusText · $percent%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            // Compact progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }
        }
    }
}

/** Status pill indicating listening state */
@Composable
private fun StatusPill(context: Context, connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) IsleColors.Good else IsleColors.Bad))
        Text(
            if (connected) Lang.s(context, "listening") else Lang.s(context, "not_listening"),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (connected) IsleColors.GoodText else IsleColors.BadText,
        )
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = IsleColors.Text) },
        onClick = onClick,
        modifier = Modifier.widthIn(min = 180.dp).heightIn(min = 44.dp),
    )
}

/** Island preview + sample picker + send button: fires any [IslandSamples] card on demand. */
@Composable
private fun SampleTester(context: Context, onSampleSent: () -> Unit = {}) {
    var selected by remember { mutableIntStateOf(0) }
    val sample = IslandSamples.all.getOrElse(selected) { IslandSamples.all.first() }
    var sentAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sentAt) {
        if (sentAt != 0L) { delay(1600); sentAt = 0L }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.Surface)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(IsleColors.PreviewBg)
                .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IslandPreviewView(sample.preview)
            Text(
                sample.summary,
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.PreviewLabel,
                textAlign = TextAlign.Center,
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(IslandSamples.all) { i, s ->
                ChoiceChip(
                    s.name,
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContainer = IsleColors.Text,
                    selectedContent = IsleColors.Background,
                    shape = RoundedCornerShape(16.dp),
                    height = 34.dp,
                    horizontalPadding = 12.dp,
                )
            }
        }
        PillButton(
            if (sentAt != 0L) Lang.s(context, "sent") else Lang.s(context, "send_button"),
            onClick = {
                sample.post(context)
                sentAt = System.currentTimeMillis()
                onSampleSent()
            },
            modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
            icon = IsleIcons.Send,
            container = IsleColors.Accent,
            content = Color.White,
            height = 46.dp,
        )
    }
}

/** The platform time picker, in minutes since midnight, honouring the device's 12/24h setting. */
private fun pickTime(context: Context, initial: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initial / 60, initial % 60,
        android.text.format.DateFormat.is24HourFormat(context),
    ).show()
}

private fun stopAll(context: Context) {
    context.startService(Intent(context, PlaygroundService::class.java).setAction(PlaygroundService.ACTION_STOP))
}

private fun recastAll(context: Context) {
    PlaygroundService.keepAlive(context)
    val listener = NotificationCastListener.instance
    if (listener == null) {
        val message = when (NotificationCastListener.forceRebind(context)) {
            NotificationCastListener.RebindResult.NO_ACCESS ->
                if (Lang.currentLang(context) == "vi") "Chưa cấp quyền truy cập thông báo — hãy bật trong Cài đặt."
                else "Notification access isn't granted — turn it on in Settings, then tap again."
            else ->
                if (Lang.currentLang(context) == "vi") "Dịch vụ đang kết nối lại… Vui lòng thử lại sau vài giây."
                else "Listener not connected — reconnecting now. Give it a few seconds, then tap again."
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        return
    }
    val count = listener.recastAll()
    val msg = if (Lang.currentLang(context) == "vi") "Đã quét lại $count thông báo." else "Recast swept $count notifications."
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun AssistantSelectRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) IsleColors.Accent.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) IsleColors.Text else IsleColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = IsleColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) IsleColors.Accent else IsleColors.SurfaceHigher),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    IsleIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun VivoOptimizerDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val shizukuReady = remember { LtmVivoOptimizer.isShizukuAvailable(context) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showCallBlockDialog by remember { mutableStateOf(false) }
    var selectedPackages by remember {
        mutableStateOf(
            setOf(
                "com.zing.zalo",
                "com.facebook.orca",
                "org.telegram.messenger",
                "com.google.android.gm",
                "com.whatsapp",
                "com.facebook.katana"
            )
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(IsleIcons.Bolt, contentDescription = null, tint = IsleColors.TileAmber, modifier = Modifier.size(24.dp))
                Text(Lang.s(context, "vivo_optimizer"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status banner
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (shizukuReady) IsleColors.Good.copy(alpha = 0.15f) else IsleColors.TileAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (shizukuReady) IsleColors.Good else IsleColors.TileAmber)
                    )
                    Text(
                        if (shizukuReady) "Shizuku / Wireless ADB: Sẵn sàng chạy 1-chạm"
                        else "Chưa kết nối Shizuku: Hãy dùng nút Sao chép lệnh ADB bên dưới",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (shizukuReady) IsleColors.Good else IsleColors.TileAmber,
                    )
                }

                if (statusMessage != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IsleColors.SurfaceHigher)
                            .padding(10.dp)
                    ) {
                        Text(
                            statusMessage!!,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.Text,
                        )
                    }
                }

                // Mục 1: Giữ đảo luôn chạy ngầm
                GroupCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "1. Giữ đảo luôn chạy ngầm (Chống tắt khi xoá đa nhiệm)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Cấp quyền Doze Whitelist & AppOps cho LTMori, cho phép người dùng vuốt xóa app khỏi danh sách ứng dụng gần đây mà đảo vẫn hoạt động và chuyển tiếp thông báo vĩnh viễn.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                enabled = !isWorking && shizukuReady,
                                onClick = {
                                    coroutineScope.launch {
                                        isWorking = true
                                        statusMessage = "Đang cấp quyền chạy ngầm qua Shizuku…"
                                        val res = LtmVivoOptimizer.executeKeepAlive(context)
                                        statusMessage = res.message
                                        isWorking = false
                                    }
                                },
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IsleColors.Accent),
                            ) {
                                Text("Chạy 1-chạm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_KeepAlive", LtmVivoOptimizer.getKeepAliveAdbScript()))
                                    android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB giữ đảo chạy ngầm", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Sao chép ADB", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Mục 2: Fix trễ thông báo Vivo / iQOO nội địa
                GroupCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "2. Fix trễ thông báo máy nội địa iQOO / Vivo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Vô hiệu hóa cơ chế đóng băng Doze của ROM nội địa đối với Google Play Services, GSF, Zalo, Messenger, Telegram... và giữ kết nối mạng ngầm liên tục khi khoá màn hình.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                enabled = !isWorking && shizukuReady,
                                onClick = {
                                    coroutineScope.launch {
                                        isWorking = true
                                        statusMessage = "Đang tối ưu chống trễ thông báo qua Shizuku…"
                                        val res = LtmVivoOptimizer.executeFixNotificationDelay(context)
                                        statusMessage = res.message
                                        isWorking = false
                                    }
                                },
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IsleColors.Good),
                            ) {
                                Text("Fix trễ 1-chạm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_FixDelay", LtmVivoOptimizer.getFixNotificationDelayAdbScript()))
                                    android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB fix trễ thông báo", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Sao chép ADB", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Mục 3: Tối ưu pin & chạy ngầm cho ứng dụng tự chọn
                GroupCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "3. Tối ưu pin & chạy ngầm cho ứng dụng tự chọn",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Cấp quyền AppOps (RUN_IN_BACKGROUND, RUN_ANY_IN_BACKGROUND, AUTO_START) và đưa vào danh sách trắng Doze Whitelist để không bao giờ bị OriginOS đóng băng hay trễ tin nhắn.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Đã chọn: ${selectedPackages.size} ứng dụng",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = IsleColors.Text
                            )
                            OutlinedButton(
                                onClick = { showAppPicker = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Chọn ứng dụng…", fontSize = 12.sp)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                enabled = !isWorking && shizukuReady && selectedPackages.isNotEmpty(),
                                onClick = {
                                    coroutineScope.launch {
                                        isWorking = true
                                        statusMessage = "Đang tối ưu ${selectedPackages.size} ứng dụng qua Shizuku…"
                                        val res = LtmVivoOptimizer.executeOptimizeApps(context, selectedPackages.toList())
                                        statusMessage = res.message
                                        isWorking = false
                                    }
                                },
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IsleColors.TileTeal),
                            ) {
                                Text("Tối ưu 1-chạm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                enabled = selectedPackages.isNotEmpty(),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_OptimizeApps", LtmVivoOptimizer.getOptimizeAppsAdbScript(selectedPackages.toList())))
                                    android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB tối ưu ${selectedPackages.size} ứng dụng", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Sao chép ADB", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Mục 4: Chặn cuộc gọi bung toàn màn hình (Hiện trên Đảo)
                GroupCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "4. Chặn cuộc gọi bung toàn màn hình (Chỉ hiện trên Đảo)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Sử dụng quyền AppOps USE_FULL_SCREEN_INTENT để chặn Zalo, Messenger, Điện thoại bung cửa sổ toàn màn hình, đưa cuộc gọi đến hiển thị tinh tế trên Đảo Động.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.TextSecondary,
                        )
                        OutlinedButton(
                            onClick = { showCallBlockDialog = true },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cấu hình chặn cuộc gọi (Shizuku / ADB)…", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Mục 5: Ẩn thông báo Gỡ lỗi không dây
                GroupCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "5. Ẩn thông báo Gỡ lỗi không dây & Tùy chọn nhà phát triển",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsleColors.Text,
                        )
                        Text(
                            "Tắt Gỡ lỗi không dây (adb_wifi_enabled = 0) và ẩn Tùy chọn nhà phát triển (development_settings_enabled = 0) giúp xóa icon/thông báo trên thanh trạng thái và an toàn tuyệt đối với ứng dụng ngân hàng / VNeID.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                enabled = !isWorking && shizukuReady,
                                onClick = {
                                    coroutineScope.launch {
                                        isWorking = true
                                        statusMessage = "Đang tắt Gỡ lỗi & ẩn Tùy chọn nhà phát triển…"
                                        val res = LtmVivoOptimizer.executeHideWirelessDebugging(context)
                                        statusMessage = res.message
                                        isWorking = false
                                    }
                                },
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IsleColors.TilePurple),
                            ) {
                                Text("Ẩn ngay 1-chạm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_HideWifi", LtmVivoOptimizer.getHideWirelessDebuggingAdbScript()))
                                    android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB ẩn Gỡ lỗi & Tùy chọn phát triển", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Sao chép ADB", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Mục 6: Cài đặt thủ công
                Text(
                    "3 bước cài đặt thủ công trong OriginOS:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IsleColors.Text,
                )
                Text(
                    "• Bước 1: Vào Cài đặt → Pin → Mức tiêu thụ pin dưới nền cao → Bật cho LTMori, Zalo, Messenger...\n" +
                    "• Bước 2: Vào Cài đặt → Ứng dụng → Tự khởi chạy → Bật Tự khởi chạy & Khởi chạy liên kết.\n" +
                    "• Bước 3: Cài đặt → Quản lý thông báo → Bật Biểu ngữ thông báo & Màn hình khóa.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(Lang.s(context, "close")) }
        },
    )

    if (showAppPicker) {
        SelectAppsToOptimizeDialog(
            context = context,
            selectedPackages = selectedPackages,
            onDismiss = { showAppPicker = false },
            onConfirm = { selectedPackages = it }
        )
    }

    if (showCallBlockDialog) {
        BlockCallFullScreenDialog(
            context = context,
            onDismiss = { showCallBlockDialog = false }
        )
    }
}

@Composable
private fun SelectAppsToOptimizeDialog(
    context: Context,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelected by remember { mutableStateOf(selectedPackages) }
    var allApps by remember { mutableStateOf<List<LtmVivoOptimizer.AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = LtmVivoOptimizer.getInstalledAppList(context)
            withContext(Dispatchers.Main) {
                allApps = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chọn ứng dụng cần tối ưu pin", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm ứng dụng hoặc package…", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Đã chọn: ${tempSelected.size}/${allApps.size}", fontSize = 12.sp, color = IsleColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                tempSelected = filteredApps.map { it.packageName }.toSet() + tempSelected
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Chọn hết", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                val currentFilteredPkg = filteredApps.map { it.packageName }.toSet()
                                tempSelected = tempSelected - currentFilteredPkg
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Bỏ chọn", fontSize = 11.sp)
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = IsleColors.Accent)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChecked = tempSelected.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        tempSelected = if (isChecked) {
                                            tempSelected - app.packageName
                                        } else {
                                            tempSelected + app.packageName
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        tempSelected = if (checked) {
                                            tempSelected + app.packageName
                                        } else {
                                            tempSelected - app.packageName
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = IsleColors.Text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 10.sp,
                                        color = IsleColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(tempSelected)
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IsleColors.Accent)
            ) {
                Text("Xong (${tempSelected.size})", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun BlockCallFullScreenDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val shizukuReady = remember { LtmVivoOptimizer.isShizukuAvailable(context) }
    var configuredPkgs by remember {
        mutableStateOf(LtmVivoOptimizer.getConfiguredCallPackages(context))
    }
    var blockedPkgs by remember {
        mutableStateOf(LtmVivoOptimizer.getBlockedCallPackages(context))
    }
    var showAppPicker by remember { mutableStateOf(false) }

    val appNameMap = remember {
        mapOf(
            "com.zing.zalo" to "Zalo",
            "com.facebook.orca" to "Messenger",
            "org.telegram.messenger" to "Telegram",
            "com.whatsapp" to "WhatsApp",
            "com.android.phone" to "Điện thoại (OriginOS Phone)",
            "com.android.incallui" to "Điện thoại (OriginOS InCallUI)",
            "com.google.android.dialer" to "Điện thoại Google (Phone)",
            "com.vivo.incallui" to "Điện thoại Vivo (InCallUI)",
            "com.bbk.incallui" to "Điện thoại BBK (InCallUI)",
            "com.vivo.telecom" to "Viễn thông Vivo (Telecom)"
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(IsleIcons.Phone, contentDescription = null, tint = IsleColors.TileGreen, modifier = Modifier.size(24.dp))
                Text("Chặn & Khôi phục cuộc gọi toàn màn hình", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Khi bật chặn, cuộc gọi đến sẽ không bung đè toàn màn hình mà chỉ hiện gọn trên Đảo Động với 2 nút Nghe / Hủy. Tắt công tắc để khôi phục lại màn hình cuộc gọi toàn màn hình như mặc định.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = IsleColors.TextSecondary,
                )

                if (!shizukuReady) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IsleColors.SurfaceHigher)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "⚠️ Chưa kết nối Shizuku / Gỡ lỗi không dây",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = IsleColors.Accent,
                            )
                            Text(
                                "Khi gạt công tắc hoặc bấm nút nhanh, hệ thống sẽ lưu cấu hình của bạn. Để nạp lệnh có hiệu lực vào máy, hãy bấm nút 'Sao chép ADB' tương ứng để chạy qua máy tính.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = IsleColors.TextSecondary,
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IsleColors.SurfaceHigher)
                            .padding(10.dp)
                    ) {
                        Text(
                            statusMessage!!,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = IsleColors.Text,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ứng dụng (${blockedPkgs.intersect(configuredPkgs).size}/${configuredPkgs.size} đang chặn):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IsleColors.Text
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(IsleColors.SurfaceHigh)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    configuredPkgs.forEach { pkg ->
                        val isBlocked = blockedPkgs.contains(pkg)
                        val label = appNameMap[pkg] ?: runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                        val isDefault = pkg in LtmVivoOptimizer.DEFAULT_CALL_PACKAGES

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBlocked) IsleColors.TileGreen.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = IsleColors.Text
                                )
                                Text(
                                    if (isBlocked) "Đã chặn: Chỉ hiện trên Đảo (2 nút)" else "Cho phép bung toàn màn hình",
                                    fontSize = 10.sp,
                                    color = if (isBlocked) IsleColors.TileGreen else IsleColors.TextSecondary
                                )
                            }
                            if (!isDefault) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            val updated = configuredPkgs - pkg
                                            configuredPkgs = updated
                                            LtmVivoOptimizer.saveConfiguredCallPackages(context, updated)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(IsleIcons.Close, contentDescription = "Xóa", tint = IsleColors.TextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                            IsleSwitch(
                                checked = isBlocked,
                                onCheckedChange = { checked ->
                                    if (isWorking) return@IsleSwitch
                                    if (!shizukuReady) {
                                        LtmVivoOptimizer.setBlockedCallPackage(context, pkg, checked)
                                        blockedPkgs = if (checked) blockedPkgs + pkg else blockedPkgs - pkg
                                        statusMessage = "Đã lưu '${if (checked) "Chặn" else "Cho phép"}' cho $label. Hãy bấm 'Sao chép ADB' bên dưới để nạp lệnh."
                                    } else {
                                        coroutineScope.launch {
                                            isWorking = true
                                            statusMessage = "Đang ${if (checked) "chặn" else "khôi phục"} quyền cho $label…"
                                            val res = LtmVivoOptimizer.executeToggleBlockFullScreen(context, pkg, checked)
                                            if (res.success) {
                                                blockedPkgs = if (checked) blockedPkgs + pkg else blockedPkgs - pkg
                                                statusMessage = "Đã ${if (checked) "chặn toàn màn hình (chỉ hiện trên Đảo)" else "cho phép bung toàn màn hình"} cho $label!"
                                            } else {
                                                statusMessage = res.message
                                            }
                                            isWorking = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showAppPicker = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("+ Thêm ứng dụng khác…", fontSize = 12.sp)
                    }
                }

                Text("Thao tác đồng loạt:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.TextSecondary)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        enabled = !isWorking && configuredPkgs.isNotEmpty(),
                        onClick = {
                            if (!shizukuReady) {
                                LtmVivoOptimizer.setAllBlockedCallPackages(context, configuredPkgs)
                                blockedPkgs = configuredPkgs
                                statusMessage = "Đã lưu cài đặt Chặn tất cả. Hãy bấm 'Sao chép ADB (Chặn)' để nạp lệnh."
                            } else {
                                coroutineScope.launch {
                                    isWorking = true
                                    statusMessage = "Đang chặn toàn màn hình tất cả ứng dụng…"
                                    val res = LtmVivoOptimizer.executeBlockFullScreenIntent(context, configuredPkgs.toList(), block = true)
                                    if (res.success) {
                                        LtmVivoOptimizer.setAllBlockedCallPackages(context, configuredPkgs)
                                        blockedPkgs = configuredPkgs
                                    }
                                    statusMessage = res.message
                                    isWorking = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = IsleColors.TileGreen),
                    ) {
                        Text("Chặn tất cả", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        enabled = !isWorking && configuredPkgs.isNotEmpty(),
                        onClick = {
                            if (!shizukuReady) {
                                LtmVivoOptimizer.setAllBlockedCallPackages(context, emptySet())
                                blockedPkgs = emptySet()
                                statusMessage = "Đã lưu cài đặt Khôi phục tất cả. Hãy bấm 'Sao chép ADB (Khôi phục)' để nạp lệnh."
                            } else {
                                coroutineScope.launch {
                                    isWorking = true
                                    statusMessage = "Đang khôi phục toàn màn hình tất cả ứng dụng…"
                                    val res = LtmVivoOptimizer.executeBlockFullScreenIntent(context, configuredPkgs.toList(), block = false)
                                    if (res.success) {
                                        LtmVivoOptimizer.setAllBlockedCallPackages(context, emptySet())
                                        blockedPkgs = emptySet()
                                    }
                                    statusMessage = res.message
                                    isWorking = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Khôi phục tất cả", fontSize = 12.sp)
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = configuredPkgs.isNotEmpty(),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_BlockCall", LtmVivoOptimizer.getBlockFullScreenIntentAdbScript(configuredPkgs.toList(), block = true)))
                            android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB Chặn toàn màn hình", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Sao chép ADB (Chặn)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        enabled = configuredPkgs.isNotEmpty(),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_UnblockCall", LtmVivoOptimizer.getBlockFullScreenIntentAdbScript(configuredPkgs.toList(), block = false)))
                            android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB Khôi phục toàn màn hình", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Sao chép ADB (Khôi phục)", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(Lang.s(context, "close")) }
        }
    )

    if (showAppPicker) {
        SelectAppsToOptimizeDialog(
            context = context,
            selectedPackages = configuredPkgs,
            onDismiss = { showAppPicker = false },
            onConfirm = { updated ->
                configuredPkgs = updated
                LtmVivoOptimizer.saveConfiguredCallPackages(context, updated)
            }
        )
    }
}
