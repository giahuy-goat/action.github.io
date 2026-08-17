package com.gamecorner.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.roundToInt

/**
 * Transparent container that paints the detached, symmetrical HUD wing shape
 * seen in the reference recording. Child controls remain normal Android views
 * so the menu stays accessible and easy to extend.
 */
class WingHudLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : android.widget.FrameLayout(context, attrs) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(236, 10, 11, 16)
        style = Paint.Style.FILL
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 43, 63)
        strokeWidth = 2.dp
        style = Paint.Style.STROKE
    }
    private val innerEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255)
        strokeWidth = 1.dp
        style = Paint.Style.STROKE
    }
    private var accent = Color.rgb(244, 43, 63)
    private var neonPulse: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setAccentColor(color: Int) {
        accent = color
        edge.color = color
        invalidate()
    }

    fun startNeonPulse() {
        neonPulse?.cancel()
        neonPulse = ValueAnimator.ofInt(145, 235).apply {
            duration = 1100L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                edge.alpha = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    fun stopNeonPulse() {
        neonPulse?.cancel()
        neonPulse = null
        edge.alpha = 255
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val centerLeft = w * 0.36f
        val centerRight = w * 0.64f
        val top = h * 0.16f
        val bottom = h * 0.9f

        val left = Path().apply {
            moveTo(0f, h * 0.62f)
            lineTo(0f, h * 0.3f)
            lineTo(w * 0.16f, 0f)
            lineTo(w * 0.34f, top)
            lineTo(centerLeft, top)
            lineTo(centerLeft, bottom)
            lineTo(w * 0.2f, h)
            lineTo(w * 0.05f, h * 0.9f)
            close()
        }
        val right = Path().apply {
            moveTo(w, h * 0.62f)
            lineTo(w, h * 0.3f)
            lineTo(w * 0.84f, 0f)
            lineTo(w * 0.66f, top)
            lineTo(centerRight, top)
            lineTo(centerRight, bottom)
            lineTo(w * 0.8f, h)
            lineTo(w * 0.95f, h * 0.9f)
            close()
        }

        canvas.drawPath(left, fill)
        canvas.drawPath(right, fill)
        canvas.drawPath(left, edge)
        canvas.drawPath(right, edge)

        val center = RectF(centerLeft, top, centerRight, bottom)
        canvas.drawRoundRect(center, 10.dp.toFloat(), 10.dp.toFloat(), fill)
        canvas.drawRoundRect(center, 10.dp.toFloat(), 10.dp.toFloat(), edge)
        canvas.drawLine(w * 0.05f, h * 0.88f, w * 0.26f, h * 0.88f, innerEdge)
        canvas.drawLine(w * 0.74f, h * 0.88f, w * 0.95f, h * 0.88f, innerEdge)

        // Small accent rails make the selected mode readable even over a game.
        val rail = Paint(edge).apply { alpha = 190 }
        canvas.drawLine(w * 0.06f, h * 0.74f, w * 0.18f, h * 0.74f, rail)
        canvas.drawLine(w * 0.82f, h * 0.74f, w * 0.94f, h * 0.74f, rail)
    }

    private val Int.dp: Float
        get() = this * resources.displayMetrics.density
}