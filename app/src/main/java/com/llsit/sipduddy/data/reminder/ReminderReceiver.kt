package com.llsit.sipduddy.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.llsit.sipduddy.domain.usecase.HandleReminderUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val handleReminderUseCase: HandleReminderUseCase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(9_000) { handleReminderUseCase(shouldNotify = intent.action == null) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("SipBuddy", "Unable to handle reminder", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
