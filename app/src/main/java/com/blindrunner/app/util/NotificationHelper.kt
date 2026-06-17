package com.blindrunner.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.blindrunner.app.R
import com.blindrunner.app.ui.blind.BlindHomeActivity
import com.blindrunner.app.ui.volunteer.VolunteerHomeActivity

object NotificationHelper {
    private const val CHANNEL_DEMAND = "demand_channel"
    private const val CHANNEL_RUN = "running_channel"

    fun init(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_DEMAND, "需求通知", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "陪跑需求状态变更通知" })
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_RUN, "跑步服务", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "跑步计时前台通知" })
    }

    fun notifyDemandAccepted(context: Context, location: String, contactPhone: String) {
        val intent = Intent(context, BlindHomeActivity::class.java).apply {
            putExtra("show_demand_accepted", true)
            putExtra("location", location)
            putExtra("contact", contactPhone)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, 100,
            intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEMAND)
            .setContentTitle("🎉 需求已被接单！")
            .setContentText("跑步地点：$location，志愿者联系方式：$contactPhone")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(200, notification)
    }

    fun notifyDemandStatusChange(context: Context, msg: String) {
        val intent = Intent(context, BlindHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, 101,
            intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEMAND)
            .setContentTitle("需求状态更新")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(201, notification)
    }
}
