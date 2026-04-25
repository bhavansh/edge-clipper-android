package dev.bmg.edgeclip.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("edgeclip_settings", Context.MODE_PRIVATE)

    var isBackgroundPollingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BG_POLLING, true)
        set(value) = prefs.edit().putBoolean(KEY_BG_POLLING, value).apply()

    var pollingFrequencySeconds: Int
        get() = prefs.getInt(KEY_POLLING_FREQ, 5)
        set(value) = prefs.edit().putInt(KEY_POLLING_FREQ, value).apply()

    var edgeSide: String
        get() = prefs.getString(KEY_EDGE_SIDE, "right") ?: "right"
        set(value) = prefs.edit().putString(KEY_EDGE_SIDE, value).apply()

    var databaseLimit: Int
        get() = prefs.getInt(KEY_DB_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_DB_LIMIT, value).apply()

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    var closeOnOutsideClick: Boolean
        get() = prefs.getBoolean(KEY_CLOSE_OUTSIDE, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOSE_OUTSIDE, value).apply()

    var isPaused: Boolean
        get() = prefs.getBoolean(KEY_IS_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PAUSED, value).apply()

    var blacklistedPackages: Set<String>
        get() = prefs.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_BLACKLIST, value).apply()

    var isSmsReaderEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_READER, false)
        set(value) = prefs.edit().putBoolean(KEY_SMS_READER, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val KEY_BG_POLLING = "background_polling"
        const val KEY_POLLING_FREQ = "polling_frequency"
        const val KEY_EDGE_SIDE = "edge_side"
        const val KEY_DB_LIMIT = "db_limit"
        const val KEY_RETENTION_DAYS = "retention_days"
        const val KEY_CLOSE_OUTSIDE = "close_outside"
        const val KEY_IS_PAUSED = "is_paused"
        const val KEY_BLACKLIST = "blacklisted_packages"
        const val KEY_SMS_READER = "sms_reader_enabled"
    }
}
