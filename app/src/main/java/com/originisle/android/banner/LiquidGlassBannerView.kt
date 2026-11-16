package com.originisle.android.banner

import android.app.PendingIntent
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
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
        color = Color.BLACK // Pure OLED Black matching the camera punch-hole perfectly
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(0.75f) // Ultra-fine specular glass rim
    }

    private val boundsRect = RectF()
    private val cornerRadius = dp(28f)

    private var avatarView: ImageView
    private var badgeView: ImageView
    private var titleView: TextView
    private var timeView: TextView
    private var messageView: TextView

    private var contentIntent: PendingIntent? = null
    var onDismissRequested: (() -> Unit)? = null

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
            setTextColor(Color.parseColor("#8E8E93"))
            textSize = 11.5f
            typeface = Typeface.SANS_SERIF
            text = nowLabel
        }
        headerRow.addView(timeView)
        textContainer.addView(headerRow)

        messageView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpI(2f)
            }
            setTextColor(Color.parseColor("#EBEBF5"))
            textSize = 13.5f
            typeface = Typeface.SANS_SERIF
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        textContainer.addView(messageView)
        rootLayout.addView(textContainer)

        addView(rootLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        setupTouchInteraction()
    }

    private fun createBadgeBackground(): Drawable {
        val shape = android.graphics.drawable.GradientDrawable()
        shape.shape = android.graphics.drawable.GradientDrawable.OVAL
        shape.setColor(Color.BLACK) // Pure black separator ring around the badge
        shape.setStroke(dpI(1f), Color.parseColor("#222226"))
        return shape
    }

    fun bind(
        title: String,
        message: String,
        appLabel: String,
        avatarIcon: Icon?,
        appIcon: Icon?,
        pi: PendingIntent?,
    ) {
        this.contentIntent = pi
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

    private fun setupTouchInteraction() {
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.rawY
                    isDragging = false
                    view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - touchStartY
                    if (deltaY < -dp(6f)) {
                        isDragging = true
                        view.translationY = deltaY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    val deltaY = event.rawY - touchStartY
                    if (isDragging && deltaY < -dp(20f)) {
                        // User swiped up: dismiss immediately!
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDismissRequested?.invoke()
                    } else if (abs(deltaY) < dp(10f)) {
                        // User tapped: trigger action
                        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        try {
                            contentIntent?.send()
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                        onDismissRequested?.invoke()
                    } else {
                        // Snap back down smoothly
                        view.animate().translationY(0f).setDuration(180).start()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).translationY(0f).setDuration(120).start()
                    true
                }
                else -> false
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokePaint.strokeWidth / 2f
        boundsRect.set(inset, inset, w.toFloat() - inset, h.toFloat() - inset)

        // Apple Liquid Glass Specular Gradient: Subtle highlight at top edge fading downward
        val gradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#44FFFFFF"), // Subtle glass highlight on top edge
                Color.parseColor("#22FFFFFF"),
                Color.parseColor("#14FFFFFF"), // Soft dark rim on bottom
            ),
            floatArrayOf(0.0f, 0.4f, 1.0f),
            Shader.TileMode.CLAMP,
        )
        strokePaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        // 1. Draw Pure OLED Black Island Body
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Draw Ultra-Fine Liquid Glass Specular Rim
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, strokePaint)

        super.onDraw(canvas)
    }
}
