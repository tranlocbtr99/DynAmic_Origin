package com.aikeybridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aikeybridge.data.ActionMode
import com.aikeybridge.ui.AppViewModel
import com.aikeybridge.ui.Intents
import rikka.shizuku.Shizuku

// Consistent Dark Theme Palette matching LTMori
private object BridgeColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceHigher = Color(0xFF2C2C2E)
    val Border = Color(0x33FFFFFF)
    val Accent = Color(0xFF0A84FF)
    val Text = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary = Color(0xFF636366)
    val Good = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)
    val Danger = Color(0xFFFF453A)
    val Divider = Color(0xFF2C2C2E)
}

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    private val listener = Shizuku.OnRequestPermissionResultListener { request, result ->
        if (request == AppViewModel.SHIZUKU_REQUEST_CODE) {
            model.onShizukuPermissionResult(result == android.content.pm.PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
        setContent {
            val darkScheme = darkColorScheme(
                primary = BridgeColors.Accent,
                background = BridgeColors.Background,
                surface = BridgeColors.Surface,
                onPrimary = Color.White,
                onBackground = BridgeColors.Text,
                onSurface = BridgeColors.Text,
            )
            MaterialTheme(colorScheme = darkScheme) {
                SetupScreen(model, ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refreshPermissions()
        model.checkScript()
    }

    override fun onDestroy() {
        runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
        super.onDestroy()
    }
}

@Composable
private fun SetupScreen(model: AppViewModel, onBack: () -> Unit) {
    val status by model.script.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val wirelessAdb by model.wirelessAdb.collectAsStateWithLifecycle()
    val permissions by model.permissions.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLogs by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf<String?>(null) }
    var showGuideDialog by remember { mutableStateOf(false) }

    if (showGuideDialog) {
        MethodGuideDialog(onDismiss = { showGuideDialog = false })
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BridgeColors.Background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header with Back button & Guide button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BridgeColors.SurfaceHigher)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lc_chevron_left),
                        contentDescription = "Quay lại",
                        tint = BridgeColors.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Thiết lập Hi Jovi & Phím AI",
                        color = BridgeColors.Text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Cầu nối điều hướng trợ lý Google trên Vivo",
                        color = BridgeColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BridgeColors.SurfaceHigher)
                        .clickable { showGuideDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lc_sparkles),
                        contentDescription = "Hướng dẫn",
                        tint = BridgeColors.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // 1. Trạng thái cầu nối
            BridgeCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Trạng thái hoạt động",
                            color = BridgeColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val (statusBadgeText, badgeColor) = when {
                            status.busy -> "Đang xử lý" to BridgeColors.Warning
                            status.running == true -> "Đang chạy" to BridgeColors.Good
                            else -> "Đã dừng" to BridgeColors.TextSecondary
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Text(
                                statusBadgeText,
                                color = badgeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Text(
                        when {
                            status.busy -> "Đang cập nhật hoặc thực thi lệnh nền…"
                            status.running == true && status.detachedFromAdbd == true ->
                                "Cầu nối đang chạy nền độc lập và đã tách khỏi tiến trình ADB."
                            status.running == true ->
                                "Cầu nối đang hoạt động để bắt tín hiệu Hi Jovi / phím AI."
                            else ->
                                "Cầu nối đang tắt. Bật Gỡ lỗi không dây hoặc khởi động lại bên dưới."
                        },
                        color = BridgeColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    // Control buttons
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            enabled = !status.busy && status.running != true,
                            onClick = { model.startScript() },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BridgeColors.Accent,
                                disabledContainerColor = BridgeColors.SurfaceHigher,
                            ),
                        ) {
                            Text("Bật cầu nối", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        OutlinedButton(
                            enabled = !status.busy && status.running == true,
                            onClick = model::stopScript,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (status.running == true) BridgeColors.Danger else BridgeColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BridgeColors.Danger,
                                disabledContentColor = BridgeColors.TextTertiary,
                            ),
                        ) {
                            Text("Tắt cầu nối", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 2. Lựa chọn hành vi kích hoạt (Tick chọn 1 trong 2: Gemini Voice vs Circle to Search)
            BridgeCard {
                Column(Modifier.padding(vertical = 12.dp)) {
                    Text(
                        "KHI NÓI HI JOVI HOẶC BẤM PHÍM AI",
                        color = BridgeColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    // Option 1: Gemini Voice
                    ActionSelectionRow(
                        title = "Gemini Voice",
                        subtitle = "Mở lớp phủ Gemini và tự động kích hoạt micro",
                        selected = settings.voiceAction == ActionMode.GEMINI_VOICE || settings.voiceAction == ActionMode.DEFAULT,
                        onClick = { model.setVoiceAction(ActionMode.GEMINI_VOICE) },
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(BridgeColors.Divider)
                    )

                    // Option 2: Circle to Search
                    ActionSelectionRow(
                        title = "Circle to Search",
                        subtitle = "Khoanh tròn tìm kiếm hoặc dịch nội dung màn hình",
                        selected = settings.voiceAction == ActionMode.GOOGLE_CTS,
                        onClick = { model.setVoiceAction(ActionMode.GOOGLE_CTS) },
                    )
                }
            }

            // 3. Phương thức 1: Trợ năng thông minh (Không cần ADB / Wi-Fi)
            BridgeCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BridgeColors.Good.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lc_accessibility),
                                    contentDescription = null,
                                    tint = BridgeColors.Good,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                "1. Trợ năng (Không cần ADB)",
                                color = BridgeColors.Text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        val isA11yOn = permissions.accessibility
                        val a11yBadgeText = if (isA11yOn) "Đã bật" else "Chưa bật"
                        val a11yBadgeColor = if (isA11yOn) BridgeColors.Good else BridgeColors.Warning
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(a11yBadgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(a11yBadgeColor)
                            )
                            Text(
                                a11yBadgeText,
                                color = a11yBadgeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Text(
                        "Giải pháp độc lập 100% trên máy không cần máy tính hay gỡ lỗi. Tự bắt cửa sổ BlueLM Copilot qua Window Manager và chuyển hướng sang Gemini.",
                        color = BridgeColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    Button(
                        onClick = { Intents.openAccessibility(context) },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (permissions.accessibility) BridgeColors.SurfaceHigher else BridgeColors.Accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lc_accessibility),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (permissions.accessibility) "Quản lý Cài đặt Trợ năng" else "Bật quyền Trợ năng ngay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // 4. Phương thức 2: Gỡ lỗi không dây nội bộ (Tự động tắt sau khi kết nối)
            BridgeCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BridgeColors.Accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lc_radio),
                                contentDescription = null,
                                tint = BridgeColors.Accent,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Text(
                            "2. Gỡ lỗi không dây (Tự tắt)",
                            color = BridgeColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Text(
                        "Nhập mã ghép nối 6 chữ số qua thông báo hệ thống. Sau khi kết nối, hệ thống sẽ tự động tắt Gỡ lỗi để ẩn icon trạng thái & an toàn với app ngân hàng.",
                        color = BridgeColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = model::startWirelessPairing,
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BridgeColors.Accent),
                        ) {
                            Text("Ghép nối", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = model::connectBuiltInEngine,
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BridgeColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BridgeColors.Text),
                        ) {
                            Text("Kết nối lại", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        TextButton(
                            onClick = model::stopWirelessPairing,
                            modifier = Modifier.weight(0.9f).height(40.dp),
                        ) {
                            Text("Hủy", color = BridgeColors.TextSecondary, fontSize = 13.sp)
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BridgeColors.Divider)
                    )

                    // Tự động tắt / ẩn Gỡ lỗi không dây sau khi kết nối
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { model.setAutoHideWirelessAdb(!wirelessAdb.autoHideEnabled) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Tự động tắt/ẩn sau khi kết nối",
                                color = BridgeColors.Text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Tắt adb_wifi_enabled=0 để xóa thông báo thanh trạng thái & an toàn trước app ngân hàng",
                                color = BridgeColors.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                        Switch(
                            checked = wirelessAdb.autoHideEnabled,
                            onCheckedChange = { model.setAutoHideWirelessAdb(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BridgeColors.Accent,
                                uncheckedThumbColor = BridgeColors.TextSecondary,
                                uncheckedTrackColor = BridgeColors.SurfaceHigher,
                            ),
                        )
                    }

                    OutlinedButton(
                        onClick = model::hideWirelessAdb,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BridgeColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BridgeColors.Text),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lc_shield_check),
                            contentDescription = null,
                            tint = BridgeColors.Good,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Tắt / Ẩn thông báo Gỡ lỗi không dây ngay", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 5. Phương thức 3: Cài đặt ADB trực tiếp qua PC (Cáp USB 1 lần)
            BridgeCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BridgeColors.Warning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lc_terminal),
                                contentDescription = null,
                                tint = BridgeColors.Warning,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Text(
                            "3. Cài ADB trực tiếp qua PC (Cáp USB)",
                            color = BridgeColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Text(
                        "Dành cho ai muốn chạy lệnh shell UID 2000 nhưng không muốn mở Wi-Fi Debugging. Cắm cáp USB vào máy tính, mở CMD/Terminal và chạy lệnh bên dưới, sau đó rút cáp tắt Gỡ lỗi USB.",
                        color = BridgeColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    // Monospace Code Box
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BridgeColors.SurfaceHigher)
                            .padding(10.dp)
                    ) {
                        Text(
                            model.directAdbPermissionCommand,
                            color = BridgeColors.Text,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_Direct", model.directAdbPermissionCommand))
                                android.widget.Toast.makeText(context, "Đã sao chép lệnh ADB cấp quyền", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BridgeColors.Accent),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lc_copy),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Sao chép lệnh", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB_Engine", model.shizukuStartCommand))
                                android.widget.Toast.makeText(context, "Đã sao chép lệnh Engine đầy đủ", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.3f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BridgeColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BridgeColors.Text),
                        ) {
                            Text("Sao chép Engine", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 4. Chẩn đoán & Nhật ký (gọn gàng, nhấn để xem)
            BridgeCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Kiểm tra nhật ký",
                            color = BridgeColors.Text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(
                            onClick = {
                                model.checkScript()
                                val directory = context.getExternalFilesDir(null)
                                fun read(name: String): String = directory?.let { folder ->
                                    runCatching {
                                        java.io.File(folder, name).bufferedReader().use { reader ->
                                            generateSequence { reader.readLine() }.take(8).joinToString("\n").take(800)
                                        }.ifBlank { "Chưa có dữ liệu" }
                                    }.getOrDefault("Chưa có dữ liệu")
                                } ?: "Chưa có dữ liệu"
                                logText = "• Nhịp nền gần nhất:\n${read("script-alive")}\n\n" +
                                        "• Lệnh nhận diện gần nhất:\n${read("script-trace.log")}"
                                showLogs = !showLogs
                            },
                        ) {
                            Text(if (showLogs) "Ẩn log" else "Xem log", color = BridgeColors.Accent, fontSize = 13.sp)
                        }
                    }

                    if (showLogs && logText != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BridgeColors.SurfaceHigher)
                                .padding(10.dp)
                        ) {
                            Text(
                                logText!!,
                                color = BridgeColors.TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp,
                            )
                        }
                    }

                    message?.takeUnless { status.running != true && it.startsWith("Script nền đang chạy.") }?.let { msg ->
                        Text(
                            msg,
                            color = BridgeColors.Warning,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSelectionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) BridgeColors.Accent.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (selected) BridgeColors.Text else BridgeColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = BridgeColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Radio / Check Indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) BridgeColors.Accent else Color.Transparent)
                .then(
                    if (!selected) Modifier.background(Color.Transparent)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.lc_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BridgeColors.SurfaceHigher)
                )
            }
        }
    }
}

@Composable
private fun BridgeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BridgeColors.Surface,
        border = BorderStroke(1.dp, BridgeColors.Border),
        content = content,
    )
}

@Composable
private fun MethodGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.lc_sparkles),
                    contentDescription = null,
                    tint = BridgeColors.Accent,
                    modifier = Modifier.size(22.dp),
                )
                Text("Cơ chế & Phương thức kết nối", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Tại sao không thể tự chạy ADB local mà không có Wi-Fi hoặc cáp?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BridgeColors.Text,
                )
                Text(
                    "Theo kiến trúc bảo mật Sandbox của Android, mọi ứng dụng người dùng cài đặt đều chạy ở UID ứng dụng thường (không có quyền chạy lệnh shell hệ thống).\n" +
                    "Để chạy lệnh shell UID 2000, bắt buộc phải giao tiếp với tiến trình adbd của hệ điều hành. Tiến trình adbd chỉ mở cổng mạng TCP nội bộ khi bật 'Gỡ lỗi không dây' hoặc giao tiếp qua 'Cáp USB máy tính'. Do đó không một ứng dụng nào có thể tự lấy quyền ADB mà không thông qua 1 trong 2 kênh này.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = BridgeColors.TextSecondary,
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BridgeColors.Divider)
                )

                Text(
                    "So sánh 3 phương thức kích hoạt:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BridgeColors.Text,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Phương thức 1 — Trợ năng thông minh (Khuyên dùng):\n" +
                        "Hoàn toàn 100% không cần ADB hay máy tính. Chỉ cần bật Trợ năng cho LTMori trong Cài đặt một lần duy nhất, vĩnh viễn không mất sau khi khởi động lại máy và an toàn với mọi app ngân hàng/VNeID.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = BridgeColors.TextSecondary,
                    )
                    Text(
                        "• Phương thức 2 — Gỡ lỗi không dây tự tắt:\n" +
                        "Dành cho ai muốn dùng Engine UID 2000 ngay trên điện thoại. Sau khi kết nối, hệ thống tự động tắt cờ Gỡ lỗi để ẩn icon thanh trạng thái và tránh bị app ngân hàng phát hiện.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = BridgeColors.TextSecondary,
                    )
                    Text(
                        "• Phương thức 3 — Cài ADB trực tiếp qua PC (Cáp USB):\n" +
                        "Cắm cáp vào máy tính chạy 1 lệnh duy nhất rồi rút cáp tắt Gỡ lỗi USB. Không cần mở Wi-Fi Debugging.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = BridgeColors.TextSecondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đã hiểu", color = BridgeColors.Accent, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BridgeColors.Surface,
        titleContentColor = BridgeColors.Text,
        textContentColor = BridgeColors.TextSecondary,
    )
}
