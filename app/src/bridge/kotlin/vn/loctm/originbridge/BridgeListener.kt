package vn.loctm.originbridge

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BridgeListener : NotificationListenerService() {
    private data class Active(
        val id: Int,
        val revision: Int,
        val card: SuperX.Card,
        val expiry: Runnable?
    )

    private val handler = Handler(Looper.getMainLooper())
    private val cards = mutableMapOf<String, Active>()
    private var nextId = 20000

    override fun onListenerConnected() {
        if (packageName != NATIVE_PACKAGE) return
        current = this
        getSystemService(NotificationManager::class.java)?.activeNotifications
            ?.filter { it.tag == SuperX.TAG && it.notification.channelId == SuperX.CHANNEL && it.id != SAMPLE_ID }
            ?.forEach { SuperX.end(this, it.id) }
        if (enabled(this)) activeNotifications?.forEach { source ->
            if (source.isOngoing) onNotificationPosted(source)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (packageName != NATIVE_PACKAGE) return
        if (sbn.packageName == packageName) {
            discardRejectedCopy(sbn)
            return
        }
        if (!enabled(this) || sbn.packageName in systemPackages || !appEnabled(this, sbn.packageName)) return
        val source = sbn.notification
        if ((source.flags and Notification.FLAG_GROUP_SUMMARY) != 0 ||
            source.visibility == Notification.VISIBILITY_SECRET
        ) return
        val locked = getSystemService(KeyguardManager::class.java)?.isDeviceLocked != false
        if (locked && !lockscreen(this)) return
        try {
            publish(sbn, locked)
        } catch (error: RuntimeException) {
            Diagnostics.record(this, "FORWARD_ERROR_" + error.javaClass.simpleName)
        }
    }

    private fun publish(sbn: StatusBarNotification, locked: Boolean) {
        val source = sbn.notification
        val extras = source.extras
        val app = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(sbn.packageName)
        val icon = source.smallIcon ?: Icon.createWithResource(this, android.R.drawable.ic_dialog_info)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX)
        val progress = if (progressMax > 0 && !extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)) {
            (extras.getInt(Notification.EXTRA_PROGRESS).toLong() * 100 / progressMax).toInt().coerceIn(0, 100)
        } else null
        val redacted = locked || lockscreen(this)
        val message = extras.getParcelableArray(Notification.EXTRA_MESSAGES, android.os.Bundle::class.java)
            ?.lastOrNull()?.getCharSequence("text")
        val title = if (redacted) app else {
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty().ifBlank { app }
        }
        val body = if (redacted) "Có thông báo mới" else sequenceOf(
            message, extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT), extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        ).mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }.firstOrNull() ?: app
        val click = if (redacted) null else source.contentIntent ?: runCatching {
            packageManager.getLaunchIntentForPackage(sbn.packageName)?.let {
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
            }
        }.getOrNull()
        val ongoing = sbn.isOngoing || source.category == Notification.CATEGORY_CALL || progressMax > 0
        val actions = if (redacted) emptyList() else source.actions.orEmpty().filter {
            it.actionIntent != null && it.remoteInputs.isNullOrEmpty() && !it.isContextual && !it.title.isNullOrBlank()
        }.take(1)
        val card = SuperX.Card(title.take(100), body.take(300), app, icon, click, ongoing,
            progress.takeUnless { redacted }, actions)
        val previous = cards[sbn.key]
        if (previous?.card == card) return
        val id = previous?.id ?: nextId++
        val revision = (previous?.revision ?: 0) + 1
        val result = SuperX.post(this, id, revision, card, lockscreen(this))
        if (result != "SUPERX_POSTED") {
            Diagnostics.record(this, result)
            return
        }
        previous?.expiry?.let(handler::removeCallbacks)
        val expiry = if (ongoing) null else Runnable { remove(sbn.key) }
        cards[sbn.key] = Active(id, revision, card, expiry)
        expiry?.let { handler.postDelayed(it, 8000L) }
    }

    private fun discardRejectedCopy(sbn: StatusBarNotification) {
        if (sbn.tag != null || sbn.notification.channelId != SuperX.CHANNEL ||
            sbn.notification.extras.get("SUPERX_VALUE")?.toString() != "false"
        ) return
        getSystemService(NotificationManager::class.java)?.cancel(sbn.id)
        Diagnostics.record(this, "SUPERX_REJECTED")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = remove(sbn.key)

    private fun remove(key: String) {
        val active = cards.remove(key) ?: return
        active.expiry?.let(handler::removeCallbacks)
        runCatching { SuperX.end(this, active.id) }
    }

    override fun onListenerDisconnected() {
        if (current === this) current = null
        cards.keys.toList().forEach(::remove)
        if (enabled(this)) requestRebind(android.content.ComponentName(this, BridgeListener::class.java))
    }

    override fun onDestroy() {
        if (current === this) current = null
        cards.keys.toList().forEach(::remove)
        super.onDestroy()
    }

    companion object {
        private var current: BridgeListener? = null
        const val NATIVE_PACKAGE = "com.autonavi.minimap"
        const val SAMPLE_ID = 61061
        private val systemPackages = setOf("android", "com.android.systemui")
        private fun prefs(context: Context) =
            context.getSharedPreferences("superx", Context.MODE_PRIVATE)

        fun enabled(context: Context): Boolean =
            context.packageName == NATIVE_PACKAGE && prefs(context).getBoolean("forward", false)

        fun appEnabled(context: Context, pkg: String): Boolean =
            pkg !in prefs(context).getStringSet("excluded_apps", emptySet()).orEmpty()

        fun setExcludedApps(context: Context, packages: Set<String>) {
            prefs(context).edit().putStringSet("excluded_apps", packages).apply()
            current?.let { listener ->
                listener.activeNotifications?.filter { it.packageName in packages }?.forEach {
                    listener.remove(it.key)
                }
            }
        }

        fun lockscreen(context: Context): Boolean = prefs(context).getBoolean("lockscreen", false)

        fun setLockscreen(context: Context, enabled: Boolean) {
            prefs(context).edit().putBoolean("lockscreen", enabled).apply()
            endActive(context)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            prefs(context).edit().putBoolean("forward", enabled).apply()
            if (!enabled) endActive(context)
        }

        private fun endActive(context: Context) {
            current?.let { listener ->
                listener.cards.keys.toList().forEach(listener::remove)
            }
            context.getSystemService(NotificationManager::class.java)?.activeNotifications
                ?.filter { it.tag == SuperX.TAG && it.notification.channelId == SuperX.CHANNEL }
                ?.forEach { runCatching { SuperX.end(context, it.id) } }
        }
    }
}
