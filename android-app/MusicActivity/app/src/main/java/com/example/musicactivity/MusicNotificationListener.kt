package com.example.musicactivity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicNotificationListener : NotificationListenerService() {

    companion object {
        var instance: MusicNotificationListener? = null

        private const val CHANNEL_ID = "music_sharing_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE_SHARING =
            "com.example.musicactivity.TOGGLE_SHARING"
    }

    private val allowedApps = setOf(
        "com.spotify.music",
        "com.vivi.vivimusic",
        "app.revanced.android.youtube"
    )

    private val handler =
        Handler(Looper.getMainLooper())

    private var currentController: MediaController? = null

    private var currentTitle: String? = null
    private var currentArtist: String? = null
    private var currentAppName: String? = null

    private var isPlaying = false


    private val periodicUpdate =
        object : Runnable {

            override fun run() {

                if (
                    isPlaying &&
                    MusicState.sharingEnabled.value
                ) {
                    sendCurrentStatus()

                    handler.postDelayed(
                        this,
                        60_000
                    )
                }
            }
        }


    override fun onListenerConnected() {

        super.onListenerConnected()

        instance = this

        createNotificationChannel()

        updateSharingNotification()
    }


    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        if (sbn.packageName !in allowedApps) {
            return
        }

        val extras =
            sbn.notification.extras


        val token: MediaSession.Token? =

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                extras.getParcelable(
                    Notification.EXTRA_MEDIA_SESSION,
                    MediaSession.Token::class.java
                )

            } else {

                @Suppress("DEPRECATION")

                extras.getParcelable(
                    Notification.EXTRA_MEDIA_SESSION
                )
            }


        val controller =
            token?.let {
                MediaController(this, it)
            }


        val metadata =
            controller?.metadata


        val title =

            metadata
                ?.getString(
                    MediaMetadata.METADATA_KEY_TITLE
                )
                ?.takeIf {
                    it.isNotBlank()
                }

                ?: extras
                    .getCharSequence(
                        Notification.EXTRA_TITLE
                    )
                    ?.toString()
                    ?.takeIf {
                        it.isNotBlank()
                    }


        val artist =

            metadata
                ?.getString(
                    MediaMetadata.METADATA_KEY_ARTIST
                )
                ?.takeIf {
                    it.isNotBlank()
                }

                ?: metadata
                    ?.getString(
                        MediaMetadata.METADATA_KEY_ALBUM_ARTIST
                    )
                    ?.takeIf {
                        it.isNotBlank()
                    }

                ?: extras
                    .getCharSequence(
                        Notification.EXTRA_TEXT
                    )
                    ?.toString()
                    ?.takeIf {
                        it.isNotBlank()
                    }


        if (title.isNullOrBlank()) {
            return
        }


        val appName = try {

            val appInfo =
                packageManager.getApplicationInfo(
                    sbn.packageName,
                    0
                )

            packageManager
                .getApplicationLabel(appInfo)
                .toString()

        } catch (e: Exception) {

            sbn.packageName
        }


        val newArtist =
            artist ?: "Unknown artist"


        val songChanged =

            currentTitle != title ||
                    currentArtist != newArtist ||
                    currentAppName != appName


        val previousPlayingState =
            isPlaying


        currentTitle =
            title

        currentArtist =
            newArtist

        currentAppName =
            appName


        isPlaying =

            controller
                ?.playbackState
                ?.state == PlaybackState.STATE_PLAYING


        MusicState.update(
            song = title,
            artist = newArtist,
            app = appName
        )


        if (controller != null) {

            attachMediaController(
                controller
            )
        }


        val playbackChanged =

            previousPlayingState !=
                    isPlaying


        if (
            songChanged ||
            playbackChanged
        ) {

            sendCurrentStatus()
        }


        updatePeriodicTimer()

        updateSharingNotification()
    }


    private fun attachMediaController(
        controller: MediaController
    ) {

        if (
            currentController?.sessionToken ==
            controller.sessionToken
        ) {
            return
        }


        currentController
            ?.unregisterCallback(
                mediaCallback
            )


        currentController =
            controller


        currentController
            ?.registerCallback(
                mediaCallback
            )
    }


    private val mediaCallback =

        object : MediaController.Callback() {


            override fun onMetadataChanged(
                metadata: MediaMetadata?
            ) {

                val title =

                    metadata
                        ?.getString(
                            MediaMetadata.METADATA_KEY_TITLE
                        )


                val artist =

                    metadata
                        ?.getString(
                            MediaMetadata.METADATA_KEY_ARTIST
                        )

                        ?: metadata
                            ?.getString(
                                MediaMetadata.METADATA_KEY_ALBUM_ARTIST
                            )


                if (
                    title.isNullOrBlank()
                ) {
                    return
                }


                val newArtist =

                    artist
                        ?: "Unknown artist"


                val songChanged =

                    currentTitle != title ||
                            currentArtist != newArtist


                currentTitle =
                    title

                currentArtist =
                    newArtist


                MusicState.update(
                    song = title,
                    artist = newArtist,
                    app =
                        currentAppName
                            ?: ""
                )


                if (
                    songChanged
                ) {

                    sendCurrentStatus()
                }


                updateSharingNotification()
            }


            override fun onPlaybackStateChanged(
                state: PlaybackState?
            ) {

                val previousPlayingState =
                    isPlaying


                isPlaying =

                    state?.state ==
                            PlaybackState.STATE_PLAYING


                if (
                    previousPlayingState !=
                    isPlaying
                ) {

                    sendCurrentStatus()
                }


                updatePeriodicTimer()

                updateSharingNotification()
            }
        }


    fun resumeSharingNow() {

        sendCurrentStatus()

        updatePeriodicTimer()

        updateSharingNotification()
    }


    private fun sendCurrentStatus() {

        val title =
            currentTitle ?: return

        val artist =
            currentArtist ?: return

        val app =
            currentAppName ?: return


        if (
            !MusicState.sharingEnabled.value
        ) {

            Log.d(
                "MUSIC",
                "Sharing paused"
            )

            return
        }


        MusicApi.send(
            song = title,
            artist = artist,
            app = app,
            playing = isPlaying
        )


        handler.postDelayed(
            {
                MusicApi.fetchStatus()
            },
            1500
        )


        Log.d(
            "MUSIC",
            "Sent: $title | Playing: $isPlaying"
        )
    }


    private fun updatePeriodicTimer() {

        handler.removeCallbacks(
            periodicUpdate
        )


        if (
            isPlaying &&
            MusicState.sharingEnabled.value
        ) {

            handler.postDelayed(
                periodicUpdate,
                60_000
            )
        }
    }


    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Music Sharing",
                    NotificationManager.IMPORTANCE_LOW
                )


            channel.description =
                "Shows whether music sharing is active"


            val manager =

                getSystemService(
                    NotificationManager::class.java
                )


            manager.createNotificationChannel(
                channel
            )
        }
    }


    fun updateSharingNotification() {

        val sharingEnabled =
            MusicState.sharingEnabled.value


        val notificationTitle =

            if (sharingEnabled) {

                "🎵 Sharing music"

            } else {

                "⏸ Music sharing paused"
            }


        val notificationText =

            if (
                sharingEnabled &&
                currentTitle != null
            ) {

                "$currentTitle — ${currentArtist ?: "Unknown artist"}"

            } else if (
                sharingEnabled
            ) {

                "Waiting for music..."

            } else {

                "Sharing is currently paused"
            }


        val actionText =

            if (sharingEnabled) {

                "Pause Sharing"

            } else {

                "Resume Sharing"
            }


        val actionIntent =

            Intent(
                this,
                MusicActionReceiver::class.java
            ).apply {

                action =
                    ACTION_TOGGLE_SHARING
            }


        val actionPendingIntent =

            PendingIntent.getBroadcast(
                this,
                0,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        val openAppIntent =

            Intent(
                this,
                MainActivity::class.java
            )


        val openAppPendingIntent =

            PendingIntent.getActivity(
                this,
                1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        val notification =

            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )

                .setSmallIcon(
                    android.R.drawable.ic_media_play
                )

                .setContentTitle(
                    notificationTitle
                )

                .setContentText(
                    notificationText
                )

                .setContentIntent(
                    openAppPendingIntent
                )

                .setOnlyAlertOnce(
                    true
                )

                .setOngoing(
                    sharingEnabled
                )

                .addAction(
                    android.R.drawable.ic_media_pause,
                    actionText,
                    actionPendingIntent
                )

                .build()


        val manager =

            getSystemService(
                NotificationManager::class.java
            )


        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    fun pauseSharingNow() {

        val title =
            currentTitle ?: return

        val artist =
            currentArtist ?: return

        val app =
            currentAppName ?: return

        MusicApi.send(
            song = title,
            artist = artist,
            app = app,
            playing = false
        )

        handler.removeCallbacks(
            periodicUpdate
        )
    }


    override fun onDestroy() {

        instance = null


        handler.removeCallbacks(
            periodicUpdate
        )


        currentController
            ?.unregisterCallback(
                mediaCallback
            )


        super.onDestroy()
    }
}