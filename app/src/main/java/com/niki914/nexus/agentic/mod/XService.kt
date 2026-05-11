package com.niki914.nexus.agentic.mod

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.niki914.nexus.agentic.R
import com.niki914.nexus.h.util.xlog

object XService {
    suspend fun refreshWebSettings(context: Context, packageName: String, versionCode: Long) {
        refreshWebSettingsInternal(context, packageName, versionCode)
    }

    fun getWebSettings(context: Context): XSettings.WebSettings {
        return readWebSettings(context)
    }

    fun getLocalSettings(context: Context): XSettings.LocalSettings {
        return readLocalSettings(context)
    }

    fun putLocalSettings(context: Context, settings: XSettings.LocalSettings) {
        writeLocalSettings(context, settings)
    }

    fun postNotification(
        context: Context,
        title: String,
        content: String,
        shouldStartActivity: Boolean
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            xlog("XService.postNotification skipped: POST_NOTIFICATIONS not granted")
            return
        }
        postNotificationInternal(context, title, content, shouldStartActivity)
    }

    @SuppressLint("MissingPermission")
    private fun postNotificationInternal(
        context: Context,
        title: String,
        content: String,
        shouldStartActivity: Boolean
    ) {
        ensureNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        if (shouldStartActivity) {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (launchIntent != null) {
                val pendingIntentFlags =
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    pendingIntentFlags
                )
                builder.setContentIntent(pendingIntent)
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}

private const val CHANNEL_ID = "nexus_xservice_default_channel"
private const val CHANNEL_NAME = "Nexus"
private const val NOTIFICATION_ID = 1001