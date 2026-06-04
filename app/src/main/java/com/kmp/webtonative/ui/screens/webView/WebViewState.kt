package com.kmp.webtonative.ui.screens.webView

sealed class WebViewState {
    object Loading : WebViewState()
    object Success : WebViewState()
    data class Error(val message: String) : WebViewState()
}