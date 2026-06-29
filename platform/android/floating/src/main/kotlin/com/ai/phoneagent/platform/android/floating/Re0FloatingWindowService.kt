package com.ai.phoneagent.platform.android.floating

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ai.phoneagent.core.capability.FloatingWindowState

class Re0FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> show(intent)
            ACTION_HIDE -> {
                hide()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hide()
        super.onDestroy()
    }

    private fun show(intent: Intent) {
        hide()
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val mode = intent.getStringExtra(EXTRA_MODE) ?: "compact"
        val width = dp(intent.getIntExtra(EXTRA_WIDTH_DP, DEFAULT_WIDTH_DP))
        val height = dp(intent.getIntExtra(EXTRA_HEIGHT_DP, DEFAULT_HEIGHT_DP))
        val view = createContentView(mode)
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(96)
        }
        attachDrag(view, params)
        windowManager.addView(view, params)
        floatingView = view
        layoutParams = params
        currentState = FloatingWindowState(
            sessionId = sessionId,
            visible = true,
            mode = mode,
        )
    }

    private fun hide() {
        floatingView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        floatingView = null
        layoutParams = null
        currentState = FloatingWindowState()
    }

    private fun createContentView(mode: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dimensionPixelSize(R.dimen.re0_floating_padding_horizontal),
                dimensionPixelSize(R.dimen.re0_floating_padding_vertical),
                dimensionPixelSize(R.dimen.re0_floating_padding_horizontal),
                dimensionPixelSize(R.dimen.re0_floating_padding_vertical),
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.re0_floating_background))
            addView(
                TextView(context).apply {
                    setText(R.string.re0_floating_title)
                    setTextColor(ContextCompat.getColor(context, R.color.re0_floating_title))
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dimension(R.dimen.re0_floating_title_text_size))
                },
            )
            addView(
                TextView(context).apply {
                    text = mode
                    setTextColor(ContextCompat.getColor(context, R.color.re0_floating_body))
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dimension(R.dimen.re0_floating_body_text_size))
                },
            )
        }
    }

    private fun attachDrag(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchStartX).toInt()
                    params.y = startY + (event.rawY - touchStartY).toInt()
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun dimension(resourceId: Int): Float = resources.getDimension(resourceId)

    private fun dimensionPixelSize(resourceId: Int): Int = resources.getDimensionPixelSize(resourceId)

    companion object {
        const val ACTION_SHOW = "com.ai.phoneagent.re0.floating.SHOW"
        const val ACTION_HIDE = "com.ai.phoneagent.re0.floating.HIDE"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_MODE = "mode"
        const val EXTRA_WIDTH_DP = "width_dp"
        const val EXTRA_HEIGHT_DP = "height_dp"
        private const val DEFAULT_WIDTH_DP = 260
        private const val DEFAULT_HEIGHT_DP = 120

        @Volatile
        private var currentState: FloatingWindowState = FloatingWindowState()

        fun currentState(): FloatingWindowState = currentState
    }
}
