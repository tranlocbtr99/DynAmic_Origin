package com.originisle.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-run gate: walks through every authorization the app needs before letting the user into the
 * main screen. Only notification access is mandatory to continue; the rest are strongly recommended
 * (battery, keep-alive, auto-start) and can be granted later from Home's ⋯ › Permissions.
 *
 * The startup row matters more than "recommended" suggests: with "Associated startup" off, nothing
 * brings casting back after a swipe-away — not START_STICKY, not the restart alarm, not the
 * accessibility service — and only a reboot recovers.
 */
@Composable
fun OnboardingScreen(context: Context, prefs: SharedPreferences, onDone: () -> Unit) {
    val tick = rememberResumeTick()

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { tick.intValue++ }

    val notifGranted = remember(tick.intValue) { isListenerEnabled(context) }
    val postGranted = remember(tick.intValue) {
        Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    val accGranted = remember(tick.intValue) {
        com.aikeybridge.guard.BankGuard.isAccessibilityOn(context) || com.aikeybridge.guard.BankGuard.bankDisabledByUs
    }

    var autoStartAck by remember {
        mutableStateOf(prefs.getBoolean("onboarding_autostart_ack", false))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Chào mừng đến với LTMori",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.7).sp,
                    lineHeight = 38.sp,
                    color = IsleColors.Text,
                )
                Text(
                    "Cần cấp một số quyền cần thiết để LTMori có thể chuyển tiếp thông báo lên đảo.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = IsleColors.TextTertiary,
                )
            }
        }
        item {
            GroupCard {
                OnboardingRow(
                    icon = IsleIcons.Bell,
                    tile = IsleColors.TileBlue,
                    title = "Truy cập thông báo",
                    description = "Bắt buộc — cho phép LTMori đọc thông báo để chuyển tiếp lên đảo.",
                    granted = notifGranted,
                    mandatory = true,
                ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                GroupDivider()
                OnboardingRow(
                    icon = IsleIcons.Bell,
                    tile = IsleColors.TilePurple,
                    title = "Cho phép gửi thông báo",
                    description = "Cho phép LTMori hiển thị thông báo trạng thái đảo linh hoạt.",
                    granted = postGranted,
                    mandatory = false,
                ) {
                    if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                GroupDivider()
                OnboardingRow(
                    icon = IsleIcons.Grid,
                    tile = IsleColors.TileTeal,
                    title = "Dịch vụ Trợ năng (Accessibility)",
                    description = "Chặn cuộc gọi chiếm toàn màn hình và bắt phím gọi AI (Tự né ngân hàng & VNeID).",
                    granted = accGranted,
                    mandatory = false,
                ) {
                    if (com.aikeybridge.data.PermissionsReader.hasWriteSecureSettings(context)) {
                        com.aikeybridge.guard.BankGuard.setAccessibility(context, true)
                        tick.intValue++
                    } else {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }

                GroupDivider()
                OnboardingRow(
                    icon = IsleIcons.Battery,
                    tile = IsleColors.TileGreen,
                    title = "Pin & Chạy nền không hạn chế",
                    description = "Ngăn hệ thống OriginOS tắt dịch vụ đảo ngầm để tiết kiệm pin.",
                    granted = batteryOk,
                    mandatory = false,
                ) { requestIgnoreBattery(context) }
                GroupDivider()
                OnboardingRow(
                    icon = IsleIcons.Power,
                    tile = IsleColors.TileAmber,
                    title = "Tự khởi chạy & Khởi chạy liên kết",
                    description = "Bật cả 2 mục để đảo không bị biến mất khi bạn vuốt xóa ứng dụng khỏi đa nhiệm.",
                    granted = autoStartAck,
                    mandatory = false,
                ) {
                    // Only ack if a screen actually opened — a ✓ the user never saw is worse than
                    // no ✓ for the one setting that decides whether casting survives a swipe-away.
                    if (openAutoStartSettings(context)) {
                        autoStartAck = true
                        prefs.edit().putBoolean("onboarding_autostart_ack", true).apply()
                    }
                }
            }
        }
        item {
            PillButton(
                if (notifGranted) "Tiếp tục vào ứng dụng" else "Cần cấp quyền truy cập thông báo để tiếp tục",
                onClick = {
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    onDone()
                },
                enabled = notifGranted,
                modifier = Modifier.fillMaxWidth(),
                container = IsleColors.Accent,
                content = Color.White,
            )
        }
    }
}

@Composable
private fun OnboardingRow(
    icon: ImageVector,
    tile: Color,
    title: String,
    description: String,
    granted: Boolean,
    mandatory: Boolean,
    onAction: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(icon, tile)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IsleColors.Text,
                )
                if (mandatory) {
                    Text(
                        "Required",
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(IsleColors.CastTagBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.CastTagFg,
                    )
                }
            }
            Text(description, fontSize = 13.sp, lineHeight = 18.sp, color = IsleColors.TextSecondary)
            if (!granted) {
                PillButton(
                    "Open settings",
                    onClick = onAction,
                    modifier = Modifier.padding(top = 8.dp),
                    container = IsleColors.SurfaceHigh,
                    content = IsleColors.AccentText,
                    height = 44.dp,
                )
            }
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (granted) IsleColors.GoodIconBg else IsleColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (granted) {
                Icon(IsleIcons.Check, "Granted", Modifier.size(14.dp), tint = IsleColors.GoodText)
            }
        }
    }
}
