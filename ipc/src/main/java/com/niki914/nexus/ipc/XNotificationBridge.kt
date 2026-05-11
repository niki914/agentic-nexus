package com.niki914.nexus.ipc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

internal object XNotificationBridge {

    private const val CHANNEL_ID = "nexus_xservice_default_channel"
    private const val CHANNEL_NAME = "Nexus"
    private const val NOTIFICATION_ID = 1001

    fun post(
        context: Context,
        title: String,
        content: String,
        uri: String?
    ) {
        fun checkPermission() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (checkPermission()) return
        ensureNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(resolveSmallIcon(context))
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        createContentIntent(context, uri)?.let(builder::setContentIntent)
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

    private fun createContentIntent(context: Context, uri: String?): PendingIntent? {
        if (uri.isNullOrBlank()) {
            return null
        }
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val resolved = context.packageManager.resolveActivity(intent, 0) ?: return null
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(
            context,
            resolved.activityInfo.packageName.hashCode(),
            intent,
            pendingIntentFlags
        )
    }

    private fun resolveSmallIcon(context: Context): Int {
        return context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info
    }
}