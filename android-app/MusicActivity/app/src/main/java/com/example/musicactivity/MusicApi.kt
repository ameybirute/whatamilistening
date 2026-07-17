package com.example.musicactivity

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object MusicApi {

    private val client = OkHttpClient()

    private const val URL =
        "https://api.whatamilistening.com/api/update"

    private const val STATUS_URL =
        "https://api.whatamilistening.com/api/status"

    fun fetchStatus() {

        val request = Request.Builder()
            .url(STATUS_URL)
            .get()
            .build()

        client.newCall(request).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: java.io.IOException
                ) {
                    e.printStackTrace()
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {
                    val body = response.body?.string() ?: return

                    val json = org.json.JSONObject(body)

                    val song = json.optString("song")
                    val artist = json.optString("artist")
                    val app = json.optString("app")
                    val artwork =
                        json.optString("artwork")
                            .takeIf { it.isNotBlank() && it != "null" }

                    MusicState.update(
                        song = song,
                        artist = artist,
                        app = app,
                        artwork = artwork
                    )

                    response.close()
                }
            }
        )
    }
    fun send(
        song: String?,
        artist: String?,
        app: String?,
        playing: Boolean
    ) {

        val json = """
        {
            "song":"${song ?: ""}",
            "artist":"${artist ?: ""}",
            "app":"${app ?: ""}",
            "playing":$playing
        }
        """.trimIndent()

        val body = json.toRequestBody(
            "application/json".toMediaType()
        )

        val request = Request.Builder()
            .url(URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {
                    e.printStackTrace()
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {
                    response.close()
                }
            }
        )
    }
}