package com.originisle.android.island

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.LtmIslandHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * Posts, updates and ends OriginIsland "SuperX" cards.
 *
 * Callers drive it with an [Intent] full of `oi_*` extras (the same contract the original app
 * used — see [readAndPost]); the service maps those onto [OriginIslandBuilder.buildBundle] and
 * posts the result as a notification tagged [OriginIslandConstants.SUPERX_TAG]. That tag is what
 * makes OriginOS render it in the island rather than the shade.
 */
class PlaygroundService : Service() {
    private val lastCastByApp = java.util.concurrent.ConcurrentHashMap<String, Long>()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        // OriginOS refused a card we posted (sent by the notification listener with its "id").
        const val ACTION_REJECTED = "ACTION_REJECTED"
        // Just brings the foreground service up (no card) so the process — and the notification
        // listener living in it — stays alive for background casts.
        const val ACTION_KEEPALIVE = "ACTION_KEEPALIVE"

        /** Start/keep the foreground service alive. Safe to call from a foreground context. */
        fun keepAlive(context: Context) {
            context.startService(Intent(context, PlaygroundService::class.java).setAction(ACTION_KEEPALIVE))
        }

        // Keep-alive channel created at IMPORTANCE_NONE (blocked): a foreground service on a blocked
        // channel keeps running but its notification is SUPPRESSED — no status-bar icon, even on
        // OriginOS which force-badges IMPORTANCE_MIN foreground services.
        const val CHANNEL_ID = "originisle_keepalive"

        /** Ids of cards currently on the island — used to detect a crowded island. */
        @Volatile
        var activeCardIds: Set<Int> = emptySet()

        /** Timestamp of the last transient (non-ongoing, e.g. message) card posted. */
        @Volatile
        var lastTransientCardPostTime: Long = 0L

        const val ORIGIN_CHANNEL_ID = "originisle_island_hi"
        const val FGS_ID = 9999

        /** PendingIntent request code for the [onTaskRemoved] restart alarm. */
        private const val RESTART_REQUEST = 7001
        private const val WATCHDOG_REQUEST = 7002
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L

        /** Layouts a card falls back from to the plain base card when OriginOS refuses it. */
        private val FANCY_TEMPLATES = setOf(
            OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL,
            OriginIslandConstants.TEMPLATE_BUTTONS,
            OriginIslandConstants.TEMPLATE_DRIVING_NAVI,
        )
    }

    private lateinit var notificationManager: NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isForegroundActive = false

    private val activeIds = linkedSetOf<Int>()
    private val originScenes = HashMap<Int, String>()
    private val originChangeRecord = HashMap<Int, Int>()
    // Pending auto-dismiss timers, keyed by card id — only ever holds entries for non-live cards
    // (see readAndPost); a live card's id is simply never added here.
    private val autoDismissTasks = ConcurrentHashMap<Int, Runnable>()
    // Which card id currently occupies each scene. Only NAVIGATION is granted on most devices, so
    // every card shares it; vivo merges same-scene cards, so a new card must end the previous one
    // or it inherits stale fields (a ride ETA lingering under a flight card, etc.).
    private val sceneCurrentId = HashMap<String, Int>()
    // The latest post of each card on the island, so one OriginOS refuses can go up again, simpler.
    private val lastPosts = HashMap<Int, Intent>()
    // Cards OriginOS refused once: plain base card, no buttons, until they end.
    private val plainIds = HashSet<Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        OriginIslandBuilder.grantScenes(this)
        restoreIdleCardIfEnabled()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A null intent is a START_REDELIVER_INTENT relaunch and ACTION_KEEPALIVE is the alarm/UI anchor
        // request: both only mean "stay up", which is pointless once casting is off. Explicit card
        // actions still work with it off, so the sample buttons are unaffected.
        if ((intent == null || intent.action == ACTION_KEEPALIVE) && !isCastingEnabled()) {
            cancelRestartAlarm()
            cancelHeartbeatWatchdog()
            stopSelf()
            return START_NOT_STICKY
        }
        ensureForeground()
        scheduleHeartbeatWatchdog()
        Log.i("PlaygroundService", "onStartCommand: action=${intent?.action}, id=${intent?.getIntExtra("id", -1)}")
        if (intent == null || intent.action == ACTION_KEEPALIVE) {
            restoreIdleCardIfEnabled()
        }
        try {
            when (intent?.action) {
                ACTION_START -> readAndPost(intent)
                ACTION_CANCEL -> cancelNotification(intent)
                ACTION_REJECTED -> onRejected(intent.getIntExtra("id", -1))
                ACTION_STOP -> stopEverything()
                ACTION_KEEPALIVE -> Unit // handled above via restoreIdleCardIfEnabled()
            }
        } catch (e: Throwable) {
            Log.e("PlaygroundService", "onStartCommand failed", e)
        }
        // START_REDELIVER_INTENT so the system relaunches the service (and our process + listener) with intent if killed.
        return START_REDELIVER_INTENT
    }

    /**
     * In addition to START_REDELIVER_INTENT, arm an immediate ELAPSED_REALTIME_WAKEUP alarm
     * to revive the process 500ms later if swiped away from Recents.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // With casting off there is nothing to come back for, and arming the alarm would revive the
        // service a second after the user swiped it away — for good, since nothing else stops it.
        if (!isCastingEnabled()) {
            cancelRestartAlarm()
            cancelHeartbeatWatchdog()
            stopSelf()
            return
        }
        runCatching { ensureForeground() }
        runCatching {
            restartAlarmIntent(PendingIntent.FLAG_UPDATE_CURRENT)?.let { restart ->
                val am = getSystemService(AlarmManager::class.java) ?: return@runCatching
                val triggerAt = SystemClock.elapsedRealtime() + 500L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restart)
                    } catch (e: SecurityException) {
                        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restart)
                    }
                } else {
                    am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restart)
                }
            }
        }
    }

    /** Matches the listener's own gate — either card source keeps the service worth running. */
    private fun isCastingEnabled(): Boolean = LtmIslandHelper.isIslandEnabled(this)

    private fun restoreIdleCardIfEnabled() {
        if (!isCastingEnabled()) return
        if (!LtmIslandHelper.isIdleCardEnabled(this)) return
        if (activeIds.any { it != 0xee84 }) return
        mainHandler.post {
            try {
                LtmIslandHelper.updateIdlePreset(this)
                LtmIslandHelper.syncIdleTicker(this)
            } catch (t: Throwable) {
                Log.w("PlaygroundService", "restoreIdleCardIfEnabled failed: ${t.message}")
            }
        }
    }

    /** [flags] picks between arming (FLAG_UPDATE_CURRENT) and looking one up (FLAG_NO_CREATE). */
    private fun restartAlarmIntent(flags: Int): PendingIntent? = PendingIntent.getForegroundService(
        this,
        RESTART_REQUEST,
        Intent(this, PlaygroundService::class.java).setAction(ACTION_KEEPALIVE),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelRestartAlarm() {
        runCatching {
            restartAlarmIntent(PendingIntent.FLAG_NO_CREATE)?.let {
                getSystemService(AlarmManager::class.java)?.cancel(it)
                it.cancel()
            }
        }
    }

    private fun watchdogAlarmIntent(flags: Int): PendingIntent? = PendingIntent.getForegroundService(
        this,
        WATCHDOG_REQUEST,
        Intent(this, PlaygroundService::class.java).setAction(ACTION_KEEPALIVE),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun scheduleHeartbeatWatchdog() {
        if (!isCastingEnabled()) return
        runCatching {
            val am = getSystemService(AlarmManager::class.java) ?: return@runCatching
            val pi = watchdogAlarmIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return@runCatching
            val triggerAt = SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL_MS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            }
        }
    }

    private fun cancelHeartbeatWatchdog() {
        runCatching {
            watchdogAlarmIntent(PendingIntent.FLAG_NO_CREATE)?.let {
                getSystemService(AlarmManager::class.java)?.cancel(it)
                it.cancel()
            }
        }
    }

    private fun ensureForeground() {
        if (isForegroundActive) return
        // Starts UNCONDITIONALLY. This used to be skipped when the keep-alive AccessibilityService
        // was enabled, assuming it anchored the process — but a swipe-away kill makes
        // AccessibilityManagerService mark the service CRASHED and never rebind it, so it stayed
        // "enabled" while completely dead and nothing restarted us until a reboot. The status-bar
        // icon that skip was avoiding is handled instead by CHANNEL_ID being IMPORTANCE_NONE.
        createNotificationChannel(CHANNEL_ID)
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LTMori")
            .setContentText("Dịch vụ thông báo đảo LTMori")
            // A fully transparent small icon: OriginOS force-shows foreground-service icons in the
            // status bar even at IMPORTANCE_MIN, so we make the icon invisible. The entry still shows
            // (with text) in the expanded notification panel; only the status-bar glyph disappears.
            .setSmallIcon(R.drawable.ic_transparent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOngoing(true)
            .build()
        // Both calls can throw: the onTaskRemoved alarm starts this service from the background, and
        // that is only allowed while the app holds the battery-optimisation exemption (which
        // onboarding lets the user skip).
        isForegroundActive = runCatching {
            startForeground(FGS_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }.recoverCatching {
            startForeground(FGS_ID, n)
        }.isSuccess
        // Catching isn't enough to survive it: that alarm uses startForegroundService(), so the
        // system kills us anyway unless we either go foreground or stop. Stop cleanly instead.
        if (!isForegroundActive) stopSelf()
    }

    // --- posting -----------------------------------------------------------------

    /** Read the `oi_*` extras contract off [intent] and post/refresh a SuperX card. */
    private fun readAndPost(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id == -1) return
        if (!LtmIslandHelper.isIslandEnabled(this)) return
        if (id == 0xee84 && !LtmIslandHelper.isIdleCardEnabled(this)) return
        LtmIslandHelper.applyIdleCardText(this, intent)
        lastPosts[id] = intent
        val plain = id in plainIds

        val scene = intent.getStringExtra("oi_scene").orEmptyOr("NAVIGATION")
        val title = intent.getStringExtra("title").orEmpty()
        val text = intent.getStringExtra("text").orEmpty()
        val leftContent = intent.getStringExtra("oi_left_content").orEmpty()
        val rightContent = intent.getStringExtra("oi_right_content").orEmpty()
        val statusChip = intent.getStringExtra("status_chip_text").orEmpty()

        val template = intent.getIntExtra("oi_template", 0)
            .let { if (plain && it in FANCY_TEMPLATES) OriginIslandConstants.TEMPLATE_BASE else it }
        val rightTemplate = intent.getIntExtra("oi_right_template", 0)

        val extra1 = intent.getStringExtra("oi_extra1").orEmpty()
        val extra2 = intent.getStringExtra("oi_extra2").orEmpty()
        val extra3 = intent.getStringExtra("oi_extra3").orEmpty()
        val extra4 = intent.getStringExtra("oi_extra4").orEmpty()
        val subtext = intent.getStringExtra("subtext")
        val navMsg = intent.getStringExtra("oi_nav_msg")
        val navAssist = intent.getStringExtra("oi_nav_assist")

        val bgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_bg_color"), 0)
        val fgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_fg_color"), 0)
        val cardBgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_card_bg_color"), 0)
        val lightColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_light_color"), 0)

        val showProgress = intent.getBooleanExtra("show_progress", false)
        val progressMax = intent.getIntExtra("progress_max", 0)
        val rawProgress = intent.getIntExtra("progress", 0)
        val progress = when {
            intent.hasExtra("oi_progress") -> intent.getIntExtra("oi_progress", 0)
            showProgress && progressMax > 0 -> rawProgress * 100 / progressMax
            else -> rawProgress
        }

        val keepDuration = intent.getIntExtra("oi_keep_duration", 0)
        // Surfaces the card appears on: lockscreen|statusbar|AOD — NOT the plain notification-shade
        // bit (1). The SuperX post is a real, HIGH-importance notification under Origin Isle's own
        // identity; including the notification bit made it ALSO show as a duplicate shade entry
        // ("Origin Isle: Starting navigation" etc, mirroring whatever the source app's text was),
        // on top of the island rendering. Dropping it should leave the island/lockscreen/AOD intact.
        //
        // The LOCKSCREEN bit specifically is NOT a per-card decision — it's owned centrally by the
        // "Live card on lockscreen" setting (Cast tab), so every card behaves the same regardless of
        // what it passes in oi_displays. Strip whatever the card asked for, then re-add it only if the
        // user opted in.
        val baseDisplays = intent.getIntExtra(
            "oi_displays",
            OriginIslandConstants.DISPLAY_STATUSBAR or OriginIslandConstants.DISPLAY_AOD,
        )
        val lockscreenOptIn = getSharedPreferences(PREFS_NAME, 0)
            .getBoolean("cast_lockscreen_live_card", true)
        val hideFromShade = getSharedPreferences(PREFS_NAME, 0).getBoolean("cast_hide_from_shade", false)
        val displays = ((baseDisplays and OriginIslandConstants.DISPLAY_LOCKSCREEN.inv()) or
            (if (lockscreenOptIn) OriginIslandConstants.DISPLAY_LOCKSCREEN else 0)).let {
            if (hideFromShade) it and OriginIslandConstants.DISPLAY_NOTIFICATION.inv() else it
        }
        val isIdleCard = (id == 0xee84) || intent.getBooleanExtra("oi_is_idle_ticker", false)
        val isCall = intent.getBooleanExtra("is_call", false)
        val isLiquidGlassHandled = intent.getBooleanExtra("is_liquid_glass_handled", false)
        val bannerStyle = getSharedPreferences(PREFS_NAME, 0).getString("notif_banner_style", "liquid_glass")
        val isUpdatingExisting = activeIds.contains(id)
        val expandOnNotif = getSharedPreferences(PREFS_NAME, 0).getBoolean("cast_expand_on_notification", true)

        val showNotify = if (isIdleCard) false else (intent.getBooleanExtra("oi_show_notify", true) &&
            !getSharedPreferences(PREFS_NAME, 0).getBoolean("cast_hide_from_shade", false))
        val islandNotify = if (isIdleCard) false else intent.getBooleanExtra("oi_island_notify", false)
        val islandShowTime = intent.getIntExtra("oi_island_show_time", 0)

        var forceShow = when {
            isIdleCard -> false
            isUpdatingExisting -> false
            isLiquidGlassHandled || bannerStyle == "liquid_glass" || bannerStyle == "capsule_only" -> false
            isCall -> true
            else -> intent.getBooleanExtra("oi_force_show", false) || expandOnNotif
        }
        var forceShowCard = when {
            isIdleCard -> false
            isUpdatingExisting -> false
            isLiquidGlassHandled || bannerStyle == "liquid_glass" || bannerStyle == "capsule_only" -> false
            isCall -> true
            else -> intent.getBooleanExtra("oi_force_show_card", false) || expandOnNotif
        }

        val sourcePkg = intent.getStringExtra("source_pkg")
        val dedupEnabled = getSharedPreferences(PREFS_NAME, 0).getBoolean("cast_dedup_same_app", true)
        val cooldownSec = getSharedPreferences(PREFS_NAME, 0).getInt("cast_dedup_cooldown_sec", 10)
        val now = System.currentTimeMillis()
        val suppressExpand = dedupEnabled && sourcePkg != null &&
            (now - (lastCastByApp[sourcePkg] ?: 0L)) < cooldownSec * 1000L
        if (dedupEnabled && sourcePkg != null && !suppressExpand) lastCastByApp[sourcePkg] = now
        if (suppressExpand) { forceShow = false; forceShowCard = false }
        val dismissWhenKill = if (isIdleCard) false else intent.getBooleanExtra("oi_dismiss_when_kill", true)
        val keepScreenOn = intent.getBooleanExtra("oi_keep_screen_on", false)
        val disableInvert = intent.getBooleanExtra("oi_disable_invert", true)
        val showRightIcon = intent.getBooleanExtra("oi_show_right_icon", true)
        val showLeftIcon = intent.getBooleanExtra("oi_show_left_icon", true)
        val lightMode = intent.getIntExtra("oi_light_mode", 0)
        val generatingStatus = intent.getIntExtra("oi_generating_status", 0)
        val iconStatusType = intent.getIntExtra("oi_icon_status_type", -1)
        val waveState = intent.getIntExtra("oi_wave_state", 1)
        val waveColorList = intent.getStringArrayListExtra("oi_wave_color_list")
        val progressState = if (intent.getBooleanExtra("progress_indeterminate", false)) 1 else 0

        val customTemplate: RemoteViews? =
            intent.getParcelableExtra("oi_custom_template", RemoteViews::class.java)

        // Icons: prefer the ones the listener captured for this cast id, else the app icon.
        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        val baseIcon: Icon = IconCache.activeSmallIcons[id]
            ?: Icon.createWithResource(this, iconRes)
        // Optional distinct island icons (e.g. home/away club crests); fall back to the base icon.
        val leftIcon: Icon = intent.getParcelableExtra("oi_left_icon", Icon::class.java) ?: baseIcon
        val rightIcon: Icon = intent.getParcelableExtra("oi_right_icon", Icon::class.java) ?: baseIcon
        val largeIcon: Icon? = intent.getParcelableExtra("oi_large_icon", Icon::class.java)
            ?: IconCache.activeLargeIcons[id]
            ?: IconCache.activeLargeBitmaps[id]?.let { Icon.createWithBitmap(it) }
        val subImage: Icon? = intent.getParcelableExtra("oi_sub_image", Icon::class.java)
        val progressNodeIcons = intent.getParcelableArrayListExtra("oi_progress_nodes", Icon::class.java).orEmpty()
        val progressIndicatorIcon = intent.getParcelableExtra("oi_progress_indicator", Icon::class.java)
        val progressColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_progress_color"), 0)

        // Actions -> OriginActions.
        val actionSourcePkg = intent.getStringExtra("source_pkg")
        val actions = if (plain) emptyList() else intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
            ?.map { a ->
                OriginIslandBuilder.OriginAction(
                    title = a.title?.toString().orEmpty(),
                    // getIcon() = the Icon; the bare `icon` field is a legacy int res
                    icon = a.getIcon()?.ownedBy(actionSourcePkg),
                    pendingIntent = a.actionIntent,
                )
            } ?: emptyList()

        val clickResp: PendingIntent? =
            intent.getParcelableExtra("source_content_intent", PendingIntent::class.java)
                ?: intent.getParcelableExtra("click_resp", PendingIntent::class.java)

        val leftDoubleLine = intent.getStringArrayListExtra("oi_left_doubleline") ?: emptyList()
        val rightDoubleLine = intent.getStringArrayListExtra("oi_right_doubleline") ?: emptyList()
        val buttonTitles = if (plain) emptyList() else intent.getStringArrayListExtra("oi_button_titles") ?: emptyList()

        // capsule / right-island chip text: prefer explicit chip, else right content.
        val chip = statusChip.ifBlank { rightContent }

        val changeRecord = ((originChangeRecord[id] ?: 0) + 1).also { originChangeRecord[id] = it }

        val bundle = OriginIslandBuilder.buildBundle(
            context = this,
            template = template,
            title = title,
            content = text,
            leftContent = leftContent,
            rightContent = chip,
            extra1 = extra1, extra2 = extra2, extra3 = extra3, extra4 = extra4,
            rightTemplate = rightTemplate,
            defaultIcon = baseIcon,
            leftIcon = leftIcon,
            rightIcon = rightIcon,
            progress = progress,
            bgColor = bgColor,
            fgColor = fgColor,
            scene = scene,
            clickResp = clickResp,
            largeIcon = largeIcon,
            subText = subtext,
            subImage = subImage,
            navMsg = navMsg,
            navAssistText = navAssist,
            actions = actions,
            keepDuration = keepDuration,
            displays = displays,
            dismissWhenKill = dismissWhenKill,
            islandShowTime = islandShowTime,
            forceShow = forceShow,
            forceShowCard = forceShowCard,
            showRightIcon = showRightIcon,
            showLeftIcon = showLeftIcon,
            progressState = progressState,
            progressNodeIcons = progressNodeIcons,
            progressIndicatorIcon = progressIndicatorIcon,
            progressColor = progressColor,
            cardBgColor = cardBgColor,
            cardRing = intent.getIntExtra("oi_card_ring", -1),
            keepScreenOn = keepScreenOn,
            disableInvertColor = disableInvert,
            lightColor = lightColor,
            lightMode = lightMode,
            generatingStatus = generatingStatus,
            iconStatusType = iconStatusType,
            leftDoubleLine = leftDoubleLine,
            rightDoubleLine = rightDoubleLine,
            buttonTitles = buttonTitles,
            customTemplate = customTemplate,
            waveState = waveState,
            waveColorList = waveColorList,
            changeRecord = changeRecord,
            showNotify = showNotify,
            islandNotify = islandNotify,
            islandClick = intent.getIntExtra("oi_island_click", OriginIslandConstants.ISLAND_CLICK_SHOW_CARD),
        )

        val sourceApp = intent.getStringExtra("source_app").orEmpty()
        val isOngoing = intent.getBooleanExtra("is_ongoing", false)
        if (!isOngoing && !isIdleCard) {
            lastTransientCardPostTime = now
        }

        if (id != 0xee84 && activeIds.contains(0xee84)) {
            endOrigin(0xee84, "NAVIGATION")
            notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, 0xee84)
            activeIds.remove(0xee84)
            originChangeRecord.remove(0xee84)
            originScenes.remove(0xee84)
            lastPosts.remove(0xee84)
            plainIds.remove(0xee84)
        }

        // Scene replace ONLY between tester/sports sample cards (id >= 40000) so switching samples
        // doesn't leave stale fields. Real cast notifications (id 20000-29999) are NOT ended against
        // each other — vivo happily stacks several live cards (a main one + reduced ones), and ending
        // one mid-update is what left a match card half-rendered (one badge / one score). Cards are
        // still cleared when their own notification is dismissed (onNotificationRemoved -> cancel).
        if (id >= 40000) {
            val prevInScene = sceneCurrentId[scene]
            val lastIntent = lastPosts[id]
            val templateChanged = lastIntent != null && (
                lastIntent.getIntExtra("oi_template", 0) != template ||
                lastIntent.getIntExtra("oi_right_template", 0) != rightTemplate
            )
            if ((prevInScene != null && prevInScene != id && prevInScene >= 40000) || templateChanged) {
                val toEnd = if (templateChanged) id else prevInScene!!
                endOrigin(toEnd, scene)
                activeIds.remove(toEnd)
                originChangeRecord.remove(toEnd)
                originScenes.remove(toEnd)
                lastPosts.remove(toEnd)
                plainIds.remove(toEnd)
            }
            sceneCurrentId[scene] = id
        }

        activeIds.add(id)
        originScenes[id] = scene
        activeCardIds = activeIds.toSet()
        com.originisle.android.ui.LtmIslandHelper.limitMessageSurfaces(this, intent, bundle)
        if (isIdleCard) com.originisle.android.ui.LtmIslandHelper.sanitizeIdleBundle(bundle)
        postSuperX(id, iconRes, title, text, sourceApp, isOngoing, scene, bundle, clickResp)

        // Auto-dismiss: only for non-live cards (isOngoing is the same live/persistent flag the
        // card posters set — media, sports and any ongoing/call/progress GenericCard all mark
        // themselves live), and only when the user opted into a duration in Cast settings (default 6s for messages).
        autoDismissTasks.remove(id)?.let { mainHandler.removeCallbacks(it) }
        val autoDismissSec = getSharedPreferences(PREFS_NAME, 0).getInt("cast_auto_dismiss_seconds", 0)
        val timeoutSec = if (autoDismissSec > 0) autoDismissSec else if (!isOngoing && !isIdleCard) 6 else 0
        if (timeoutSec > 0 && !isOngoing) {
            val task = Runnable {
                cancelNotification(
                    Intent(this, PlaygroundService::class.java)
                        .putExtra("id", id)
                        .putExtra("oi_scene", scene),
                )
            }
            autoDismissTasks[id] = task
            mainHandler.postDelayed(task, timeoutSec * 1000L)
        }
    }

    /**
     * OriginOS refused card [id] (see NotificationCastListener.cleanUpRejectedSuperX). Not every
     * build takes every layout: some drop cards with buttons or the driving-navigation card (#12).
     * Put it up once more as a plain base card without buttons, and keep it that way until it ends.
     * A card that was already plain stays down, as before.
     */
    private fun onRejected(id: Int) {
        val last = lastPosts[id] ?: return
        val app = last.getStringExtra("source_app").orEmpty().ifBlank { "LTMori" }
        val title = last.getStringExtra("title").orEmpty()
        val simpler = last.getIntExtra("oi_template", 0) in FANCY_TEMPLATES ||
            last.hasExtra("actions") || last.hasExtra("oi_button_titles")
        if (id in plainIds || !simpler) {
            CastLog.add(app, title, "refused by the island", false)
            return
        }
        plainIds.add(id)
        CastLog.add(app, title, "refused by the island — shown as a plain card", true)
        readAndPost(last)
    }

    /**
     * Legacy `addAction(int, …)` icons carry an empty package, meaning "the app that posted it".
     * Reposted under our package that points at a resource we don't have, which some OriginOS
     * builds choke on, so pin it to the source app (or drop it if we don't know which one).
     */
    private fun Icon.ownedBy(pkg: String?): Icon? = when {
        type != Icon.TYPE_RESOURCE || !resPackage.isNullOrEmpty() -> this
        pkg.isNullOrEmpty() -> null
        else -> Icon.createWithResource(pkg, resId)
    }

    /**
     * Post the SuperX card exactly the way the original did: a HIGH-importance channel, real
     * title/text (NOT empty), no `setSilent`. On vivo a low-importance/silent/empty notification
     * is minimised and the island never renders it — that was the bug.
     */
    private fun postSuperX(
        id: Int,
        iconRes: Int,
        title: String,
        text: String,
        sourceApp: String,
        isOngoing: Boolean,
        scene: String,
        bundle: android.os.Bundle,
        contentIntent: PendingIntent? = null,
    ) {
        OriginIslandBuilder.grantScenes(this)
        createNotificationChannel(ORIGIN_CHANNEL_ID)
        val cancel = Intent(this, PlaygroundService::class.java)
            .setAction(ACTION_CANCEL)
            .putExtra("id", id)
            .putExtra("oi_scene", scene)
        val deletePi = PendingIntent.getService(
            this, id, cancel,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(isOngoing)
            .setAutoCancel(!isOngoing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDeleteIntent(deletePi)
            .setSmallIcon(iconRes)
            .setExtras(bundle)
        if (id == 0xee84) {
            builder.setSilent(true)
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }
        if (sourceApp.isNotEmpty()) builder.setSubText(sourceApp)
        notificationManager.notify(OriginIslandConstants.SUPERX_TAG, id, builder.build())
    }

    // --- ending ------------------------------------------------------------------

    private fun endOrigin(id: Int, scene: String) {
        try {
            OriginIslandBuilder.grantScenes(this)
            createNotificationChannel(ORIGIN_CHANNEL_ID)
            val n = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("")
                .setContentText("")
                .setSilent(true)
                .setExtras(OriginIslandBuilder.buildEndBundle(scene))
                .build()
            notificationManager.notify(OriginIslandConstants.SUPERX_TAG, id, n)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelNotification(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val scene = originScenes.remove(id) ?: intent.getStringExtra("oi_scene")
        Log.i("PlaygroundService", "cancelNotification: id=$id, scene=$scene")
        if (id == -1) return
        activeIds.remove(id)
        originChangeRecord.remove(id)
        lastPosts.remove(id)
        plainIds.remove(id)
        activeCardIds = activeIds.toSet()

        val remainingId = activeIds.firstOrNull { it != 0xee84 && (originScenes[it] == scene || scene == null) }
        if (remainingId != null) {
            notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, id)
            val remainingIntent = lastPosts[remainingId]
            if (remainingIntent != null) {
                mainHandler.postDelayed({
                    readAndPost(remainingIntent)
                }, 100L)
            }
        } else {
            if (scene != null) {
                endOrigin(id, scene)
                mainHandler.postDelayed({
                    notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, id)
                    notificationManager.cancel(id)
                }, 350L)
            } else {
                notificationManager.cancel(id)
                notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, id)
            }
            mainHandler.postDelayed({
                if (activeIds.isEmpty() || (activeIds.size == 1 && activeIds.contains(0xee84))) {
                    restoreIdleCardIfEnabled()
                }
            }, 500L)
        }
        // Keep the sceneCurrentId entry (now stale) so the NEXT different card clears this scene's
        // leftovers via the staleness-aware replace in readAndPost.
        // NOTE: we deliberately do NOT stopSelf() when idle. The notification listener runs in the
        // background, and Android 14 forbids starting a foreground service from the background — so
        // if this service stopped, the next real cast could not restart it and casting would break.
        // Keeping it alive (on an IMPORTANCE_MIN, icon-less channel) is the cost of reliable casting.
        // Use the "Stop all cards" button (ACTION_STOP) to shut it down fully.
    }

    private fun stopEverything() {
        // An explicit stop must not be undone by an alarm a previous swipe left armed.
        cancelRestartAlarm()
        cancelHeartbeatWatchdog()
        autoDismissTasks.values.forEach { mainHandler.removeCallbacks(it) }
        autoDismissTasks.clear()
        val hadScenes = originScenes.isNotEmpty()
        HashMap(originScenes).forEach { (id, scene) -> endOrigin(id, scene) }
        originScenes.clear()
        originChangeRecord.clear()
        sceneCurrentId.clear()
        lastPosts.clear()
        plainIds.clear()
        val ids = activeIds.toList()
        activeIds.clear()
        activeCardIds = emptySet()
        val finish = {
            ids.forEach {
                notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, it)
                notificationManager.cancel(it)
            }
            // Clear the flag with the teardown, or a start arriving before we're destroyed sees a
            // stale "already foreground" and skips ensureForeground(), leaving the process unanchored.
            isForegroundActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        if (hadScenes) mainHandler.postDelayed({ finish() }, 350L) else finish()
    }

    private fun createNotificationChannel(channelId: String) {
        if (notificationManager.getNotificationChannel(channelId) != null) return
        val isIsland = channelId == ORIGIN_CHANNEL_ID
        val name = if (isIsland) "OriginIsland" else "Background keep-alive"
        // Island channel must be HIGH (4) like the original — LOW gets minimised and never reaches
        // the island. The keep-alive channel is NONE (blocked) so its foreground-service notification
        // is suppressed entirely — the service keeps running with no status-bar icon.
        val importance = if (isIsland) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_NONE
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = "Channel for $name Service"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            if (isIsland) {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun String?.orEmptyOr(fallback: String): String {
        val v = this?.trim().orEmpty()
        return v.ifBlank { fallback }
    }
}
