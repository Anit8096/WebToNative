package com.kmp.webtonative.di

import com.kmp.webtonative.model.repository.HistoryRepository
import com.kmp.webtonative.model.room.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Room — single database instance
    single { AppDatabase.getInstance(androidContext()) }

    // DAO — pulled from the database, not constructed separately
    single { get<AppDatabase>().historyDao() }

    // Repository gets DAO injected automatically
    singleOf(::HistoryRepository)
}

val viewModel = module {

}