package com.llsit.sipduddy.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.llsit.sipduddy.MainActivity
import com.llsit.sipduddy.R
import com.llsit.sipduddy.core.notification.ReminderNotificationChannel
import com.llsit.sipduddy.domain.reminder.ReminderNotificationPublisher

class AndroidReminderNotificationPublisher(private val context: Context) :
    ReminderNotificationPublisher {
    override fun show() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderNotificationChannel.ID,
                "เตือนดื่มน้ำ",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (manager.getNotificationChannel(ReminderNotificationChannel.ID)?.importance == NotificationManager.IMPORTANCE_NONE) return
        val open = PendingIntent.getActivity(
            context, 11, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            ReminderNotificationChannel.NOTIFICATION_ID,
            NotificationCompat.Builder(context, ReminderNotificationChannel.ID)
                .setSmallIcon(R.drawable.ic_water).setContentTitle("ถึงเวลาจิบน้ำแล้ว ♡")
                .setContentText("พักสักนิด เติมความสดชื่นกับ SipBuddy").setContentIntent(open)
                .setAutoCancel(true).build()
        )
    }
}
