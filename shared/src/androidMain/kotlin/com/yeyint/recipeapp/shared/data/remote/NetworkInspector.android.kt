package com.yeyint.recipeapp.shared.data.remote

import io.ktor.client.HttpClientConfig
import sp.bvantur.inspektify.ktor.InspektifyKtor

/** Shake the device to open the Inspektify network inspector. */
actual fun HttpClientConfig<*>.installNetworkInspector() {
    install(InspektifyKtor)
}
