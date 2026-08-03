package com.yeyint.recipeapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface SettingsContract {
    /** null = follow system theme. */
    val darkMode: StateFlow<Boolean?>
    fun setDarkMode(enabled: Boolean?)
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel(), SettingsContract {

    override val darkMode: StateFlow<Boolean?> = settingsRepository.darkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    }
}
