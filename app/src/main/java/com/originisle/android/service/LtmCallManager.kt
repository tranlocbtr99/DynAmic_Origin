package com.originisle.android.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aikeybridge.service.AssistantAccessibilityService
import com.originisle.android.ui.PREFS_NAME

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
                    if (accessibility != null) {
                        Log.i(TAG, "Suppressing full-screen call window via AccessibilityService HOME action")
                        accessibility.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
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
