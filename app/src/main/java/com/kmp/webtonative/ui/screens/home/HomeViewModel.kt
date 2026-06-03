package com.kmp.webtonative.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.webtonative.model.repository.auth.AuthRepository
import com.kmp.webtonative.model.repository.database.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Idle)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    // URL input field value — survives recomposition
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    fun onUrlChange(value: String) {
        _urlInput.value = value
    }

    // Called when user taps Paste button
    fun onPaste(text: String) {
        _urlInput.value = text
    }

    // Sanitize and validate URL before navigating
    fun buildUrl(): String? {
        val raw = _urlInput.value.trim()
        if (raw.isEmpty()) return null
        return if (raw.startsWith("http://") || raw.startsWith("https://")) {
            raw
        } else {
            "https://$raw"
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _homeState.value = HomeState.SignedOut
        }
    }

    fun resetState() {
        _homeState.value = HomeState.Idle
    }
}