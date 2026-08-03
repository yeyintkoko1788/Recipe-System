package com.yeyint.recipeapp.shared.data.local

import com.russhwolf.settings.Settings

/**
 * Persists the auth session (tokens + minimal user info) using
 * multiplatform-settings (SharedPreferences on Android, NSUserDefaults on
 * iOS). Hardening note: for stronger at-rest protection move tokens to
 * Keystore/Keychain-backed storage — the interface already isolates callers
 * from that change.
 */
class TokenStorage(private val settings: Settings) {

    var accessToken: String?
        get() = settings.getStringOrNull(KEY_ACCESS)
        set(value) = put(KEY_ACCESS, value)

    var refreshToken: String?
        get() = settings.getStringOrNull(KEY_REFRESH)
        set(value) = put(KEY_REFRESH, value)

    var userId: String?
        get() = settings.getStringOrNull(KEY_USER_ID)
        set(value) = put(KEY_USER_ID, value)

    var userName: String?
        get() = settings.getStringOrNull(KEY_USER_NAME)
        set(value) = put(KEY_USER_NAME, value)

    var userEmail: String?
        get() = settings.getStringOrNull(KEY_USER_EMAIL)
        set(value) = put(KEY_USER_EMAIL, value)

    var userRole: String?
        get() = settings.getStringOrNull(KEY_USER_ROLE)
        set(value) = put(KEY_USER_ROLE, value)

    val hasSession: Boolean get() = !refreshToken.isNullOrBlank()

    fun clear() {
        listOf(KEY_ACCESS, KEY_REFRESH, KEY_USER_ID, KEY_USER_NAME, KEY_USER_EMAIL, KEY_USER_ROLE)
            .forEach(settings::remove)
    }

    private fun put(key: String, value: String?) {
        if (value == null) settings.remove(key) else settings.putString(key, value)
    }

    private companion object {
        const val KEY_ACCESS = "auth.access_token"
        const val KEY_REFRESH = "auth.refresh_token"
        const val KEY_USER_ID = "auth.user_id"
        const val KEY_USER_NAME = "auth.user_name"
        const val KEY_USER_EMAIL = "auth.user_email"
        const val KEY_USER_ROLE = "auth.user_role"
    }
}
