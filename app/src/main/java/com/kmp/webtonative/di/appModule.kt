package com.kmp.webtonative.di

import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.kmp.webtonative.model.repository.auth.AuthRepository
import com.kmp.webtonative.model.repository.database.HistoryRepository
import com.kmp.webtonative.model.room.AppDatabase
import com.kmp.webtonative.ui.screens.auth.AuthViewModel
import com.kmp.webtonative.ui.screens.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Room
    single { AppDatabase.getInstance(androidContext()) }
    // DAO
    single { get<AppDatabase>().historyDao() }
    // Repository
    singleOf(::HistoryRepository)


    // Firebase
    single { FirebaseAuth.getInstance() }
    // CredentialManager
    single { CredentialManager.create(androidContext()) }
    // Auth repository
    singleOf(::AuthRepository)
}

val viewModel = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
}