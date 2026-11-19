package com.originisle.android.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aikeybridge.guard.BankGuard
import com.aikeybridge.service.AssistantAccessibilityService
import com.aikeybridge.shell.ShizukuShell
import com.originisle.android.ui.PREFS_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages incoming call handling for LTMori Dynamic Island.
 * Allows displaying incoming calls on the island without disrupting the user with a full-screen takeover.
 */
object LtmCallManager {
    private const val TAG = "LtmCallManager"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeCalls = mutableSetOf<String>()

    fun onNotificationPosted(context: Context, sbn: StatusBarNotification, isCall: Boolean) {
        if (!isCall) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val suppressFullScreen = prefs.getBoolean("cast_call_island_only", true)

        activeCalls.add(sbn.key)
        Log.i(TAG, "Incoming call detected from ${sbn.packageName}. suppressFullScreen=$suppressFullScreen")

        if (suppressFullScreen) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenInteractive = pm?.isInteractive ?: true

            // If the user is actively using their phone (screen is unlocked / on), suppress the full-screen popup
            if (isScreenInteractive) {
                mainHandler.postDelayed({
                    val accessibility = AssistantAccessibilityService.activeService
                    val callPkgs = setOf(
                        sbn.packageName,
                        "com.android.incallui",
                        "com.android.phone",
                        "com.google.android.dialer",
                        "com.vivo.incallui",
                        "com.bbk.incallui",
                        "com.vivo.telecom",
                        "com.zing.zalo",
                        "com.facebook.orca",
                        "org.telegram.messenger",
                        "com.whatsapp"
                    )

                    val currentTop = AssistantAccessibilityService.lastWindowPackage
                        ?: BankGuard.getForegroundPackage(context)

                    // If the current foreground is NOT a call package, it means the full screen was ALREADY
                    // successfully suppressed (e.g. via USE_FULL_SCREEN_INTENT deny).
                    // In that case, we MUST NOT press HOME because that would minimize the user's active game/app!
                    if (currentTop != null && currentTop !in callPkgs) {
                        Log.i(TAG, "Full-screen call prevented by AppOps/system; active app is $currentTop. Skipping HOME.")
                        return@postDelayed
                    }

                    // If the call app DID pop up into the foreground, minimize it back to the island
                    if (accessibility != null) {
                        Log.i(TAG, "Suppressing full-screen call window ($currentTop) via AccessibilityService HOME action")
                        accessibility.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    } else if (LtmVivoOptimizer.isShizukuAvailable(context)) {
                        Log.i(TAG, "Suppressing full-screen call window ($currentTop) via Shizuku shell HOME keyevent")
                        CoroutineScope(Dispatchers.IO).launch {
                            ShizukuShell(context).exec("input keyevent 3")
                        }
                    }
                }, 300)
            }
        }
    }

    fun onNotificationRemoved(context: Context, sbn: StatusBarNotification) {
        activeCalls.remove(sbn.key)
    }

    fun hasActiveCall(): Boolean = activeCalls.isNotEmpty()
}
