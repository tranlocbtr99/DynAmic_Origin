package com.originisle.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
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
 * The main screen: listener status, the sample-card tester, casting settings, and links to the Apps
 * and Activity screens. Setup/permissions and the update check live in the ⋯ menu.
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
    var donateExpanded by remember { mutableStateOf(false) }
    var sendToIslandExpanded by remember { mutableStateOf(false) }
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
    var idlePresetOpen by remember { mutableStateOf(false) }
    var idlePreset by remember { mutableStateOf(prefs.getString("ltm_idle_preset", "earth_only").orEmpty()) }
    var idleTitle by remember { mutableStateOf(prefs.getString("ltm_idle_card_title", "LTMori").orEmpty()) }
    var idleText by remember { mutableStateOf(prefs.getString("ltm_idle_card_text", "Đang chờ").orEmpty()) }
    var chargingLiveUpdates by remember { mutableStateOf(prefs.getBoolean("cast_charging_live_updates", false)) }
    var messageIslandOnly by remember { mutableStateOf(prefs.getBoolean("ltmori_message_island_only", true)) }
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
            // Denied, or the dialog was skipped (Android stops showing it after a refusal): the
            // app's own page is where it can still be turned on. Back here, the resume check
            // below picks it up — the switch stays wanted in prefs.
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
    // "Nearby devices" may have been turned on (or off) in Settings while we were away.
    LaunchedEffect(tick.intValue) {
        val now = prefs.getBoolean("cast_earbuds", false) && EarbudsCard.hasPermission(context)
        if (now != earbudsOn) {
            earbudsOn = now
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        }
    }
    // Setup only needs attention again if something actually broke (OriginOS revoking notification
    // access or re-restricting battery is a known failure mode) — then Home shows a one-line banner;
    // otherwise the permissions stay behind the ⋯ menu so they're not in the way on every open.
    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Poll the listener once a second so the status pill stays live.
    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    LaunchedEffect(Unit) {
        while (true) { connected = NotificationCastListener.instance != null; delay(1000) }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }
    if (idlePresetOpen) {
        val presets = listOf("dot" to "Chấm", "earth_only" to "Trái Đất", "date_earth" to "Ngày + Trái Đất",
            "weather" to "Thời tiết", "weather_wave" to "Thời tiết + sóng", "wave" to "Sóng nhạc",
            "ram" to "RAM", "network" to "Mạng", "gemini" to "Gemini", "custom" to "Tùy chỉnh")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { idlePresetOpen = false },
            title = { Text("Kiểu thẻ chờ") },
            text = {
                Column {
                    presets.forEach { (key, label) ->
                        TextButton(onClick = {
                            idlePreset = key
                            prefs.edit().putString("ltm_idle_preset", key).apply()
                            LtmIslandHelper.updateIdlePreset(context)
                            LtmIslandHelper.syncIdleTicker(context)
                            idlePresetOpen = false
                        }) { Text(if (key == idlePreset) "✓ $label" else label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { idlePresetOpen = false }) { Text("Đóng") } },
        )
    }
    if (idleCardEditOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { idleCardEditOpen = false },
            title = { Text("Tùy chỉnh thẻ chờ") },
            text = {
                Column {
                    OutlinedTextField(idleTitle, { idleTitle = it.take(32) }, label = { Text("Tiêu đề") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(idleText, { idleText = it.take(64) }, label = { Text("Nội dung") }, maxLines = 3)
                    TextButton(onClick = { idleTitle = "LTMori"; idleText = "Đang chờ" }) { Text("Khôi phục mặc định") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString("ltm_idle_card_title", idleTitle.trim().ifEmpty { "LTMori" })
                        .putString("ltm_idle_card_text", idleText.trim()).apply()
                    LtmIslandHelper.syncIdleTicker(context)
                    LtmIslandHelper.updateIdlePreset(context)
                    idleCardEditOpen = false
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { idleCardEditOpen = false }) { Text("Hủy") } },
        )
    }

    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (selectedTab == 0) item {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Origin Isle",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.7).sp,
                        lineHeight = 38.sp,
                        color = IsleColors.Text,
                    )
                    StatusPill(connected, onClick = onOpenActivity)
                }
                Box {
                    CircleIconButton(IsleIcons.More, "More", onClick = { menuOpen = true })
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        offset = DpOffset(0.dp, 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = IsleColors.Menu,
                    ) {
                        MenuItem("Check for updates") {
                            menuOpen = false
                            coroutineScope.launch {
                                updateStatus = "Checking…"
                                updateStatus = when (val result = UpdateChecker.check(context)) {
                                    is UpdateChecker.Result.UpToDate ->
                                        "You're on the latest version (v${result.current})."
                                    is UpdateChecker.Result.UpdateAvailable -> {
                                        UpdateChecker.openRelease(context, result.url)
                                        "Update available: v${result.latest} — opening GitHub…"
                                    }
                                    is UpdateChecker.Result.Error -> result.message
                                }
                            }
                        }
                        MenuItem("Permissions") {
                            menuOpen = false
                            permissionsOpen = true
                        }
                    }
                }
            }
            updateStatus?.let {
                Text(
                    it,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    fontSize = 13.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        }

        if (selectedTab == 0) item {
            GroupCard {
                Column {
                    NavRow(IsleIcons.Heart, IsleColors.TileAmber, "Ủng hộ tác giả", if (donateExpanded) "Thu gọn" else "Mở rộng") {
                        donateExpanded = !donateExpanded
                    }
                    if (donateExpanded) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Techcombank · Tran Minh Loc", fontSize = 13.sp, color = IsleColors.TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("4444444790", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("STK", "4444444790"))
                                    android.widget.Toast.makeText(context, "Đã copy số tài khoản", android.widget.Toast.LENGTH_SHORT).show()
                                }) { Text("Copy") }
                            }
                            TextButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ltmprofile.pages.dev/")))
                            }) { Text("Liên hệ tác giả") }
                        }
                    }
                }
            }
        }

        if (!listenerOk || !batteryOk) {
            item { SetupAttentionBanner(onClick = { permissionsOpen = true }) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SampleTester(context)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        "Recast all", onClick = { recastAll(context) },
                        modifier = Modifier.weight(1f), icon = IsleIcons.Refresh,
                        enabled = castOn || mediaOn,
                    )
                    PillButton(
                        "Stop all", onClick = { stopAll(context) },
                        modifier = Modifier.weight(1f), icon = IsleIcons.Stop, iconSize = 16.dp,
                        content = IsleColors.Danger,
                    )
                }
            }
        }

        if (selectedTab == 0) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Send to island")
                GroupCard {
                    NavRow(IsleIcons.Send, IsleColors.TileBlue, "Send to island", if (sendToIslandExpanded) "Thu gọn" else "Mở rộng") {
                        sendToIslandExpanded = !sendToIslandExpanded
                    }
                    if (sendToIslandExpanded) {
                        ToggleRow(IsleIcons.Bell, IsleColors.TileBlue, "Notifications", castOn,
                            subtitle = "Cast notifications to the island",
                        ) {
                            castOn = it; prefs.edit().putBoolean("cast_notifications", it).apply()
                            if (it) PlaygroundService.keepAlive(context)
                            PrivacyCard.refresh()
                            FlashlightCard.refresh()
                            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                        }
                        GroupDivider()
                        ToggleRow(IsleIcons.Music, IsleColors.TileOrange, "Media", mediaOn,
                            subtitle = "Mimic OriginPlayer for media apps",
                        ) {
                            mediaOn = it; prefs.edit().putBoolean("cast_media_sessions", it).apply()
                        }
                    }
                }
            }
        }

        if (selectedTab == 1) item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Notifications")
                GroupCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedControl(
                            listOf("Live only", "Everything"),
                            selected = if (includeMessages) 1 else 0,
                            onSelect = {
                                includeMessages = it == 1
                                prefs.edit().putBoolean("cast_include_messages", includeMessages).apply()
                            },
                        )
                        Text(
                            if (includeMessages) "Every notification, plain messages included."
                            else "Only live cards — score, navigation, payment, etc.",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                    if (includeMessages) {
                        GroupDivider()
                        SettingRow(IsleIcons.Clock, IsleColors.TileTeal, "Hide after", subtitle = "Non live cards")
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(0 to "Never", 5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s").forEach { (sec, label) ->
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
                        IsleIcons.Headphones, IsleColors.TilePurple, "Earbuds", earbudsOn,
                        subtitle = "Battery when they connect",
                    ) {
                        prefs.edit().putBoolean("cast_earbuds", it).apply()
                        if (it && !EarbudsCard.hasPermission(context)) {
                            // "Nearby devices": asked for here, only by this switch.
                            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            earbudsOn = it
                            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.TilePurple, "Skip silent", ignoreSilent,
                        subtitle = "Ignore silenced notifications",
                    ) {
                        ignoreSilent = it; prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Pulse, IsleColors.TileBlue, "Expand island on notification", expandOnNotification,
                        subtitle = "Pop the island when a notification arrives (silent ones excluded)",
                    ) {
                        expandOnNotification = it; prefs.edit().putBoolean("cast_expand_on_notification", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Pulse, IsleColors.TilePurple, "Dedup same app", dedupSameApp,
                        subtitle = "Don't re-expand for repeated notifications from one app",
                    ) {
                        dedupSameApp = it; prefs.edit().putBoolean("cast_dedup_same_app", it).apply()
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
                        IsleIcons.Send, IsleColors.TilePurple, "Hide from notification shade", hideFromShade,
                        subtitle = "Show island card only, no duplicate in shade",
                    ) {
                        hideFromShade = it; prefs.edit().putBoolean("cast_hide_from_shade", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Send, IsleColors.TileBlue, "Messages island-only", messageIslandOnly,
                        subtitle = "Hide message cards from the shade",
                    ) {
                        messageIslandOnly = it; prefs.edit().putBoolean("ltmori_message_island_only", it).apply()
                    }
                    GroupDivider()
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ngôn ngữ", modifier = Modifier.weight(1f), color = IsleColors.Text)
                        listOf("vi" to "VI", "en" to "EN").forEach { (code, label) ->
                            FilterChip(selected = appLang == code, onClick = {
                                appLang = code; Lang.setLang(context, code)
                            }, label = { Text(label) }, modifier = Modifier.weight(0.3f))
                        }
                    }
                    GroupDivider()
                    NavRow(IsleIcons.Clock, IsleColors.TileBlue, "Idle preset", idlePreset) {
                        idlePreset = prefs.getString("ltm_idle_preset", "earth_only").orEmpty()
                        idlePresetOpen = true
                    }
                    GroupDivider()
                    NavRow(IsleIcons.Send, IsleColors.TilePurple, "Idle card text", "Edit") {
                        idleTitle = prefs.getString("ltm_idle_card_title", "LTMori").orEmpty()
                        idleText = prefs.getString("ltm_idle_card_text", "Đang chờ").orEmpty()
                        idleCardEditOpen = true
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Lock, IsleColors.TileGreen, "Lock screen", lockscreenLiveCard,
                        subtitle = "Live card on lockscreen",
                    ) {
                        lockscreenLiveCard = it
                        prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                    }
                }
            }
        }

        if (selectedTab == 2) item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Cards")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Key, IsleColors.TileBlue, "Login codes", otpOn,
                        subtitle = "Show the code, tap to copy",
                    ) {
                        otpOn = it; prefs.edit().putBoolean("cast_otp", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Buttons, IsleColors.TileTeal, "Action buttons", actionsOn,
                        subtitle = "The app's own buttons on the card",
                    ) {
                        actionsOn = it; prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Music, IsleColors.TileBlue, "Live lyrics", liveLyricsOn,
                        subtitle = "Sync lyrics from LRCLIB onto the media card",
                    ) {
                        liveLyricsOn = it; prefs.edit().putBoolean("cast_live_lyrics_opt_in", it).apply()
                        if (!it) LtmIslandHelper.stopLiveLyrics()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Bolt, IsleColors.TileGreen, "Charging", chargingOn,
                        subtitle = "Battery card when you plug in",
                    ) {
                        chargingOn = it; prefs.edit().putBoolean("cast_charging", it).apply()
                        ChargingCard.onSettingsChanged(context)
                    }
                    if (chargingOn) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(false to "Briefly", true to "While charging").forEach { (keep, label) ->
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
                                IsleIcons.Bolt, IsleColors.TileGreen, "Live Updates when charging", chargingLiveUpdates,
                                subtitle = "Try Android's priority charging notification",
                            ) {
                                chargingLiveUpdates = it
                                prefs.edit().putBoolean("cast_charging_live_updates", it).apply()
                                if (!it) LtmIslandHelper.dismissChargingLive(context)
                            }
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Camera, IsleColors.TileAmber, "Camera & mic", privacyDotOn,
                        subtitle = "Dot while they're in use",
                    ) {
                        privacyDotOn = it; prefs.edit().putBoolean("cast_privacy_dot", it).apply()
                        PrivacyCard.refresh()
                    }
                }
            }
        }

        if (selectedTab == 2) item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Idle & Gemini")
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
                        IsleIcons.Check, IsleColors.TileBlue, "Tap island to open Gemini", geminiTap,
                        subtitle = "Tap the island or idle card to launch Gemini",
                    ) {
                        geminiTap = it; prefs.edit().putBoolean("ltm_idle_gemini_tap", it).apply()
                    }
                    GroupDivider()
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { LtmIslandHelper.launchGemini(context) }, modifier = Modifier.weight(1f)) { Text("Mở Gemini") }
                        OutlinedButton(onClick = { LtmIslandHelper.launchCircleToSearch(context) }, modifier = Modifier.weight(1f)) { Text("Circle to Search") }
                    }
                }
            }
        }

        if (selectedTab == 3) item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Cầu nối Gemini (Hi Jovi)")
                GroupCard {
                    NavRow(IsleIcons.Send, IsleColors.TileBlue, "Thiết lập Hi Jovi", "Mở") {
                        runCatching {
                            context.startActivity(
                                Intent().setComponent(
                                    ComponentName("com.autonavi.minimap", "com.aikeybridge.MainActivity")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 2) item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Quiet hours")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.TilePurple, "Follow Do Not Disturb", quietDnd,
                        subtitle = "Quiet while DND is on",
                    ) {
                        quietDnd = it; prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Clock, IsleColors.TileGray, "Schedule", quietSchedule,
                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} every day",
                    ) {
                        quietSchedule = it; prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                    }
                    if (quietSchedule) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ChoiceChip(
                                "From ${QuietHours.format(quietStart)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietStart) {
                                        quietStart = it; prefs.edit().putInt("cast_quiet_start", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                "To ${QuietHours.format(quietEnd)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietEnd) {
                                        quietEnd = it; prefs.edit().putInt("cast_quiet_end", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Text(
                    "While quiet, only calls, navigation, timers, login codes, media and charging show.",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        }

        if (selectedTab == 3) item {
            GroupCard {
                NavRow(
                    IsleIcons.Grid, IsleColors.TileAmber, "Apps",
                    value = if (appsOff == 0) "All on" else "$appsOff off",
                    onClick = onOpenApps,
                )
                GroupDivider()
                NavRow(IsleIcons.Pulse, IsleColors.TileGray, "Activity", onClick = onOpenActivity)
            }
        }
    }
    // Bottom navigation bar (iOS-style glass pill)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(IsleColors.Surface.copy(alpha = 0.7f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val icons = listOf(IsleIcons.Bolt, IsleIcons.Bell, IsleIcons.Grid, IsleIcons.More)
            val labels = listOf("Đảo", "Thông báo", "Tuỳ chỉnh", "Cài đặt")
            labels.forEachIndexed { i, label ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (selectedTab == i) IsleColors.Accent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { selectedTab = i }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(icons[i], contentDescription = label, tint = if (selectedTab == i) IsleColors.Accent else IsleColors.TextSecondary, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 10.sp, color = if (selectedTab == i) IsleColors.Accent else IsleColors.TextSecondary)
                }
            }
        }
    }
    }
}

/** Green "Listening" / red "Not listening" pill under the title; opens the Activity screen. */
@Composable
private fun StatusPill(connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) IsleColors.Good else IsleColors.Bad))
        Text(
            if (connected) "Listening" else "Not listening",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (connected) IsleColors.GoodText else IsleColors.BadText,
        )
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IsleColors.Text) },
        onClick = onClick,
        modifier = Modifier.widthIn(min = 200.dp).heightIn(min = 48.dp),
    )
}

/** Island preview + sample picker + send button: fires any [IslandSamples] card on demand. */
@Composable
private fun SampleTester(context: Context) {
    var selected by remember { mutableIntStateOf(0) }
    val sample = IslandSamples.all[selected]
    var sentAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sentAt) {
        if (sentAt != 0L) { delay(1600); sentAt = 0L }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(IsleColors.Surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(IsleColors.PreviewBg)
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // What the compact island will look like once this sample is sent.
            IslandPreviewView(sample.preview)
            Text(
                sample.summary,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.PreviewLabel,
                textAlign = TextAlign.Center,
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(IslandSamples.all) { i, s ->
                ChoiceChip(
                    s.name,
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContainer = IsleColors.Text,
                    selectedContent = IsleColors.Background,
                    shape = RoundedCornerShape(18.dp),
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                )
            }
        }
        PillButton(
            if (sentAt != 0L) "Sent" else "Send to island",
            onClick = { sample.post(context); sentAt = System.currentTimeMillis() },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            icon = IsleIcons.Send,
            container = IsleColors.Accent,
            content = Color.White,
            height = 48.dp,
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

/**
 * Ask the bound notification listener to replay every current notification onto the island. If the
 * listener isn't connected (access not granted, or the binding went stale after a kill), keep it
 * alive and request a rebind so the sweep works on the next tap.
 */
private fun recastAll(context: Context) {
    PlaygroundService.keepAlive(context)
    val listener = NotificationCastListener.instance
    if (listener == null) {
        val message = when (NotificationCastListener.forceRebind(context)) {
            NotificationCastListener.RebindResult.NO_ACCESS ->
                "Notification access isn't granted — turn it on in Settings, then tap again."
            else ->
                "Listener not connected — reconnecting now. Give it a few seconds, then tap again."
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        return
    }
    val count = listener.recastAll()
    android.widget.Toast.makeText(
        context, "Recast swept $count notifications.", android.widget.Toast.LENGTH_SHORT,
    ).show()
}
