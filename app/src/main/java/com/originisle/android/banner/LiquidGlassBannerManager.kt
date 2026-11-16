package com.originisle.android.banner

import android.app.PendingIntent
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.originisle.android.ui.PREFS_NAME
import kotlin.math.min

/**
 * Manages the transient presentation of the Apple-style Liquid Glass Heads-up Banner.
 * Purely transient: Attached only during presentation (4.5s) and completely detached
 * from WindowManager immediately after, consuming 0 background RAM or CPU.
 */
object LiquidGlassBannerManager {
    private const val TAG = "LiquidGlassBanner"
    private const val DEFAULT_DURATION_MS = 4500L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeView: LiquidGlassBannerView? = null
    private var windowManager: WindowManager? = null
    private var autoDismissRunnable: Runnable? = null
    private var isDismissing = false

    fun isBannerEnabled(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val style = prefs.getString("notif_banner_style", "liquid_glass")
        return style == "liquid_glass"
    }

    fun show(
        context: Context,
        title: String,
        text: String,
        appLabel: String,
        avatarIcon: Icon? = null,
        appIcon: Icon? = null,
        contentIntent: PendingIntent? = null,
        durationMs: Long = DEFAULT_DURATION_MS,
    ) {
        if (!isBannerEnabled(context)) return

        mainHandler.post {
            try {
                showInternal(context, title, text, appLabel, avatarIcon, appIcon, contentIntent, durationMs)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to display Liquid Glass banner", t)
            }
        }
    }

    private fun showInternal(
        context: Context,
        title: String,
        text: String,
        appLabel: String,
        avatarIcon: Icon?,
        appIcon: Icon?,
        contentIntent: PendingIntent?,
        durationMs: Long,
    ) {
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return
        windowManager = wm

        // Cancel existing dismiss timer
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }

        val currentView = activeView
        if (currentView != null && !isDismissing && currentView.isAttachedToWindow) {
            // Already showing -> update content in-place with a subtle spring bounce
            currentView.bind(title, text, appLabel, avatarIcon, appIcon, contentIntent)
            currentView.animate()
                .scaleX(1.025f).scaleY(1.025f)
                .setDuration(120)
                .withEndAction {
                    currentView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }.start()

            scheduleAutoDismiss(durationMs)
            return
        }

        // If previous view was in the process of dismissing, clean it up
        if (currentView != null) {
            runCatching { wm.removeViewImmediate(currentView) }
            activeView = null
            isDismissing = false
        }

        val bannerView = LiquidGlassBannerView(context)
        bannerView.bind(title, text, appLabel, avatarIcon, appIcon, contentIntent)
        bannerView.onDismissRequested = { dismiss() }

        val dm = context.resources.displayMetrics
        val targetWidth = min((dm.widthPixels * 0.88f).toInt(), (368 * dm.density).toInt())
        val statusBarHeight = getStatusBarHeight(context)
        // Offset downwards so the liquid glass banner never overlaps the native status bar island
        val topMargin = statusBarHeight + Math.round(10 * dm.density)

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        val params = WindowManager.LayoutParams(
            targetWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topMargin
        }

        // Initial entrance state (expanding outward directly from the punch-hole camera)
        bannerView.translationY = -dp(context, 30f)
        bannerView.scaleX = 0.45f
        bannerView.scaleY = 0.25f
        bannerView.alpha = 0f

        wm.addView(bannerView, params)
        activeView = bannerView

        // Spring entrance animation expanding smoothly from the camera cutout
        bannerView.animate()
            .translationY(0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setInterpolator(OvershootInterpolator(0.9f))
            .setDuration(320)
            .start()

        scheduleAutoDismiss(durationMs)
    }

    private fun scheduleAutoDismiss(durationMs: Long) {
        val runnable = Runnable { dismiss() }
        autoDismissRunnable = runnable
        mainHandler.postDelayed(runnable, durationMs)
    }

    fun dismiss() {
        mainHandler.post {
            val view = activeView ?: return@post
            if (isDismissing) return@post
            isDismissing = true

            autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
            autoDismissRunnable = null

            val exitTargetY = -dp(view.context, 35f)

            // Retract & morph upwards towards the camera cutout
            view.animate()
                .translationY(exitTargetY)
                .scaleX(0.45f)
                .scaleY(0.25f)
                .alpha(0f)
                .setInterpolator(AccelerateInterpolator(1.5f))
                .setDuration(220)
                .withEndAction {
                    try {
                        if (view.isAttachedToWindow) {
                            windowManager?.removeViewImmediate(view)
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error removing banner view", t)
                    }
                    if (activeView === view) {
                        activeView = null
                    }
                    isDismissing = false
                }
                .start()
        }
    }

    private fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else Math.round(32 * context.resources.displayMetrics.density)
    }

    private fun dp(context: Context, v: Float): Float = v * context.resources.displayMetrics.density
}
