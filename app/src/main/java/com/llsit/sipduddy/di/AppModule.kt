package com.llsit.sipduddy.di

import androidx.room.Room
import com.llsit.sipduddy.data.*
import com.llsit.sipduddy.domain.*
import com.llsit.sipduddy.presentation.HydrationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Room.databaseBuilder(androidContext(), HydrationDatabase::class.java, "sipbuddy.db").build() }
    single { get<HydrationDatabase>().drinks() }
    single<HydrationRepository> { LocalHydrationRepository(androidContext(), get()) }
    single<ReminderScheduler> { AndroidReminders(androidContext()) }
    factory { AddDrink(get()) }
    factory { DeleteDrink(get()) }
    factory { SavePreferences(get(), get()) }
    viewModel { HydrationViewModel(get(), get(), get(), get()) }
}
