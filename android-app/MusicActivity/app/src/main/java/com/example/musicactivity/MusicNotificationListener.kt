package com.example.musicactivity

import android.app.Notification
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MusicNotificationListener : NotificationListenerService() {

    private val allowedApps = setOf(
        "com.spotify.music",
        "com.vivi.vivimusic",
        "app.revanced.android.youtube"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        if (sbn.packageName !in allowedApps) return

        val extras = sbn.notification.extras

        val token: MediaSession.Token? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(
                    Notification.EXTRA_MEDIA_SESSION,
                    MediaSession.Token::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
            }

        val metadata = token?.let {
            MediaController(this, it).metadata
        }

        val title =
            metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: extras.getCharSequence(Notification.EXTRA_TITLE)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }

        val artist =
            metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?.takeIf { it.isNotBlank() }
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(
                sbn.packageName,
                0
            )

            packageManager.getApplicationLabel(appInfo).toString()

        } catch (e: Exception) {
            sbn.packageName
        }

        if (title.isNullOrBlank()) {
            Log.d("MUSIC", "Ignored empty title")
            return
        }

        Log.d("MUSIC", "App: $appName")
        Log.d("MUSIC", "Title: $title")
        Log.d("MUSIC", "Artist: $artist")

        MusicApi.send(
            song = title,
            artist = artist ?: "Unknown artist",
            app = appName
        )
    }
}