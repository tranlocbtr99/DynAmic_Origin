package com.aikeybridge.service

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Mở trợ lý mặc định hoặc Google Circle to Search.
 * Tích hợp HiddenApiBypass để gọi trực tiếp IPC Binder của IVoiceInteractionManagerService
 * tương tự Gemini Guard 4.0, kích hoạt Gemini native siêu nhanh mà không bị hệ điều hành chặn.
 */
object AssistOverlay {

    /**
     * Mở lớp phủ trợ lý mặc định (Gemini FloatyActivity khi Google là trợ lý mặc định).
     */
    fun launch(context: Context): LaunchOutcome {
        val bundle = Bundle().apply {
            putLong("invocation_time_ms", SystemClock.elapsedRealtime())
        }
        val vis = tryVoiceInteractionService(context, bundle)
        if (vis.ok) return vis

        val assist = launchWithBundle(context, bundle, "launchAssist")
        if (assist.ok) return assist

        // Fallback: Mở trực tiếp Intent Google Voice / Gemini
        return runCatching {
            val intent = Intent(Intent.ACTION_VOICE_COMMAND)
                .setPackage(AssistantLauncher.GOOGLE_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                LaunchOutcome(true, "Mở Google Voice Command")
            } else {
                val bardIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
                    ?: context.packageManager.getLaunchIntentForPackage(AssistantLauncher.GOOGLE_PACKAGE)
                if (bardIntent != null) {
                    bardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(bardIntent)
                    LaunchOutcome(true, "Mở App Gemini")
                } else {
                    LaunchOutcome(false, "Không tìm thấy Google/Gemini")
                }
            }
        }.getOrElse {
            LaunchOutcome(false, "Lỗi mở Gemini: ${it.message}")
        }
    }

    /**
     * Mở trực tiếp giao diện Google Circle to Search (OmnientActivity - khoanh tròn tìm kiếm / dịch màn hình).
     * Truyền `omni.entry_point = 1` và `invocation_time_ms` để GsaVoiceInteractionService của Google mở
     * Circle to Search thay vì lớp phủ thoại của Gemini.
     */
    fun launchCircleToSearch(context: Context): LaunchOutcome {
        val bundle = createCtsBundle()
        val vis = tryVoiceInteractionService(context, bundle)
        if (vis.ok) return vis

        val assist = launchWithBundle(context, bundle, "CircleToSearch (launchAssist)")
        if (assist.ok) return assist

        val contextual = tryContextualSearch()
        if (contextual.ok) return contextual

        return assist
    }

    fun createCtsBundle(elapsedRealtimeMs: Long = SystemClock.elapsedRealtime()): Bundle =
        Bundle().apply {
            putLong("invocation_time_ms", elapsedRealtimeMs)
            putInt("omni.entry_point", 1)
            putBoolean("cts_trigger", true)
            putBoolean("micts_trigger", true)
        }

    private fun launchWithBundle(context: Context, bundle: Bundle, label: String): LaunchOutcome =
        runCatching {
            val manager = context.getSystemService(SearchManager::class.java)
                ?: return LaunchOutcome(false, "$label lỗi: SearchManager null")
            val ret = SearchManager::class.java.getMethod("launchAssist", Bundle::class.java).invoke(manager, bundle)
            val ok = (ret as? Boolean) ?: true
            if (ok) LaunchOutcome(true, label) else LaunchOutcome(false, "$label trả về false")
        }.getOrElse {
            val cause = it.cause ?: it
            LaunchOutcome(false, "$label lỗi: ${cause.javaClass.simpleName}: ${cause.message}")
        }

    @SuppressLint("PrivateApi")
    fun tryVoiceInteractionService(context: Context, bundle: Bundle): LaunchOutcome =
        runCatching {
            runCatching {
                HiddenApiBypass.addHiddenApiExemptions("")
            }
            val sm = Class.forName("android.os.ServiceManager")
            val getServiceMethod = sm.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "voiceinteraction") as? IBinder
                ?: return LaunchOutcome(false, "voiceinteraction service null")
            val stub = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
            val service = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
            val serviceClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")

            val method = serviceClass.methods.firstOrNull { it.name == "showSessionFromSession" }
                ?: return LaunchOutcome(false, "showSessionFromSession method not found")

            val params = if (method.parameterCount == 4) {
                arrayOf(null, bundle, 7, null)
            } else {
                arrayOf(null, bundle, 7)
            }
            val result = method.invoke(service, *params)
            val ok = (result as? Boolean) ?: true
            if (ok) {
                LaunchOutcome(true, "VoiceInteraction (IPC Direct)")
            } else {
                LaunchOutcome(false, "showSessionFromSession trả về false")
            }
        }.getOrElse {
            val cause = it.cause ?: it
            LaunchOutcome(false, "VoiceInteraction lỗi: ${cause.javaClass.simpleName}: ${cause.message}")
        }

    @SuppressLint("PrivateApi")
    private fun tryContextualSearch(): LaunchOutcome =
        runCatching {
            runCatching {
                HiddenApiBypass.addHiddenApiExemptions("")
            }
            val sm = Class.forName("android.os.ServiceManager")
            val binder = sm.getMethod("getService", String::class.java)
                .invoke(null, "contextual_search") as? IBinder
                ?: return LaunchOutcome(false, "contextual_search service null")
            val stub = Class.forName("android.app.contextualsearch.IContextualSearchManager\$Stub")
            val service = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
            val serviceClass = Class.forName("android.app.contextualsearch.IContextualSearchManager")
            serviceClass.getMethod("startContextualSearch", Int::class.javaPrimitiveType)
                .invoke(service, 1)
            LaunchOutcome(true, "CircleToSearch (ContextualSearch)")
        }.getOrElse {
            val cause = it.cause ?: it
            LaunchOutcome(false, "ContextualSearch lỗi: ${cause.javaClass.simpleName}: ${cause.message}")
        }
}
