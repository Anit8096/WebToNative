package com.kmp.webtonative.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.webtonative.BuildConfig
import com.kmp.webtonative.model.repository.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Check on app launch whether user is already signed in
    // so we can skip the Sign In screen
    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = AuthState.Loading

            val result = authRepository.signInWithGoogle(
                context = context,
                webClientId = BuildConfig.WEB_CLIENT_ID
            )

            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrThrow())
            } else {
                AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Sign-in failed"
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Idle
    }

    // Reset error state after showing snackbar
    // so it doesn't re-trigger on recomposition
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}