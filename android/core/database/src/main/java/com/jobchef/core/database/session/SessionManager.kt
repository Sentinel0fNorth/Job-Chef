package com.jobchef.core.database.session

import android.content.Context
import java.util.UUID

/**
 * Manages persistent app session data locally on the device.
 * Enforces a unique, persistent `deviceId` UUID generated on the first app launch,
 * which serves as the user's zero-auth identity token for network calls.
 */
class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Automatically initialize and persist on startup if missing
        getOrCreateDeviceId()
    }

    /**
     * Retrieves the stored device ID, or generates and saves a new UUID if it doesn't exist.
     */
    fun getOrCreateDeviceId(): String {
        var deviceId = sharedPreferences.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    companion object {
        private const val PREFS_NAME = "jobchef_session_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
