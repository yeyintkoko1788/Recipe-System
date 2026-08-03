package com.yeyint.recipeapp

import androidx.compose.ui.window.ComposeUIViewController
import com.yeyint.recipeapp.di.appModule
import com.yeyint.recipeapp.shared.data.remote.ApiConfig
import com.yeyint.recipeapp.shared.di.initSharedKoin
import com.yeyint.recipeapp.shared.di.iosPlatformModule
import platform.UIKit.UIViewController

private var koinStarted = false

/**
 * iOS entry point, called from SwiftUI (see iosApp/iosApp/ContentView.swift).
 * For a simulator build the backend on the host machine is reachable via
 * localhost.
 */
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        koinStarted = true
        initSharedKoin(
            config = ApiConfig(baseUrl = "http://localhost:8080", enableNetworkLogs = true),
            platformModule = iosPlatformModule,
            extraModules = listOf(appModule),
        )
    }
    return ComposeUIViewController { App() }
}
