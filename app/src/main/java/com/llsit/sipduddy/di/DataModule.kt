package com.llsit.sipduddy.di

import androidx.room.Room
import com.llsit.sipduddy.data.local.database.HydrationDatabaseMigration
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.local.preferences.hydrationDataStore
import com.llsit.sipduddy.data.mapper.DrinkEntryMapper
import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.data.reminder.AndroidReminderNotificationPublisher
import com.llsit.sipduddy.data.reminder.AndroidReminderScheduler
import com.llsit.sipduddy.data.repository.DataStoreHydrationSettingsRepository
import com.llsit.sipduddy.data.repository.RoomDrinkRepository
import com.llsit.sipduddy.data.time.SystemDateProvider
import com.llsit.sipduddy.domain.reminder.ReminderNotificationPublisher
import com.llsit.sipduddy.domain.reminder.ReminderScheduler
import com.llsit.sipduddy.domain.repository.DrinkRepository
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import com.llsit.sipduddy.domain.time.DateProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock

val dataModule = module {
    single<Clock> { Clock.systemUTC() }
    single { Room.databaseBuilder(androidContext(), HydrationDatabase::class.java, "sipbuddy.db").addMigrations(HydrationDatabaseMigration.FROM_1_TO_2).build() }
    single { get<HydrationDatabase>().drinks() }
    single { androidContext().hydrationDataStore }
    factory { DrinkEntryMapper() }
    factory { HydrationSettingsMapper() }
    single<DrinkRepository> { RoomDrinkRepository(get(), get(), get()) }
    single<HydrationSettingsRepository> { DataStoreHydrationSettingsRepository(get(), get()) }
    single<DateProvider> { SystemDateProvider(get()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext(), get(), get()) }
    single<ReminderNotificationPublisher> { AndroidReminderNotificationPublisher(androidContext()) }
}
