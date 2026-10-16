package com.llsit.sipduddy.di

import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase
import com.llsit.sipduddy.domain.usecase.CalculateNextReminderUseCase
import com.llsit.sipduddy.domain.usecase.CalculateWeeklyHydrationUseCase
import com.llsit.sipduddy.domain.usecase.DeleteDrinkUseCase
import com.llsit.sipduddy.domain.usecase.HandleReminderUseCase
import com.llsit.sipduddy.domain.usecase.ObserveHydrationOverviewUseCase
import com.llsit.sipduddy.domain.usecase.RestoreReminderUseCase
import com.llsit.sipduddy.domain.usecase.SaveHydrationSettingsUseCase
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val domainModule = module {
    factory { AddDrinkUseCase(get(), get()) }
    factory { DeleteDrinkUseCase(get()) }
    factory { SaveHydrationSettingsUseCase(get(), get()) }
    factory { CalculateWeeklyHydrationUseCase() }
    factory { CalculateNextReminderUseCase() }
    factory { ObserveHydrationOverviewUseCase(get(), get(), get(), get(), Dispatchers.Default) }
    factory { RestoreReminderUseCase(get(), get()) }
    factory { HandleReminderUseCase(get(), get()) }
}
