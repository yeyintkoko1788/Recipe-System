package com.yeyint.recipeapp.shared.data.repository

import com.russhwolf.settings.Settings
import com.yeyint.recipeapp.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsRepositoryImpl(private val settings: Settings) : SettingsRepository {

    private val _darkMode = MutableStateFlow(readDarkMode())
    override val darkMode: Flow<Boolean?> = _darkMode

    override suspend fun setDarkMode(enabled: Boolean?) {
        if (enabled == null) settings.remove(KEY_DARK_MODE)
        else settings.putBoolean(KEY_DARK_MODE, enabled)
        _darkMode.value = enabled
    }

    private fun readDarkMode(): Boolean? =
        if (settings.hasKey(KEY_DARK_MODE)) settings.getBoolean(KEY_DARK_MODE, false) else null

    private companion object {
        const val KEY_DARK_MODE = "settings.dark_mode"
    }
}
