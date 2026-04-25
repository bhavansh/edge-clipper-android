package dev.bmg.edgeclip.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dev.bmg.edgeclip.data.ClipRepository
import dev.bmg.edgeclip.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        val settings = SettingsManager(context)
        if (!settings.isSmsReaderEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val repository = ClipRepository.getInstance(context)

        for (message in messages) {
            val body = message.messageBody ?: continue
            Log.d("SmsReceiver", "Received SMS: $body")
            
            val detectedType = repository.detectSubtype(body)
            if (detectedType == "OTP") {
                Log.d("SmsReceiver", "OTP detected in SMS context")
                scope.launch {
                    repository.add(body)
                }
            }
        }
    }
}
