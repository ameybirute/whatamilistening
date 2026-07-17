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

        val currentlyEnabled =
            MusicState.sharingEnabled.value

        val newState =
            !currentlyEnabled

        val listener =
            MusicNotificationListener.instance

        // If we're pausing sharing,
        // tell the backend we're no longer live first.
        if (!newState) {
            listener?.pauseSharingNow()
        }

        MusicState.setSharingEnabled(
            newState
        )

        // If we're resuming, immediately publish current state.
        if (newState) {
            listener?.resumeSharingNow()
        }

        listener?.updateSharingNotification()
    }
}