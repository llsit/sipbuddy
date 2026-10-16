package com.llsit.sipduddy

import android.app.Application
import com.llsit.sipduddy.di.appModule
import com.llsit.sipduddy.domain.HydrationRepository
import com.llsit.sipduddy.domain.ReminderScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SipBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin { androidContext(this@SipBuddyApplication); modules(appModule) }.koin
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { koin.get<ReminderScheduler>().schedule(koin.get<HydrationRepository>().preferences.first()) }
            catch (e: Exception) { android.util.Log.e("SipBuddy", "Unable to restore reminder", e) }
        }
    }
}
