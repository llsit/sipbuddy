package com.llsit.sipduddy.di
import com.llsit.sipduddy.data.auth.ActiveAccountRepository
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.mapper.RemoteDrinkMapper
import com.llsit.sipduddy.data.mapper.SyncRecordMapper
import com.llsit.sipduddy.data.repository.*
import com.llsit.sipduddy.data.sync.*
import com.llsit.sipduddy.domain.repository.*
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.presentation.SyncViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
val syncModule = module {
    single { CoroutineScope(SupervisorJob()+Dispatchers.IO) }
    single<AccountRepository> { ActiveAccountRepository(get(),get()) }
    single<SyncRequestScheduler> { HydrationSyncScheduler(androidContext()) }
    single { SyncRuntimeState() }
    factory { RemoteDrinkMapper() }
    factory { SyncRecordMapper() }
    single<LocalHydrationSyncRepository> { RoomHydrationSyncRepository(get(),get()) }
    single<RemoteHydrationSyncRepository> { SupabaseHydrationSyncRepository(get(),get()) }
    single<HydrationSyncRepository> { DefaultHydrationSyncRepository(get(),get(),get(),get()) }
    factory { SyncHydrationUseCase(get(),get(),get()) }
    factory { ObserveSyncStatusUseCase(get()) }
    factory { RequestHydrationSyncUseCase(get()) }
    factory { ImportGuestHistoryUseCase(get()) }
    single { HydrationSyncCoordinator(get(),get(),get(),get(),get()) }
    viewModel { parameters -> SyncViewModel(get(),get(),get(),parameters.getOrNull<String>()) }
}
