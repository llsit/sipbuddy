package com.llsit.sipduddy.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.llsit.sipduddy.MainActivity
import com.llsit.sipduddy.R
import com.llsit.sipduddy.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.ZonedDateTime

class AndroidReminders(private val context: Context) : ReminderScheduler {
    override fun schedule(preferences: Preferences) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(context, 10, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(intent)
        if (preferences.reminderEnabled) {
            val next = nextReminder(ZonedDateTime.now(), preferences.hour, preferences.minute)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), intent)
        }
    }
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val koin = GlobalContext.get()
                val prefs = koin.get<HydrationRepository>().preferences.first()
                koin.get<ReminderScheduler>().schedule(prefs)
                if (intent.action != null || !prefs.reminderEnabled) return@launch
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel("water", "เตือนดื่มน้ำ", NotificationManager.IMPORTANCE_DEFAULT))
                val allowed = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (allowed && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    val open = PendingIntent.getActivity(context, 11, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    manager.notify(10, NotificationCompat.Builder(context, "water").setSmallIcon(R.drawable.ic_water)
                        .setContentTitle("ถึงเวลาจิบน้ำแล้ว ♡").setContentText("พักสักนิด เติมความสดชื่นกับ SipBuddy")
                        .setContentIntent(open).setAutoCancel(true).build())
                }
            } catch (e: Exception) { android.util.Log.e("SipBuddy", "Unable to deliver reminder", e) }
            finally { pending.finish() }
        }
    }
}
