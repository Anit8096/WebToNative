package com.kmp.webtonative.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {
    @Serializable data object Auth: Route
    @Serializable data object Home: Route
    @Serializable data object History: Route
    @Serializable data class VebView(
        val url: String
    ): Route
}