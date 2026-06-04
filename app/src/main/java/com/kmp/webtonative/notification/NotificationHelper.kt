package com.kmp.webtonative.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kmp.webtonative.MainActivity
import com.kmp.webtonative.R
import androidx.core.content.edit

object NotificationHelper {

    private const val CHANNEL_ID = "welcome_channel"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "notif_prefs"
    private const val KEY_LAST_SHOWN = "last_shown_date"

    // Step 1 — Create channel
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Welcome Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    // Step 2 — Check if we should show today
    private fun shouldShowToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShown = prefs.getString(KEY_LAST_SHOWN, null)
        val today = java.time.LocalDate.now().toString()
        return lastShown != today
    }

    // Step 3 — Save today's date so we don't show again
    private fun markShownToday(context: Context) {
        val today = java.time.LocalDate.now().toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LAST_SHOWN, today)
        }
    }

    // Step 4 — Check if app is in foreground
    private fun isAppInForeground(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        return manager.runningAppProcesses?.any {
            it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && it.processName == context.packageName
        } ?: false
    }

    // Step 5 — Show the notification
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(context: Context) {
        if (!shouldShowToday(context)) return
        if (isAppInForeground(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Welcome Back")
            .setContentText("Thanks for opening the app")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        markShownToday(context)
    }
}