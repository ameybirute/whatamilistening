package com.example.musicactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicactivity.ui.theme.MusicActivityTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        MusicApi.fetchStatus()

        enableEdgeToEdge()

        setContent {
            MusicActivityTheme {
                MusicScreen()
            }
        }
    }
}


@Composable
fun MusicScreen() {

    val music by MusicState.musicInfo.collectAsState()

    val sharingEnabled by
    MusicState.sharingEnabled.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1B1B1B)
        ) {

            Column(
                modifier = Modifier
                    .padding(30.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                if (music.artwork != null) {

                    AsyncImage(
                        model = music.artwork,
                        contentDescription = "Album artwork",
                        modifier = Modifier
                            .size(180.dp),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Text(
                        text = "🎵",
                        fontSize = 70.sp
                    )
                }


                Spacer(
                    modifier = Modifier.height(25.dp)
                )


                Text(
                    text = music.song,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                Text(
                    text = music.artist,
                    fontSize = 18.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )


                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFF333333)
                ) {

                    Text(
                        text = music.app,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }


                Spacer(
                    modifier = Modifier.height(25.dp)
                )


                Text(
                    text =
                        if (sharingEnabled)
                            "🟢 Sharing live"
                        else
                            "⏸ Sharing paused",

                    color =
                        if (sharingEnabled)
                            Color(0xFF4ADE80)
                        else
                            Color.Gray,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                Button(
                    onClick = {

                        val newState = !sharingEnabled

                        val listener =
                            MusicNotificationListener.instance

                        // If pausing sharing, send playing=false
                        // to the backend BEFORE disabling sharing
                        if (!newState) {
                            listener?.pauseSharingNow()
                        }

                        // Update sharing state
                        MusicState.setSharingEnabled(newState)

                        // If resuming, immediately send
                        // the current music state to the backend
                        if (newState) {
                            listener?.resumeSharingNow()
                        }

                        // Update the persistent notification
                        listener?.updateSharingNotification()
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (sharingEnabled)
                                Color(0xFFB91C1C)
                            else
                                Color(0xFF15803D)
                    )
                ) {

                    Text(
                        text =
                            if (sharingEnabled)
                                "Pause Sharing"
                            else
                                "Resume Sharing"
                    )
                }

            }

        }

    }
}