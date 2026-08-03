package com.yeyint.recipeapp

import android.app.Application
import com.yeyint.recipeapp.di.appModule
import com.yeyint.recipeapp.shared.data.remote.ApiConfig
import com.yeyint.recipeapp.shared.di.AppContextHolder
import com.yeyint.recipeapp.shared.di.androidPlatformModule
import com.yeyint.recipeapp.shared.di.initSharedKoin

/** Boots Koin with the flavor-specific API base URL (see build.gradle.kts). */
class RecipeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext
        initSharedKoin(
            config = ApiConfig(
                baseUrl = BuildConfig.API_BASE_URL,
                enableNetworkLogs = BuildConfig.DEBUG,
            ),
            platformModule = androidPlatformModule,
            extraModules = listOf(appModule),
        )
    }
}
