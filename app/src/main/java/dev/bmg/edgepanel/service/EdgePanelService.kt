package dev.bmg.edgepanel.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class EdgePanelService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var edgeView: View
    private var panelView: View? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("EdgePanel", "Service Created")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createEdgeHandle()
    }

    private fun createEdgeHandle() {
        val layoutParams = WindowManager.LayoutParams(
            40,   // slightly narrower than before
            280,  // taller than before
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL

        edgeView = LinearLayout(this).apply {
            setBackgroundColor(0x88FF0000.toInt())
        }

        windowManager.addView(edgeView, layoutParams)
        enableTouch(layoutParams)
    }

    private fun enableTouch(params: WindowManager.LayoutParams) {
        edgeView.setOnTouchListener(object : View.OnTouchListener {

            var initialY = 0
            var initialTouchY = 0f
            var isClick = true

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = params.y
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        if (abs(deltaY) > 10) {
                            isClick = false
                            params.y = initialY + deltaY
                            windowManager.updateViewLayout(edgeView, params)
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isClick) togglePanel()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun togglePanel() {
        if (panelView == null) openPanel() else closePanel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openPanel() {
        if (panelView != null) return

        val screenHeight = getScreenHeight()
        val panelWidth = 600
        val panelHeight = (screenHeight * 0.8).toInt()

        val params = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Centre the panel vertically — same feel as Samsung edge panel
        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 48)
            setBackgroundColor(0xEE1A1A2E.toInt()) // dark navy, feels premium
            // Start off-screen to the right so the slide-in animation works
            translationX = panelWidth.toFloat()
        }

        // --- Header ---
        val title = TextView(this).apply {
            text = "Edge Panel"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        panel.addView(title)

        val divider = View(this).apply {
            setBackgroundColor(0x33FFFFFF)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.setMargins(0, 24, 0, 24) }
        }
        panel.addView(divider)

        // --- Swipe-right to dismiss ---
        panel.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0f
            var isDismissing = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.rawX
                        isDismissing = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialX
                        if (!isDismissing && deltaX > 80) {
                            isDismissing = true
                            closePanel()
                        }
                        return true
                    }
                }
                return false
            }
        })

        panelView = panel
        windowManager.addView(panel, params)

        // Slide in from the right
        panel.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        panel.animate()
            .translationX(0f)
            .setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                panel.setLayerType(View.LAYER_TYPE_NONE, null)
            }
            .start()

        edgeView.visibility = View.GONE
    }

    private fun closePanel() {
        panelView?.let { panel ->
            val width = 600f

            panel.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            panel.animate()
                .translationX(width)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    panel.setLayerType(View.LAYER_TYPE_NONE, null)
                    panel.post {
                        windowManager.removeView(panel)
                        panelView = null
                        edgeView.visibility = View.VISIBLE
                    }
                }
                .start()
        }
    }

    private fun getScreenHeight(): Int = resources.displayMetrics.heightPixels

    override fun onDestroy() {
        super.onDestroy()
        panelView?.let { windowManager.removeView(it) }
        if (::edgeView.isInitialized) windowManager.removeView(edgeView)
        Log.d("EdgePanel", "Service Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}