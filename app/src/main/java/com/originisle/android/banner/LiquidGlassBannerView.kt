package com.originisle.android.banner

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.originisle.android.R
import kotlin.math.abs

/**
 * Authentic Apple iOS-style Dynamic Island Liquid Glass Heads-up Notification Banner.
 * Pure OLED Black (#000000) body seamlessly blending with the camera punch hole,
 * wrapped in an ultra-fine specular glass rim ([#44FFFFFF] -> [#14FFFFFF]) and
 * delicate ambient shadow. Zero background blurring to keep user content 100% visible and crisp.
 */
class LiquidGlassBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dpI(v: Float): Int = Math.round(v * density)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glassSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.0f) // Refractive specular liquid glass rim
    }

    private val boundsRect = RectF()
    private val cornerRadius = dp(28f)

    private var avatarView: ImageView
    private var badgeView: ImageView
    private var titleView: TextView
    private var timeView: TextView
    private var messageView: TextView

    private var contentIntent: PendingIntent? = null
    private var targetPackage: String? = null
    var onDismissRequested: (() -> Unit)? = null

    private var touchDownTime = 0L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = false

        // Elevation and outline for smooth ambient shadow around the island
        clipToOutline = false
        elevation = dp(12f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            outlineAmbientShadowColor = Color.BLACK
            outlineSpotShadowColor = Color.BLACK
        }

        // --- Build View Hierarchy Programmatically for Instant Performance ---
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpI(13f), dpI(10f), dpI(15f), dpI(10f))
        }

        // 1. Avatar container with app badge
        val avatarContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpI(40f), dpI(40f)).apply {
                marginEnd = dpI(11f)
            }
        }

        // Circular Avatar
        avatarView = ImageView(context).apply {
            layoutParams = LayoutParams(dpI(40f), dpI(40f))
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        avatarContainer.addView(avatarView)

        // App Badge overlay in bottom-right corner with black ring separator
        badgeView = ImageView(context).apply {
            layoutParams = LayoutParams(dpI(17f), dpI(17f), Gravity.BOTTOM or Gravity.END)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = createBadgeBackground()
            setPadding(dpI(1.5f), dpI(1.5f), dpI(1.5f), dpI(1.5f))
        }
        avatarContainer.addView(badgeView)
        rootLayout.addView(avatarContainer)

        // 2. Middle Text Column
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        titleView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(Color.WHITE)
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setShadowLayer(dp(3f), 0f, dp(1f), Color.parseColor("#B3000000"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        headerRow.addView(titleView)

        val isVi = com.originisle.android.ui.Lang.isVi(context)
        val nowLabel = if (isVi) "bây giờ" else "now"
        timeView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dpI(6f)
            }
            setTextColor(Color.parseColor("#C7C7CC"))
            textSize = 11.5f
            typeface = Typeface.SANS_SERIF
            setShadowLayer(dp(2f), 0f, dp(1f), Color.parseColor("#99000000"))
            text = nowLabel
        }
        headerRow.addView(timeView)
        textContainer.addView(headerRow)

        messageView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpI(2f)
            }
            setTextColor(Color.parseColor("#F5F5F7"))
            textSize = 13.5f
            typeface = Typeface.SANS_SERIF
            setShadowLayer(dp(2.5f), 0f, dp(1f), Color.parseColor("#99000000"))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        textContainer.addView(messageView)
        rootLayout.addView(textContainer)

        addView(rootLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun createBadgeBackground(): Drawable {
        val shape = android.graphics.drawable.GradientDrawable()
        shape.shape = android.graphics.drawable.GradientDrawable.OVAL
        shape.setColor(Color.parseColor("#4DFFFFFF")) // Translucent white liquid glass ring
        shape.setStroke(dpI(1f), Color.parseColor("#B3FFFFFF"))
        return shape
    }

    fun bind(
        title: String,
        message: String,
        appLabel: String,
        avatarIcon: Icon?,
        appIcon: Icon?,
        pi: PendingIntent?,
        packageName: String? = null,
    ) {
        this.contentIntent = pi
        this.targetPackage = packageName
        titleView.text = title.ifBlank { appLabel }
        messageView.text = message
        val isVi = com.originisle.android.ui.Lang.isVi(context)
        timeView.text = if (isVi) "bây giờ" else "now"

        // Load Avatar
        val avatarDrawable = runCatching { avatarIcon?.loadDrawable(context) }.getOrNull()
        val appDrawable = runCatching { appIcon?.loadDrawable(context) }.getOrNull()

        if (avatarDrawable != null) {
            avatarView.setImageDrawable(avatarDrawable)
            if (appDrawable != null) {
                badgeView.visibility = View.VISIBLE
                badgeView.setImageDrawable(appDrawable)
            } else {
                badgeView.visibility = View.GONE
            }
        } else if (appDrawable != null) {
            avatarView.setImageDrawable(appDrawable)
            badgeView.visibility = View.GONE
        } else {
            avatarView.setImageResource(R.mipmap.ic_launcher_round)
            badgeView.visibility = View.GONE
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Intercept all touches within the banner to guarantee gestures and taps work reliably
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        executeTapAction()
        onDismissRequested?.invoke()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                touchStartX = event.rawX
                touchStartY = event.rawY
                isDragging = false
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.rawY - touchStartY
                if (deltaY < -dp(6f)) {
                    isDragging = true
                    translationY = deltaY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                val deltaX = event.rawX - touchStartX
                val deltaY = event.rawY - touchStartY
                val totalDist = Math.hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
                val elapsed = System.currentTimeMillis() - touchDownTime

                if (isDragging && deltaY < -dp(20f)) {
                    // Swiped up: dismiss immediately
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onDismissRequested?.invoke()
                } else if (elapsed < 800 && totalDist < (touchSlop * 2.5f).coerceAtLeast(dp(28f))) {
                    // User tapped: trigger action
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    executeTapAction()
                    onDismissRequested?.invoke()
                } else {
                    // Snap back down smoothly
                    animate().translationY(0f).setDuration(180).start()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1.0f).scaleY(1.0f).translationY(0f).setDuration(120).start()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun executeTapAction() {
        var launched = false
        val pi = contentIntent

        // 1. Prepare ActivityOptions with BAL exemption for Android 14+ (API 34, 35, 36)
        val optionsBundle: Bundle? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }.toBundle()
            } else {
                ActivityOptions.makeBasic().toBundle()
            }
        } catch (t: Throwable) {
            Log.w("LiquidGlassBanner", "Could not build ActivityOptions", t)
            null
        }

        // 2. Try sending PendingIntent
        if (pi != null) {
            try {
                val fillInIntent = Intent().apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                pi.send(context, 0, fillInIntent, null, null, null, optionsBundle)
                launched = true
                Log.d("LiquidGlassBanner", "Successfully sent PendingIntent: $pi")
            } catch (t: Throwable) {
                Log.w("LiquidGlassBanner", "pi.send failed with options: ${t.message}")
                try {
                    val fillInIntent = Intent().apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    pi.send(context, 0, fillInIntent)
                    launched = true
                    Log.d("LiquidGlassBanner", "Successfully sent PendingIntent without options fallback")
                } catch (t2: Throwable) {
                    Log.w("LiquidGlassBanner", "PendingIntent secondary attempt also failed: ${t2.message}")
                }
            }
        }

        // 3. Fallback: If PendingIntent failed or wasn't provided, launch the target application directly
        val pkg = targetPackage
        if (!launched && !pkg.isNullOrBlank()) {
            try {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                if (launchIntent != null) {
                    if (optionsBundle != null) {
                        context.startActivity(launchIntent, optionsBundle)
                    } else {
                        context.startActivity(launchIntent)
                    }
                    Log.d("LiquidGlassBanner", "Launched target app directly as fallback: $pkg")
                }
            } catch (t: Throwable) {
                Log.e("LiquidGlassBanner", "Failed to launch package $pkg fallback", t)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokePaint.strokeWidth / 2f
        boundsRect.set(inset, inset, w.toFloat() - inset, h.toFloat() - inset)

        val prefs = context.getSharedPreferences(com.originisle.android.ui.PREFS_NAME, Context.MODE_PRIVATE)
        val transparency = prefs.getInt("liquid_glass_transparency", 65).coerceIn(10, 100) / 100f

        // Smoothly blend between deep OLED dark glass (for maximum text contrast) and luminous crystal clear glass
        fun blend(cOpaque: Int, cGlass: Int, t: Float): Int {
            val a = (Color.alpha(cOpaque) * (1f - t) + Color.alpha(cGlass) * t).toInt().coerceIn(0, 255)
            val r = (Color.red(cOpaque) * (1f - t) + Color.red(cGlass) * t).toInt().coerceIn(0, 255)
            val g = (Color.green(cOpaque) * (1f - t) + Color.green(cGlass) * t).toInt().coerceIn(0, 255)
            val b = (Color.blue(cOpaque) * (1f - t) + Color.blue(cGlass) * t).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }

        val topColor = blend(Color.parseColor("#EE1A1A1E"), Color.parseColor("#50FFFFFF"), transparency)
        val midColor = blend(Color.parseColor("#F4121215"), Color.parseColor("#28FFFFFF"), transparency)
        val botColor = blend(Color.parseColor("#FA08080A"), Color.parseColor("#15FFFFFF"), transparency)

        // 1. Pure Liquid Glass Body: Custom translucent shimmering glass based on user preference
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(topColor, midColor, botColor),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP,
        )

        val sheenTop = blend(Color.parseColor("#30FFFFFF"), Color.parseColor("#50FFFFFF"), transparency)
        val sheenMid = blend(Color.parseColor("#0EFFFFFF"), Color.parseColor("#18FFFFFF"), transparency)

        // 2. Liquid Glass Gloss Sheen: Delicate water-like reflection across the upper surface
        glassSheenPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat() * 0.55f,
            intArrayOf(
                sheenTop,
                sheenMid,
                Color.parseColor("#00FFFFFF"),
            ),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP,
        )

        // 3. Apple Liquid Glass Specular Rim: Sparkling, diamond-bright refractive border
        strokePaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#E6FFFFFF"), // 90% brilliant white reflection at top edge
                Color.parseColor("#59FFFFFF"), // 35% refractive glass edge
                Color.parseColor("#26FFFFFF"), // 15% subtle bottom rim
            ),
            floatArrayOf(0.0f, 0.35f, 1.0f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        // 1. Translucent Liquid White Glass Body (Crystal Clear, zero dark/grey tints)
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Liquid Glass Surface Sheen (Glossy reflection)
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, glassSheenPaint)

        // 3. Brilliant Liquid Glass Specular Rim
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, strokePaint)

        super.onDraw(canvas)
    }
}
