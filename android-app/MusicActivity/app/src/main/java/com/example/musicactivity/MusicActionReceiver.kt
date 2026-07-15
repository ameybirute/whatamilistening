package com.example.musicactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicActionReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            MusicNotificationListener.ACTION_TOGGLE_SHARING
        ) {
            return
        }

        val newState =
            !MusicState.sharingEnabled.value

        MusicState.setSharingEnabled(
            newState
        )

        val listener =
            MusicNotificationListener.instance

        if (newState) {
            listener?.resumeSharingNow()
        } else {
            listener?.updateSharingNotification()
        }
    }
}