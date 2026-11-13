package com.originisle.android.service

import android.content.Context
import android.util.Log
import com.aikeybridge.shell.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Powerful optimizer for Vivo / iQOO devices running OriginOS (including China Domestic ROMs).
 * 1. Permanent Keep-Alive: Whitelists LTMori from Doze and grants AppOps so it never dies on recents swipe.
 * 2. Notification Delay Fix: Unfreezes GMS, GSF, and messaging apps (Zalo, Messenger, Telegram...) from Vivo's aggressive deep sleep.
 */
object LtmVivoOptimizer {
    private const val TAG = "LtmVivoOptimizer"
    private const val TARGET_PKG = "com.autonavi.minimap"

    data class OptimizationResult(
        val success: Boolean,
        val message: String,
        val executedCommands: Int,
    )

    private val KEEP_ALIVE_COMMANDS = listOf(
        "dumpsys deviceidle whitelist +$TARGET_PKG",
        "cmd appops set $TARGET_PKG RUN_IN_BACKGROUND allow",
        "cmd appops set $TARGET_PKG RUN_ANY_IN_BACKGROUND allow",
        "cmd appops set $TARGET_PKG START_FOREGROUND allow",
        "cmd appops set $TARGET_PKG AUTO_START allow",
        "settings put secure vivo_associated_start_policy 1",
        "settings put global vivo_perf_opt_enabled 0",
    )

    private val NOTIF_DELAY_APPS = listOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.zing.zalo",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.google.android.gm",
        "com.whatsapp",
        "com.facebook.katana",
    )

    fun isShizukuAvailable(context: Context): Boolean =
        ShizukuShell.isEngineOrShizukuReady(context)

    suspend fun executeKeepAlive(context: Context): OptimizationResult = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable(context)) {
            return@withContext OptimizationResult(
                false,
                "Chưa kết nối Shizuku hoặc Gỡ lỗi không dây. Vui lòng kết nối Shizuku hoặc sao chép lệnh ADB để chạy thủ công.",
                0,
            )
        }

        val shell = ShizukuShell(context)
        var count = 0
        val errors = mutableListOf<String>()

        for (cmd in KEEP_ALIVE_COMMANDS) {
            val res = shell.exec(cmd)
            if (res.exitCode == 0 || res.exitCode == -1 && res.stdout.isNotBlank()) {
                count++
            } else if (res.stderr.isNotBlank()) {
                errors.add(res.stderr.trim())
            }
        }

        if (count > 0) {
            OptimizationResult(
                true,
                "Đã cấp quyền chạy ngầm vĩnh viễn thành công cho LTMori ($count lệnh). Bây giờ bạn có thể vuốt xóa app khỏi danh sách gần đây mà đảo vẫn hoạt động bình thường!",
                count,
            )
        } else {
            OptimizationResult(
                false,
                "Thực thi không thành công: ${errors.firstOrNull() ?: "Lỗi quyền thực thi"}",
                0,
            )
        }
    }

    suspend fun executeFixNotificationDelay(context: Context): OptimizationResult = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable(context)) {
            return@withContext OptimizationResult(
                false,
                "Chưa kết nối Shizuku hoặc Gỡ lỗi không dây. Hãy kết nối hoặc sao chép lệnh ADB để chạy qua máy tính.",
                0,
            )
        }

        val shell = ShizukuShell(context)
        var count = 0

        // 1. Unfreeze network restriction
        shell.exec("cmd netpolicy set restrict-background false")
        shell.exec("settings put global fgs_notification_policy 0")
        count += 2

        // 2. Whitelist and grant AppOps for Google Services and messaging apps
        for (pkg in NOTIF_DELAY_APPS) {
            val r1 = shell.exec("dumpsys deviceidle whitelist +$pkg")
            val r2 = shell.exec("cmd appops set $pkg RUN_IN_BACKGROUND allow")
            val r3 = shell.exec("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
            if (r1.exitCode == 0 || r2.exitCode == 0) {
                count++
            }
        }

        OptimizationResult(
            true,
            "Đã tối ưu chống trễ thông báo thành công cho Google Play Services, Zalo, Messenger, Telegram... Hệ thống sẽ không đưa ứng dụng vào trạng thái ngủ đông sâu khi tắt màn hình!",
            count,
        )
    }

    fun getKeepAliveAdbScript(): String =
        KEEP_ALIVE_COMMANDS.joinToString("\n")

    fun getFixNotificationDelayAdbScript(): String {
        val cmds = mutableListOf<String>()
        cmds.add("# 1. Tắt giới hạn mạng ngầm khi tắt màn hình")
        cmds.add("cmd netpolicy set restrict-background false")
        cmds.add("settings put global fgs_notification_policy 0")
        cmds.add("\n# 2. Bỏ ngủ đông cho Google Play Services & GSF")
        cmds.add("dumpsys deviceidle whitelist +com.google.android.gms")
        cmds.add("dumpsys deviceidle whitelist +com.google.android.gsf")
        cmds.add("cmd appops set com.google.android.gms RUN_IN_BACKGROUND allow")
        cmds.add("cmd appops set com.google.android.gms RUN_ANY_IN_BACKGROUND allow")
        cmds.add("\n# 3. Bỏ ngủ đông cho Zalo, Messenger, Telegram, Gmail...")
        for (pkg in NOTIF_DELAY_APPS.filter { !it.startsWith("com.google.android.g") }) {
            cmds.add("dumpsys deviceidle whitelist +$pkg")
            cmds.add("cmd appops set $pkg RUN_IN_BACKGROUND allow")
            cmds.add("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
        }
        return cmds.joinToString("\n")
    }
}
