package com.yeyint.recipeapp.shared.data.remote

import io.ktor.client.HttpClientConfig

/** No-op — see the expect declaration for why. Ktor's Logging plugin still
 *  prints requests and responses to the Xcode console. */
actual fun HttpClientConfig<*>.installNetworkInspector() = Unit
