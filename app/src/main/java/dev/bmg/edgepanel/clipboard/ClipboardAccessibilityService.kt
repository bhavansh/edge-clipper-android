package dev.bmg.edgepanel.clipboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dev.bmg.edgepanel.data.ClipRepository
import kotlinx.coroutines.*

class ClipboardAccessibilityService : AccessibilityService() {

    private var clipboardManager: ClipboardManager? = null
    private var repository: ClipRepository? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastStoredText: String? = null
    var isInternalCopy: Boolean = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "=== SERVICE CONNECTED ===")
        serviceInfo = AccessibilityServiceInfo().apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        Handler(Looper.getMainLooper()).postDelayed({ initClipboard() }, 300)
    }

    override fun onDestroy() {
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    // =========================================================================
    // onAccessibilityEvent — read clipboard SYNCHRONOUSLY here.
    // This runs on the main thread mid-event, giving us the brief
    // foreground context MIUI requires. No coroutine dispatch — read now.
    // =========================================================================

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Read synchronously on the accessibility thread
        // MIUI context is only valid for the duration of this callback
        val cm = clipboardManager ?: return
        val text = try {
            cm.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
                ?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Sync read failed", e)
            return
        }

        if (text.isNullOrBlank()) return
        if (text == lastStoredText) return

        if (isInternalCopy) {
            isInternalCopy = false
            lastStoredText = text
            return
        }

        Log.d(TAG, "Event type=${event.eventType} clip='${text.take(40)}'")
        lastStoredText = text

        // Dispatch store to IO thread — don't block the accessibility callback
        scope.launch(Dispatchers.IO) {
            repository?.add(text)
            Log.d(TAG, "Stored: '${text.take(60)}'")
        }
    }

    override fun onInterrupt() {}

    // =========================================================================
    // Called when panel opens — guaranteed fresh read
    // =========================================================================

    fun readClipboardNow() {
        val cm = clipboardManager ?: return
        val text = try {
            cm.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
                ?.trim()
        } catch (e: Exception) { return }

        if (text.isNullOrBlank()) return
        if (text == lastStoredText) return
        if (isInternalCopy) {
            isInternalCopy = false
            lastStoredText = text
            return
        }
        lastStoredText = text
        scope.launch(Dispatchers.IO) {
            repository?.add(text)
            Log.d(TAG, "Stored from panel open: '${text.take(60)}'")
        }
    }

    private fun initClipboard() {
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        repository = ClipRepository.getInstance(this)
        Log.d(TAG, "Init complete")
        readClipboardNow()
    }

    companion object {
        private const val TAG = "ClipboardA11y"
        var instance: ClipboardAccessibilityService? = null
    }

    init { instance = this }
}






