package com.gamecorner.app
import android.content.Context
import android.graphics.Color
import kotlin.math.roundToInt
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.NotificationCompat
package com.gamecorner.app
import android.content.Context
import android.graphics.Color
import kotlin.math.roundToInt
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.NotificationCompat


class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: TextView
    private var hudPanel: WingHudLayout? = null
    private var crosshairView: CrosshairView? = null
    private var panelClosing = false
    private val buttonParams = WindowManager.LayoutParams(
        56.dp,
        56.dp,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    )

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addFloatingButton()
    }

    private fun addFloatingButton() {
        floatingButton = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.ic_hud_diamond)
            setOnClickListener { togglePanel() }
        }
        buttonParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        buttonParams.x = 4.dp
        buttonParams.y = 0
        floatingButton.setOnTouchListener(DragTouchListener(buttonParams))
        windowManager.addView(floatingButton, buttonParams)
        floatingButton.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .alpha(0.78f)
            .setDuration(820L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (::floatingButton.isInitialized) {
                    floatingButton.animate().scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(820L)
                            .withEndAction { pulseFloatingButton() }
                            .start()
                }
            }
            .start()
    }

    private fun togglePanel() {
        if (panelClosing) return
        hudPanel?.let { panel ->
            panelClosing = true
            panel.animate()
                .translationY(panel.height.coerceAtLeast(36.dp).toFloat())
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(220L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        removePanel(panel)
                        panelClosing = false
                    }
                })
            return
        }
        val panel = layoutInflater.inflate(R.layout.layout_floating_sidebar, null) as WingHudLayout
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
            y = 12.dp
        }
        buildModes(
            panel.findViewById(R.id.modeRow),
            panel.findViewById(R.id.hudModeLabel),
            panel,
        )
        buildUtilities(
            panel.findViewById(R.id.leftUtilityGrid),
            panel.findViewById(R.id.rightUtilityGrid),
        )
        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.translationY = 36.dp.toFloat()
        panel.scaleX = 0.92f
        panel.scaleY = 0.92f
        hudPanel = panel
        windowManager.addView(panel, params)
        panel.startNeonPulse()
        panel.animate()
            .translationY(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun buildModes(row: LinearLayout, label: TextView, panel: WingHudLayout) {
        val modes = listOf(
            "X-MODE" to R.color.gc_red_bright,
            "DYNAMIC" to R.color.gc_orange,
            "DURABLE" to R.color.gc_green,
            "ADVANCED" to R.color.gc_blue,
        )
        modes.forEachIndexed { index, (name, colorRes) ->
            val modeButton = Button(this).apply {
                text = name
                textSize = 9f
                setTextColor(getColor(colorRes))
                setBackgroundResource(R.drawable.bg_mode_selected)
                isAllCaps = false
                setPadding(2, 0, 2, 0)
                setOnClickListener {
                    animateTap(this)
                    label.text = name
                    label.setTextColor(getColor(colorRes))
                    panel.setAccentColor(getColor(colorRes))
                    Toast.makeText(this@OverlayService, "$name profile selected", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(modeButton, LinearLayout.LayoutParams(0, 28.dp, 1f).apply {
                if (index > 0) marginStart = 4.dp
            })
        }
    }

    private fun buildUtilities(leftGrid: GridLayout, rightGrid: GridLayout) {
        val utilities = listOf(
            "Bypass\nCharging", "Brightness\nLock", "No Calls", "Crosshair",
            "Quick Read", "Block\nNotifications", "Graphics\nSmooth", "Master Lock",
            "Refresh Rate", "Touch\nSensitivity", "Screenshot", "FPS Lock",
        )
        utilities.forEachIndexed { index, name ->
            val toggle = Button(this).apply {
                text = name
                textSize = 6.5f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_mode_selected)
                setOnClickListener {
                    animateTap(this)
                    handleUtility(name, this)
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 32.dp
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(1.dp, 1.dp, 1.dp, 1.dp)
            }
            if (index < utilities.size / 2) {
                leftGrid.addView(toggle, params)
            } else {
                rightGrid.addView(toggle, params)
            }
        }
    }

    private fun handleUtility(name: String, button: Button) {
        button.isSelected = !button.isSelected
        when (name) {
            "Brightness\nLock" -> button.keepScreenOn = button.isSelected
            "Crosshair" -> showCrosshair(button.isSelected)
            "Screenshot" -> Toast.makeText(this, "Screenshot requires MediaProjection consent.", Toast.LENGTH_SHORT).show()
            "Bypass\nCharging" -> Toast.makeText(this, "Bypass charging depends on OEM hardware support.", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "$name ${if (button.isSelected) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCrosshair(show: Boolean) {
        if (show && crosshairView == null) {
            crosshairView = CrosshairView(this)
            val params = WindowManager.LayoutParams(
                96.dp,
                96.dp,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.CENTER
            }
            windowManager.addView(crosshairView, params)
        } else if (!show) {
            crosshairView?.let { windowManager.removeView(it) }
            crosshairView = null
        }
        Toast.makeText(
            this,
            if (show) "Crosshair overlay enabled" else "Crosshair overlay disabled",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.hud_service_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rog_mark)
            .setContentTitle("GAME CORNER HUD")
            .setContentText("Floating performance controls are active")
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun animateTap(view: View) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70L).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(150L).start()
        }.start()
    }

    private fun pulseFloatingButton() {
        if (!::floatingButton.isInitialized || floatingButton.parent == null) return
        floatingButton.animate()
            .scaleX(1.1f).scaleY(1.1f).alpha(0.78f)
            .setDuration(820L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                floatingButton.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(820L)
                    .withEndAction { pulseFloatingButton() }
                    .start()
            }.start()
    }

    private fun removePanel(panel: WingHudLayout) {
        panel.stopNeonPulse()
        if (panel.parent != null) windowManager.removeView(panel)
        if (hudPanel === panel) hudPanel = null
    }

    override fun onDestroy() {
        hudPanel?.let { removePanel(it) }
        crosshairView?.let { windowManager.removeView(it) }
        if (::floatingButton.isInitialized) windowManager.removeView(floatingButton)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DragTouchListener(
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.y
                    startX = params.x
                    startY = params.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).roundToInt()
                    params.y = startY + (event.y - downY).roundToInt()
                    windowManager.updateViewLayout(view, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if ((event.rawX - downX).let { kotlin.math.abs(it) } < 12 &&
                        (event.y - downY).let { kotlin.math.abs(it) } < 12
                    ) {
                        view.performClick()
                    }
                    return true
                }
            }
            return false
        }
    }

    private class CrosshairView(context: Context) : View(context) {
        private val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 244, 43, 63)
            strokeWidth = 3f
            style = android.graphics.Paint.Style.STROKE
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            canvas.drawCircle(cx, cy, width * 0.22f, stroke)
            canvas.drawLine(cx - width * 0.48f, cy, cx - width * 0.28f, cy, stroke)
            canvas.drawLine(cx + width * 0.28f, cy, cx + width * 0.48f, cy, stroke)
            canvas.drawLine(cx, cy - height * 0.48f, cx, cy - height * 0.28f, stroke)
            canvas.drawLine(cx, cy + height * 0.28f, cx, cy + height * 0.48f, stroke)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val CHANNEL_ID = "game_corner_hud"
        private const val NOTIFICATION_ID = 15200
    }
}