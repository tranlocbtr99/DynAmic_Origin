package com.aikeybridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aikeybridge.data.ActionMode
import com.aikeybridge.ui.AppViewModel
import rikka.shizuku.Shizuku

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
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF005FCF), onSurface = Color(0xFF142033), onBackground = Color(0xFF142033))) {
                Setup(model, ::finish)
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
private fun Setup(model: AppViewModel, back: () -> Unit) {
    val status by model.script.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val diagnostics = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    val settings by model.settings.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF5F6F8), Color(0xFFEAF2FC))))
            .systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = back) { Text("Quay lại") }
        Text("Thiết lập Hi Jovi", color = Color(0xFF142033), style = MaterialTheme.typography.headlineMedium)
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5E8ED))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ghép nối Gỡ lỗi không dây")
                Text("Nhập mã ghép nối 6 số qua thông báo LTMori.")
                Button(onClick = model::startWirelessPairing) { Text("Ghép nối") }
                OutlinedButton(onClick = model::connectBuiltInEngine) { Text("Kết nối lại") }
                TextButton(onClick = model::stopWirelessPairing) { Text("Hủy ghép nối") }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5E8ED))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Khi nói Hi Jovi")
                FilterChip(selected = settings.voiceAction == ActionMode.GEMINI_VOICE,
                    onClick = { model.setVoiceAction(ActionMode.GEMINI_VOICE) }, label = { Text("Gemini Voice") })
                FilterChip(selected = settings.voiceAction == ActionMode.GOOGLE_CTS,
                    onClick = { model.setVoiceAction(ActionMode.GOOGLE_CTS) }, label = { Text("Circle to Search") })
                Text(when {
                    status.busy -> "Đang xử lý"
                    status.running == true && status.detachedFromAdbd == false -> "Cầu nối đang chạy và còn phụ thuộc vào ADB"
                    status.running == true && status.detachedFromAdbd == true -> "Cầu nối đang chạy, đã tách khỏi ADB"
                    status.running == true -> "Cầu nối đang chạy, chưa xác định phụ thuộc ADB"
                    status.running == false -> "Cầu nối đã dừng. Bật lại Gỡ lỗi không dây để khởi động lại."
                    else -> "Chưa xác định trạng thái cầu nối"
                })
                Button(enabled = !status.busy && status.running != true, onClick = { model.startScript() }) { Text("Bật cầu nối") }
                OutlinedButton(enabled = !status.busy, onClick = model::stopScript) { Text("Tắt cầu nối") }
                TextButton(enabled = !status.busy, onClick = {
                    model.checkScript()
                    val directory = context.getExternalFilesDir(null)
                    fun read(name: String): String = directory?.let { folder ->
                        runCatching {
                            java.io.File(folder, name).bufferedReader().use { reader ->
                                generateSequence { reader.readLine() }.take(8).joinToString("\n").take(800)
                            }.ifBlank { "Chưa có dữ liệu" }
                        }.getOrDefault("Chưa có dữ liệu")
                    } ?: "Chưa có dữ liệu"
                    diagnostics.value = "Nhịp nền gần nhất\n${read("script-alive")}\n\n" +
                        "Lần nhận Hi Jovi hoặc phím AI và lệnh đã gửi\n${read("script-trace.log")}"
                }) { Text("Kiểm tra trạng thái") }
                diagnostics.value?.let { Text(it) }
                message?.takeUnless { status.running != true && it.startsWith("Script nền đang chạy.") }?.let { Text(it) }
            }
        }
    }
}
