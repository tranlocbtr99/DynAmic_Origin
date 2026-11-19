package com.aikeybridge.guard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.aikeybridge.data.PermissionsReader
import com.aikeybridge.diagnostics.Diagnostics
import com.aikeybridge.service.AssistantAccessibilityService
import kotlinx.coroutines.launch

/**
 * BankGuard — Tự động né ngân hàng & VNeID (Kế thừa từ kiến trúc Gemini Guard 4.0).
 *
 * Cơ chế:
 * 1. Dùng UsageStatsManager theo dõi ứng dụng đang ở tiền cảnh (foreground).
 * 2. Khi phát hiện mở ứng dụng Ngân hàng hoặc VNeID:
 *    Tự động tạm gỡ bỏ AccessibilityService khỏi `enabled_accessibility_services`
 *    qua quyền WRITE_SECURE_SETTINGS (hoặc Shizuku/ADB shell) trong vòng < 500ms.
 *    Ứng dụng ngân hàng kiểm tra thấy 0 dịch vụ trợ năng -> Không bị chặn, cho phép giao dịch bình thường!
 * 3. Khi người dùng thoát khỏi ứng dụng ngân hàng:
 *    Tự động bật lại AccessibilityService vào hệ thống ngay lập tức mà người dùng không cần thao tác gì.
 */
object BankGuard {
    private const val TAG = "BankGuard"
    const val PREFS_NAME = "bank_guard_prefs"
    const val KEY_BANK_GUARD_ON = "bank_guard_on"
    const val KEY_BANK_PKGS = "bank_pkgs"
    const val KEY_POLL_MS = "bank_poll_ms"
    const val KEY_DISABLED_BY_US = "bank_disabled_by_us"
    const val DEFAULT_POLL_MS = 120

    @Volatile
    private var running = false

    @Volatile
    var bankDisabledByUs = false
        private set

    @Volatile
    var currentForegroundPkg: String? = null
        private set

    private var lastCheckTs: Long = 0L
    private var lastOemFireTs: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null

    fun setBankDisabledByUs(context: Context, value: Boolean) {
        bankDisabledByUs = value
        runCatching {
            prefs(context).edit().putBoolean(KEY_DISABLED_BY_US, value).apply()
        }
    }

    /**
     * Danh sách package mặc định của hầu hết các ứng dụng Ngân hàng, Ví điện tử và Định danh tại Việt Nam.
     */
    val DEFAULT_BANK_PKGS = setOf(
        "com.VCB",                         // Vietcombank VCB Digibank
        "com.vcb",                         // Vietcombank
        "com.vietcombank.mobile",          // Vietcombank
        "com.vnpay.bidv",                  // BIDV SmartBanking
        "vn.com.bidv.smartbanking",        // BIDV SmartBanking
        "vn.com.techcombank.mb.online",    // Techcombank Mobile chính thức
        "vn.com.techcombank.bb.app",       // Techcombank Mobile (BB)
        "vn.com.techcombank.mobile.business", // Techcombank Business
        "com.vnpay.techcombank",           // Techcombank F@st Mobile
        "com.techcombank.tcbotp",          // Techcombank OTP
        "com.mbmobile",                    // MB Bank
        "com.mb.mbbank",                   // MB Bank
        "com.vnpay.mbbank",                // MB Bank
        "com.vnpay.vpbankonline",          // VPBank NEO
        "com.vpbank.neo",                  // VPBank NEO
        "com.vnpay.agribank",              // Agribank E-Mobile Banking
        "vn.vpay.nhdt",                    // Agribank
        "com.tpb.mb.gprsandroid",          // TPBank Mobile
        "vn.com.tpb.ebank.app",            // TPBank Mobile
        "com.vnpay.vietinbank",            // VietinBank iPay
        "com.vietinbank.ipay",             // VietinBank iPay
        "mobile.acb.com.vn",               // ACB ONE
        "com.acb.android",                 // ACB
        "vn.com.acb",                      // ACB
        "com.shb.mobilebanking",           // SHB Mobile
        "com.hdbank.android",              // HDBank
        "com.msb.mb.banking",              // MSB mBank
        "com.scb.phonebanking",            // SCB Mobile Banking
        "com.vnpay.sacombank",             // Sacombank mBanking
        "com.sacombank.mbanking",          // Sacombank Pay
        "vn.com.sacombank.pay",            // Sacombank Pay
        "com.cake.android",                // CAKE by VPBank
        "vn.timo.app",                     // Timo Digital Bank
        "vn.gov.bca.vneid",                // VNeID (Bộ Công An)
        "com.vnptit.vnp.vneid",            // VNeID (VNPT)
        "com.mservice.momotransfer",       // MoMo
        "vn.com.vng.zalopay",              // ZaloPay
        "com.vng.zalopay",                 // ZaloPay
        "com.viettel.viettelpay",          // Viettel Money
        "vn.viettel.viettelpay",           // Viettel Money
        "com.mplus.ocb",                   // OCB OMNI
        "com.seabank.mbanking",            // SeABank
        "vn.com.shinhan.smart",            // Shinhan Bank
        "com.mservice.vib",                // MyVIB
        "vn.bvb.openbanking",              // BVBank
        "ops.namabank.com.vn",             // Nam A Bank
        "com.lpbank.mobilebanking",        // LPBank
        "com.pvcombank.mbanking",          // PVcomBank
        "com.kasikornbank.retail",         // KBank Vietnam
        "com.baca.mobile",                 // Bac A Bank
        "com.vab.smartbanking",            // VietABank
        "com.baoviet.smartbanking",        // BaoViet Bank
        "com.kienlongbank.mbank",          // KienlongBank
        "com.eximbank.mobilebanking",      // Eximbank
        "vn.com.coopbank.mobilebanking",   // Co-opBank
        "com.shopee.vn",                   // ShopeePay
        "com.vnpt.money"                   // VNPT Money
    )

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BANK_GUARD_ON, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BANK_GUARD_ON, enabled).apply()
        sync(context)
    }

    fun getProtectedPackages(context: Context): Set<String> {
        val extra = prefs(context).getString(KEY_BANK_PKGS, "").orEmpty()
        val custom = extra.split(',', ';', ' ', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return DEFAULT_BANK_PKGS + custom
    }

    fun isBankApp(context: Context, pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val lower = pkg.lowercase()
        if (lower.contains("techcombank") ||
            lower.contains("vietcombank") ||
            lower.contains("mbbank") ||
            lower.contains("mbmobile") ||
            lower.contains("vneid") ||
            lower.contains("bidv") ||
            lower.contains("vietinbank") ||
            lower.contains("vpbank") ||
            lower.contains("agribank") ||
            lower.contains("sacombank") ||
            lower.contains("tpbank") ||
            lower.contains("shbbank") ||
            lower.contains("hdbank") ||
            lower.contains("msbbank") ||
            lower.contains("scbbank") ||
            lower.contains("seabank") ||
            lower.contains("shinhan") ||
            lower.contains("bvbank") ||
            lower.contains("namabank") ||
            lower.contains("lpbank") ||
            lower.contains("pvcombank") ||
            lower.contains("bacabank") ||
            lower.contains("vietabank") ||
            lower.contains("kienlong") ||
            lower.contains("eximbank") ||
            lower.contains("coopbank") ||
            lower.contains("zalopay") ||
            lower.contains("viettelpay") ||
            lower.contains("viettelmoney") ||
            lower.contains("shopeepay") ||
            (lower.contains("acb") && (lower.contains("bank") || lower.contains("mobile") || lower.contains("one"))) ||
            (lower.contains("vcb") && lower.contains("com.")) ||
            (lower.contains("momo") && lower.contains("transfer"))
        ) {
            return true
        }
        val pkgs = getProtectedPackages(context)
        return pkgs.any { it.equals(pkg, ignoreCase = true) }
    }

    fun hasUsageAccess(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun openUsageSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun svcComponent(context: Context): String =
        ComponentName(context, AssistantAccessibilityService::class.java).flattenToString()

    fun isAccessibilityOn(context: Context): Boolean = runCatching {
        val cr = context.contentResolver
        val enabled = Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!enabled) return false
        val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val target = svcComponent(context)
        val shortTarget = ComponentName(context, AssistantAccessibilityService::class.java).flattenToShortString()
        current.split(':').any { it.equals(target, ignoreCase = true) || it.equals(shortTarget, ignoreCase = true) }
    }.getOrDefault(false)

    fun setAccessibility(context: Context, enable: Boolean) {
        val cr = context.contentResolver
        val svc = svcComponent(context)
        val shortSvc = ComponentName(context, AssistantAccessibilityService::class.java).flattenToShortString()
        val current = runCatching {
            Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        }.getOrDefault("")

        val list = current.split(':').map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        val isPresent = list.any { it.equals(svc, ignoreCase = true) || it.equals(shortSvc, ignoreCase = true) }

        var changed = false
        if (enable) {
            if (!isPresent) {
                list.add(svc)
                changed = true
            }
        } else {
            if (isPresent) {
                list.removeAll { it.equals(svc, ignoreCase = true) || it.equals(shortSvc, ignoreCase = true) }
                changed = true
            }
        }

        val updated = list.joinToString(":")
        val hasDirectPerm = PermissionsReader.hasWriteSecureSettings(context)

        if (hasDirectPerm) {
            runCatching {
                if (changed) {
                    Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated)
                }
                Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, if (enable || list.isNotEmpty()) 1 else 0)
            }.onFailure { e ->
                Log.w(TAG, "Direct WRITE_SECURE_SETTINGS failed, falling back to shell: ${e.message}")
                applyViaShell(context, updated, enable)
            }
        } else {
            applyViaShell(context, updated, enable)
        }
    }

    private fun applyViaShell(context: Context, updated: String, enable: Boolean) {
        val cmd = buildString {
            append("settings put secure enabled_accessibility_services '$updated'; ")
            append("settings put secure accessibility_enabled ${if (enable || updated.isNotBlank()) 1 else 0}")
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                com.aikeybridge.shell.ShizukuShell(context).exec(cmd)
            }.onFailure {
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                }
            }
        }
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val ctx = appContext ?: return
            poll(ctx)
            val pollMs = prefs(ctx).getInt(KEY_POLL_MS, DEFAULT_POLL_MS).coerceIn(200, 2000)
            handler.postDelayed(this, pollMs.toLong())
        }
    }

    fun getForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return currentForegroundPkg
        val now = System.currentTimeMillis()
        val start = now - 3000L
        val events = runCatching { usm.queryEvents(start, now) }.getOrNull() ?: return currentForegroundPkg
        var topPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                topPkg = event.packageName
            }
        }
        if (topPkg != null) currentForegroundPkg = topPkg
        return topPkg ?: currentForegroundPkg
    }

    fun poll(context: Context) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val now = System.currentTimeMillis()
        val start = if (lastCheckTs > 0 && now - lastCheckTs < 10000) lastCheckTs else (now - 2500)
        lastCheckTs = now

        val events = runCatching { usm.queryEvents(start, now) }.getOrNull() ?: return
        var topPkg: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                topPkg = event.packageName
            }
        }

        if (topPkg == null) return
        currentForegroundPkg = topPkg

        val isBank = isBankApp(context, topPkg)

        // Cơ chế NoAccWatcher (Gemini Guard): Bắt phím/khẩu lệnh trợ lý hãng kể cả khi không bật Trợ năng
        val oemAssistantPkgs = setOf("com.vivo.ai.copilot", "com.vivo.agent", "com.miui.voiceassist")
        if (topPkg in oemAssistantPkgs && !isAccessibilityOn(context)) {
            val nowTime = android.os.SystemClock.elapsedRealtime()
            if (nowTime - lastOemFireTs > 3000L) {
                lastOemFireTs = nowTime
                Log.i(TAG, "BankGuard NoAccWatcher: Phát hiện trợ lý gốc ($topPkg) -> Kích hoạt Gemini IPC")
                com.aikeybridge.service.AssistOverlay.launch(context)
            }
        }

        if (isBank) {
            if (isAccessibilityOn(context)) {
                setAccessibility(context, false)
                setBankDisabledByUs(context, true)
                Diagnostics.info("bankguard", "Phát hiện app ngân hàng/VNeID ($topPkg) -> Tự động tạm tắt Trợ năng")
                Log.i(TAG, "BankGuard: Disabled accessibility for bank: $topPkg")
            }
        } else {
            val wasDisabled = bankDisabledByUs || prefs(context).getBoolean(KEY_DISABLED_BY_US, false)
            if (wasDisabled) {
                setAccessibility(context, true)
                setBankDisabledByUs(context, false)
                Diagnostics.info("bankguard", "Đã rời app ngân hàng ($topPkg) -> Tự động khôi phục Trợ năng LTMori")
                Log.i(TAG, "BankGuard: Re-enabled accessibility after exiting bank")
            }
        }
    }

    @Synchronized
    fun sync(context: Context) {
        appContext = context.applicationContext
        val enabled = isEnabled(context)
        val hasUsage = hasUsageAccess(context)
        bankDisabledByUs = prefs(context).getBoolean(KEY_DISABLED_BY_US, false)

        if (enabled && hasUsage) {
            if (!running) {
                running = true
                lastCheckTs = System.currentTimeMillis()
                handler.removeCallbacks(loop)
                handler.post(loop)
                Log.i(TAG, "BankGuard started (poll interval: ${DEFAULT_POLL_MS}ms)")
            }
        } else {
            if (running) {
                running = false
                handler.removeCallbacks(loop)
                Log.i(TAG, "BankGuard stopped")
            }
            if (bankDisabledByUs) {
                setAccessibility(context, true)
                setBankDisabledByUs(context, false)
            }
        }
    }

    @Synchronized
    fun stop(context: Context) {
        running = false
        handler.removeCallbacks(loop)
        if (bankDisabledByUs || prefs(context).getBoolean(KEY_DISABLED_BY_US, false)) {
            setAccessibility(context, true)
            setBankDisabledByUs(context, false)
        }
    }
}
