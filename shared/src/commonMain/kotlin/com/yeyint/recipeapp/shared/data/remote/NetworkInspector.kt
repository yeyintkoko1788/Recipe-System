package com.yeyint.recipeapp.shared.data.remote

import io.ktor.client.HttpClientConfig

/**
 * Installs the platform's in-app network inspector, when one is available.
 *
 * Only called for debug/staging builds (see [createHttpClient]). Android uses
 * Inspektify (shake to open — the KMP successor to Chucker); iOS is currently
 * a no-op because Inspektify's iOS klib fails to resolve under Kotlin 2.2.x,
 * so iOS relies on Ktor's Logging plugin printing to the Xcode console.
 * To re-enable iOS, move the `inspektify-ktor3` dependency back to commonMain
 * and install the plugin in the iOS actual below.
 */
expect fun HttpClientConfig<*>.installNetworkInspector()
