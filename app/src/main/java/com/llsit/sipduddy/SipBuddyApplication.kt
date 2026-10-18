package com.llsit.sipduddy

import android.app.Application
import androidx.work.Configuration
import com.llsit.sipduddy.data.sync.HydrationSyncCoordinator
import com.llsit.sipduddy.data.sync.HydrationSyncWorkerFactory
import android.util.Log
import com.llsit.sipduddy.di.appModules
import com.llsit.sipduddy.domain.usecase.RestoreReminderUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SipBuddyApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(HydrationSyncWorkerFactory()).build()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val koin = startKoin { androidContext(this@SipBuddyApplication); modules(appModules) }.koin
        koin.get<HydrationSyncCoordinator>().start()
        applicationScope.launch {
            try {
                koin.get<RestoreReminderUseCase>()()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("SipBuddy", "Unable to restore reminder", exception)
            }
        }
    }
}
