package dev.bmg.edgeclip.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",       // HTC/older Samsung
            "com.htc.intent.action.QUICKBOOT_POWERON",       // HTC devices
            Intent.ACTION_MY_PACKAGE_REPLACED -> {           // survives app updates
                Log.d(TAG, "Boot/update received: ${intent.action}")
                startEdgeClip(context)
            }
        }
    }

    private fun startEdgeClip(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "No overlay permission — skipping autostart")
            return
        }
        try {
            val intent = Intent(context, dev.bmg.edgeclip.service.EdgeClipService::class.java)
            context.startService(intent)
            Log.d(TAG, "EdgeClipService started from boot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start EdgeClipService", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}