package com.tlib.inappupdate.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.tlib.inappupdate.R

object UpdateNotificationHelper {

    private const val CHANNEL_ID = "update_ready_channel"
    private const val CHANNEL_NAME = "App Updates"
    private const val NOTIFICATION_ID = 2001

    const val ACTION_INSTALL_UPDATE =
        "com.tlib.inappupdate.update.action.INSTALL_UPDATE"

    fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description = "Shows update installation notifications"

                enableVibration(true)

                lockscreenVisibility =
                    Notification.VISIBILITY_PUBLIC
            }

            val manager =
                context.getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    fun showReadyToInstallNotification(
        context: Context,
        pendingIntent: PendingIntent
    ) {

        createChannel(context)

        val collapsedView = RemoteViews(
            context.packageName,
            R.layout.iau_notification_update
        )

        collapsedView.setTextViewText(
            R.id.txtTitle,
            "New Version is Ready 🚀"
        )

        collapsedView.setOnClickPendingIntent(
            R.id.btnInstall,
            pendingIntent
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(getAppIconOrFallback(context))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(collapsedView)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun getAppIconOrFallback(context: Context): Int {
        return try {
            context.applicationInfo.icon
        } catch (_: Exception) {
            R.drawable.iau_iconholder
        }
    }

    fun cancelNotification(context: Context) {

        val manager =
            context.getSystemService(NotificationManager::class.java)

        manager.cancel(NOTIFICATION_ID)
    }

}