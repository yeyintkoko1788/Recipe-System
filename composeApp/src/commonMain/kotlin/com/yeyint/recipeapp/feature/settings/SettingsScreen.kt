package com.yeyint.recipeapp.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/** App settings: light/dark/system theme (persisted on device). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsContract = koinViewModel<SettingsViewModel>(),
) {
    val darkMode by viewModel.darkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            ThemeOption("Follow system", selected = darkMode == null) { viewModel.setDarkMode(null) }
            ThemeOption("Light", selected = darkMode == false) { viewModel.setDarkMode(false) }
            ThemeOption("Dark", selected = darkMode == true) { viewModel.setDarkMode(true) }
        }
    }
}

private class FakeSettingsContract(darkModeValue: Boolean?) : SettingsContract {
    override val darkMode: StateFlow<Boolean?> = MutableStateFlow(darkModeValue)
    override fun setDarkMode(enabled: Boolean?) = Unit
}

@Preview
@Composable
private fun SettingsScreenSystemPreview() {
    MaterialTheme {
        Surface {
            SettingsScreen(onBack = {}, viewModel = FakeSettingsContract(null))
        }
    }
}

@Preview
@Composable
private fun SettingsScreenLightPreview() {
    MaterialTheme {
        Surface {
            SettingsScreen(onBack = {}, viewModel = FakeSettingsContract(false))
        }
    }
}

@Preview
@Composable
private fun SettingsScreenDarkPreview() {
    MaterialTheme {
        Surface {
            SettingsScreen(onBack = {}, viewModel = FakeSettingsContract(true))
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
