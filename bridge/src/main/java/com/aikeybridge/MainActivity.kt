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
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLogs by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf<String?>(null) }

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
            // Header with Back button
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
                        fontSize = 20.sp,
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

            // 3. Ghép nối Gỡ lỗi không dây (Wireless Pairing)
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
                            "Gỡ lỗi không dây",
                            color = BridgeColors.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Text(
                        "Nhập mã ghép nối 6 chữ số qua thông báo hệ thống khi kết nối.",
                        color = BridgeColors.TextSecondary,
                        fontSize = 13.sp,
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
