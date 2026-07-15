package com.example.musicactivity

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object MusicApi {

    private val client = OkHttpClient()

    private const val URL =
        "http://192.168.1.11:3000/api/update"

    fun send(
        song: String?,
        artist: String?,
        app: String?
    ) {

        val json = """
        {
            "song":"${song ?: ""}",
            "artist":"${artist ?: ""}",
            "app":"${app ?: ""}",
            "playing":true
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