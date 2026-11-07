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
        val expiry: Runnable?,
        val retried: Boolean = false
    )

    private val handler = Handler(Looper.getMainLooper())
    private val cards = mutableMapOf<String, Active>()
    private var nextId = 20000
    private var idleVisible = false
    private var idleRevision = 0
    private var idleEnding = false
    private val pending = linkedMapOf<String, StatusBarNotification>()
    private val finishIdleHandoff = Runnable {
        idleEnding = false
        val waiting = pending.values.toList()
        pending.clear()
        waiting.forEach(::onNotificationPosted)
        refreshIdle()
    }
    private var lastOutcome = ""
    private fun outcome(value: String) {
        if (value != lastOutcome) {
            lastOutcome = value
            Diagnostics.record(this, value)
        }
    }

    override fun onListenerConnected() {
        if (packageName != NATIVE_PACKAGE) return
        current = this
        if (accepted(this)) outcome("LISTENER_CONNECTED")
        getSystemService(NotificationManager::class.java)?.activeNotifications
            ?.filter { it.tag == SuperX.TAG && it.notification.channelId == SuperX.CHANNEL && it.id != SAMPLE_ID }
            ?.forEach { SuperX.end(this, it.id) }
        if (enabled(this)) activeNotifications?.forEach { source ->
            if (source.isOngoing) onNotificationPosted(source)
        }
        refreshIdle()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (packageName != NATIVE_PACKAGE) return
        if (sbn.packageName == packageName) {
            discardRejectedCopy(sbn)
            return
        }
        if (!accepted(this) || sbn.packageName in systemPackages) return
        if (!enabled(this)) return outcome("FORWARD_DISABLED")
        if (!appEnabled(this, sbn.packageName)) return outcome("SOURCE_EXCLUDED")
        val source = sbn.notification
        if (source.visibility == Notification.VISIBILITY_SECRET) return outcome("SOURCE_SECRET")
        if ((source.flags and Notification.FLAG_GROUP_SUMMARY) != 0 &&
            activeNotifications?.any {
                it.groupKey == sbn.groupKey && it.packageName == sbn.packageName &&
                    (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            } == true
        ) return outcome("SOURCE_GROUP_DUPLICATE")
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != false
        if (locked && !lockscreen(this)) return outcome("SOURCE_LOCKED")
        if (idleVisible || idleEnding) {
            pending[sbn.key] = sbn
            hideIdle()
            return
        }
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
            outcome(result)
            return
        }
        outcome("FORWARD_POSTED")
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
        if (sbn.id == IDLE_ID) idleVisible = false
        if (!accepted(this)) return
        outcome("SUPERX_REJECTED")
        val entry = cards.entries.firstOrNull { it.value.id == sbn.id } ?: return
        val active = entry.value
        if (enabled(this) && !active.retried &&
            (getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == false || lockscreen(this))
        ) {
            val basic = active.card.copy(
                icon = Icon.createWithResource(this, android.R.drawable.ic_dialog_info),
                actions = emptyList(),
                progress = null
            )
            val result = runCatching {
                SuperX.post(this, active.id, active.revision + 1, basic, lockscreen(this))
            }.getOrNull()
            if (result == "SUPERX_POSTED") {
                cards[entry.key] = active.copy(revision = active.revision + 1, card = basic, retried = true)
                return
            }
        }
        cards.remove(entry.key)
        active.expiry?.let(handler::removeCallbacks)
        refreshIdle()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != packageName) {
            pending.remove(sbn.key)
            remove(sbn.key)
        }
    }

    private fun refreshIdle() {
        if (current !== this || !idleEnabled(this) || cards.isNotEmpty()) {
            hideIdle()
            return
        }
        if (idleVisible || idleEnding) return
        val intent = android.content.Intent(this, MainActivity::class.java)
        val open = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val card = SuperX.Card("LTMori", "Đảo đang chờ", "LTMori",
            Icon.createWithResource(this, android.R.drawable.ic_dialog_info), open, ongoing = true, idle = true)
        val result = runCatching { SuperX.post(this, IDLE_ID, ++idleRevision, card, false) }
            .getOrElse { "IDLE_ERROR_" + it.javaClass.simpleName }
        idleVisible = result == "SUPERX_POSTED"
        Diagnostics.record(this, if (idleVisible) "IDLE_POSTED" else result)
    }

    private fun hideIdle() {
        if (!idleVisible) return
        idleVisible = false
        runCatching { SuperX.end(this, IDLE_ID) }
        idleEnding = true
        handler.postDelayed(finishIdleHandoff, SuperX.END_DELAY_MS + 50)
    }

    private fun remove(key: String) {
        val active = cards.remove(key) ?: return
        active.expiry?.let(handler::removeCallbacks)
        runCatching { SuperX.end(this, active.id) }
        refreshIdle()
    }

    override fun onListenerDisconnected() {
        if (current === this) current = null
        hideIdle()
        handler.removeCallbacks(finishIdleHandoff)
        pending.clear()
        idleEnding = false
        cards.keys.toList().forEach(::remove)
        if (enabled(this) || idleEnabled(this)) requestRebind(android.content.ComponentName(this, BridgeListener::class.java))
    }

    override fun onDestroy() {
        if (current === this) current = null
        hideIdle()
        handler.removeCallbacks(finishIdleHandoff)
        pending.clear()
        idleEnding = false
        cards.keys.toList().forEach(::remove)
        super.onDestroy()
    }

    companion object {
        private var current: BridgeListener? = null
        const val NATIVE_PACKAGE = "com.autonavi.minimap"
        const val SAMPLE_ID = 61061
        private const val IDLE_ID = 61060
        private val systemPackages = setOf("android", "com.android.systemui")
        private fun prefs(context: Context) =
            context.getSharedPreferences("superx", Context.MODE_PRIVATE)

        fun connectionStatus(context: Context): String {
            val manager = context.getSystemService(NotificationManager::class.java)
            val component = android.content.ComponentName(context, BridgeListener::class.java)
            return when {
                manager?.isNotificationListenerAccessGranted(component) != true -> "Chưa cấp quyền đọc"
                current == null -> "Đã cấp quyền · Đang chờ kết nối"
                else -> "Đã kết nối"
            }
        }

        fun enabled(context: Context): Boolean =
            accepted(context) && prefs(context).getBoolean("forward", false)

        fun accepted(context: Context): Boolean =
            context.packageName == NATIVE_PACKAGE && prefs(context).getInt("policy_version", 0) == 1

        fun accept(context: Context) {
            prefs(context).edit().putInt("policy_version", 1).apply()
        }

        fun withdraw(context: Context) {
            prefs(context).edit().remove("policy_version").remove("forward").remove("idle").apply()
            endActive(context)
            context.deleteFile("diagnostics.txt")
        }

        fun idleEnabled(context: Context): Boolean =
            accepted(context) && prefs(context).getBoolean("idle", false)

        fun setIdle(context: Context, value: Boolean) {
            prefs(context).edit().putBoolean("idle", value).apply()
            current?.refreshIdle()
        }

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
            endActive(context, includeIdle = false)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            prefs(context).edit().putBoolean("forward", enabled).apply()
            if (!enabled) endActive(context, includeIdle = false)
            else current?.let { listener ->
                listener.activeNotifications?.filter {
                    it.packageName != context.packageName && it.packageName !in systemPackages &&
                        appEnabled(context, it.packageName)
                }?.maxByOrNull { it.postTime }?.let(listener::onNotificationPosted)
            }
        }

        private fun endActive(context: Context, includeIdle: Boolean = true) {
            current?.let { listener ->
                listener.pending.clear()
                listener.cards.keys.toList().forEach(listener::remove)
                if (includeIdle) listener.hideIdle()
            }
            context.getSystemService(NotificationManager::class.java)?.activeNotifications
                ?.filter { it.tag == SuperX.TAG && it.notification.channelId == SuperX.CHANNEL &&
                    (includeIdle || it.id != IDLE_ID) }
                ?.forEach { runCatching { SuperX.end(context, it.id) } }
        }
    }
}
