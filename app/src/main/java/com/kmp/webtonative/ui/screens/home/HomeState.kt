package com.kmp.webtonative.ui.screens.home

sealed class HomeState {
    object Idle : HomeState()
    object Loading : HomeState()
    object SignedOut : HomeState()
    data class Error(val message: String) : HomeState()
}