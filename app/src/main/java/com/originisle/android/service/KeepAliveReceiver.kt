package com.originisle.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ui.LtmIslandHelper

/**
 * Catches system unlock (USER_PRESENT), boot (BOOT_COMPLETED), power connected, and AlarmManager ticks
 * to ensure NotificationCastListener and PlaygroundService are permanently alive, bound, and displaying the island.
 */
class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i("KeepAliveReceiver", "onReceive: action=$action -> ensuring services are active")
        runCatching { NotificationCastListener.ensureEnabled(context) }
        runCatching { NotificationCastListener.forceRebind(context) }
        runCatching { PlaygroundService.keepAlive(context) }
        runCatching { com.aikeybridge.guard.BankGuard.sync(context) }
        runCatching {
            LtmIslandHelper.updateIdlePreset(context)
            LtmIslandHelper.syncIdleTicker(context)
        }
    }
}
