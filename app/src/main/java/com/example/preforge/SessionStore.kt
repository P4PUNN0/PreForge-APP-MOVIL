package com.example.preforge

import android.content.Context
import java.util.UUID

class SessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getOrCreateGuest(requestedName: String): AppUser {
        val guestId = preferences.getString(KEY_GUEST_ID, null)
            ?: "guest-${UUID.randomUUID()}".also { id ->
                preferences.edit().putString(KEY_GUEST_ID, id).apply()
            }
        val displayName = requestedName.trim().ifBlank {
            preferences.getString(KEY_GUEST_NAME, null) ?: "Invitado"
        }
        preferences.edit()
            .putString(KEY_GUEST_NAME, displayName)
            .apply()
        return AppUser(
            id = guestId,
            displayName = displayName,
            isGuest = true
        )
    }

    fun loadGuest(): AppUser? {
        val guestId = preferences.getString(KEY_GUEST_ID, null) ?: return null
        val displayName = preferences.getString(KEY_GUEST_NAME, null) ?: "Invitado"
        return AppUser(
            id = guestId,
            displayName = displayName,
            isGuest = true
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "preforge_session"
        const val KEY_GUEST_ID = "guest_id"
        const val KEY_GUEST_NAME = "guest_name"
    }
}
