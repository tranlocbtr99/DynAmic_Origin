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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", false)) }
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var expandOnNotification by remember { mutableStateOf(prefs.getBoolean("cast_expand_on_notification", true)) }
    var hideFromShade by remember { mutableStateOf(prefs.getBoolean("cast_hide_from_shade", false)) }
    var dedupSameApp by remember { mutableStateOf(prefs.getBoolean("cast_dedup_same_app", true)) }
    var dedupCooldown by remember { mutableStateOf(prefs.getInt("cast_dedup_cooldown_sec", 10)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var autoDismissSec by remember { mutableStateOf(prefs.getInt("cast_auto_dismiss_seconds", 0)) }
    var otpOn by remember { mutableStateOf(prefs.getBoolean("cast_otp", true)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }
    var liveLyricsOn by remember { mutableStateOf(prefs.getBoolean("cast_live_lyrics_opt_in", false)) }
    var geminiTap by remember { mutableStateOf(prefs.getBoolean("ltm_idle_gemini_tap", false)) }
    val bridgePrefs = remember { context.getSharedPreferences("ai_key_bridge_settings", Context.MODE_PRIVATE) }
    var voiceAction by remember { mutableStateOf(bridgePrefs.getString("voice_action", "GEMINI_VOICE").orEmpty()) }
    var appLang by remember { mutableStateOf(Lang.currentLang(context)) }
    var idleCardEditOpen by remember { mutableStateOf(false) }
    var privacyPolicyOpen by remember { mutableStateOf(false) }
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
                        "• Bản quyền phần mềm © LocTM. Dựa trên mã nguồn mở Origin Isle & AI Key Bridge.",
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
                                "Origin Isle",
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
                                    coroutineScope.launch {
                                        updateStatus = Lang.s(context, "checking")
                                        updateStatus = when (val result = UpdateChecker.check(context)) {
                                            is UpdateChecker.Result.UpToDate ->
                                                if (appLang == "vi") "Bạn đang ở phiên bản mới nhất (v${result.current})."
                                                else "You're on the latest version (v${result.current})."
                                            is UpdateChecker.Result.UpdateAvailable -> {
                                                UpdateChecker.openRelease(context, result.url)
                                                if (appLang == "vi") "Có bản cập nhật mới: v${result.latest}"
                                                else "Update available: v${result.latest}"
                                            }
                                            is UpdateChecker.Result.Error -> result.message
                                        }
                                    }
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
                                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("Techcombank · Tran Minh Loc", fontSize = 13.sp, color = IsleColors.TextSecondary)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text("4444444790", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IsleColors.Text)
                                        TextButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("STK", "4444444790"))
                                            android.widget.Toast.makeText(context, Lang.s(context, "copied"), android.widget.Toast.LENGTH_SHORT).show()
                                        }) { Text(Lang.s(context, "copy")) }
                                    }
                                    TextButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ltmprofile.pages.dev/")))
                                    }) { Text(Lang.s(context, "contact_author")) }
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
                            }
                        }
                    }
                }

                // 4. THỬ NGHIỆM HIỂN THỊ TRÊN ĐẢO (DANH SÁCH GỘP TOÀN BỘ THẺ MẪU & THẺ CHỜ)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(Lang.s(context, "sample_tester_title"))
                        SampleTester(context)
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
                                "LTMori · Origin Isle v3.0.1",
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = { privacyPolicyOpen = true }) {
                                    Text(Lang.s(context, "privacy_rule"), fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ltmprofile.pages.dev/")))
                                }) {
                                    Text(Lang.s(context, "contact_author"), fontSize = 12.sp)
                                }
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
                            ToggleRow(
                                IsleIcons.Pulse, IsleColors.TileBlue,
                                Lang.s(context, "expand_on_notif"), expandOnNotification,
                                subtitle = Lang.s(context, "expand_on_notif_subtitle"),
                            ) {
                                expandOnNotification = it
                                prefs.edit().putBoolean("cast_expand_on_notification", it).apply()
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
                        SectionLabel(Lang.s(context, "idle_card_text"))
                        GroupCard {
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
                        SectionLabel(Lang.s(context, "idle_gemini"))
                        GroupCard {
                            SegmentedControl(
                                listOf("Gemini Voice", "Circle to Search"),
                                selected = if (voiceAction == "GOOGLE_CTS") 1 else 0,
                                onSelect = {
                                    voiceAction = if (it == 1) "GOOGLE_CTS" else "GEMINI_VOICE"
                                    bridgePrefs.edit().putString("voice_action", voiceAction).apply()
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                            ToggleRow(
                                IsleIcons.Check, IsleColors.TileBlue,
                                Lang.s(context, "gemini_tap"), geminiTap,
                                subtitle = Lang.s(context, "gemini_tap_subtitle"),
                            ) {
                                geminiTap = it
                                prefs.edit().putBoolean("ltm_idle_gemini_tap", it).apply()
                            }
                            GroupDivider()
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { LtmIslandHelper.launchGemini(context) },
                                    modifier = Modifier.weight(1f),
                                ) { Text(Lang.s(context, "open_gemini")) }
                                OutlinedButton(
                                    onClick = { LtmIslandHelper.launchCircleToSearch(context) },
                                    modifier = Modifier.weight(1f),
                                ) { Text(Lang.s(context, "circle_to_search")) }
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
                        color = IsleColors.Text
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
private fun SampleTester(context: Context) {
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
            onClick = { sample.post(context); sentAt = System.currentTimeMillis() },
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
