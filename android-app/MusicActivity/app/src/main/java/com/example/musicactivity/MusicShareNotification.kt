package com.example.musicactivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object MusicShareNotification {

    const val CHANNEL_ID = "music_share"

    fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Sharing",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description = "Shows when music data is being shared"

            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}