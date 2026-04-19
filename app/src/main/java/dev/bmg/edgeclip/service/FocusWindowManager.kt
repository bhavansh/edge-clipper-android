package dev.bmg.edgeclip.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import dev.bmg.edgeclip.clipboard.ClipboardAccessibilityService
import dev.bmg.edgeclip.data.SettingsManager

class FocusWindowManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var focusWindow: View? = null
    private var focusWindowParams: WindowManager.LayoutParams? = null
    private var clipboardPollHandler: Handler? = null
    private val settingsManager = SettingsManager(context)

    private val idleFlags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private val readFlags =
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    fun createFocusWindow() {
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            idleFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = 0; y = 0
            alpha = 0f
        }
        val view = View(context)
        focusWindow = view
        focusWindowParams = params
        windowManager.addView(view, params)

        startPeriodicClipboardPoll()
    }

    fun restartPolling() {
        startPeriodicClipboardPoll()
    }

    private fun startPeriodicClipboardPoll() {
        stopPeriodicClipboardPoll()
        if (!settingsManager.isBackgroundPollingEnabled) return

        val handler = Handler(Looper.getMainLooper())
        clipboardPollHandler = handler

        val pollRunnable = object : Runnable {
            override fun run() {
                doFocusRead()
                val interval = settingsManager.pollingFrequencySeconds.toLong() * 1000L
                handler.postDelayed(this, interval)
            }
        }
        handler.postDelayed(pollRunnable, 1500)
    }

    fun stopPeriodicClipboardPoll() {
        clipboardPollHandler?.removeCallbacksAndMessages(null)
        clipboardPollHandler = null
    }

    fun triggerFocusRead() {
        doFocusRead()
    }

    private fun doFocusRead() {
        val fw = focusWindow ?: return
        val params = focusWindowParams ?: return

        params.flags = readFlags
        try {
            windowManager.updateViewLayout(fw, params)
        } catch (e: Exception) {
            return
        }

        // Give the OS a moment to process the focus change before reading
        Handler(Looper.getMainLooper()).postDelayed({
            ClipboardAccessibilityService.instance?.readClipboardNow()

            // Keep focus for a moment then release
            Handler(Looper.getMainLooper()).postDelayed({
                params.flags = idleFlags
                try { windowManager.updateViewLayout(fw, params) } catch (_: Exception) {}
            }, 100)
        }, 100)
    }

    fun removeFocusWindow() {
        stopPeriodicClipboardPoll()
        focusWindow?.let { 
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        focusWindow = null
        focusWindowParams = null
    }
}
