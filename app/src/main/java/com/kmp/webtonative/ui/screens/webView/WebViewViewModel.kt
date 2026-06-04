package com.kmp.webtonative.ui.screens.webView

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.webtonative.model.repository.database.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebViewViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private var lastRecordedUrl: String? = null
    private var lastRecordedTime: Long = 0L

    private val _webViewState = MutableStateFlow<WebViewState>(WebViewState.Loading)
    val webViewState: StateFlow<WebViewState> = _webViewState.asStateFlow()

    // Tracks the live URL as user navigates inside WebView
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    // Tracks loading progress 0..100
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    fun onPageStarted(url: String) {
        _currentUrl.value = url
        _webViewState.value = WebViewState.Loading
    }

    fun onProgressChanged(newProgress: Int) {
        _progress.value = newProgress
    }

    fun onPageFinished(url: String, title: String) {
        _currentUrl.value = url
        _webViewState.value = WebViewState.Success

        val now = System.currentTimeMillis()
        if ( (lastRecordedUrl == url) && (now - lastRecordedTime < 3000)) {
            return
        }

        lastRecordedUrl = url
        lastRecordedTime = now

        viewModelScope.launch {
            historyRepository.recordVisit(
                url = url,
                title = title.ifBlank { url }
            )
        }
    }

    fun onPageError(errorMessage: String) {
        _webViewState.value = WebViewState.Error(errorMessage)
    }

    fun onUrlChanged(url: String) {
        _currentUrl.value = url
    }
}
