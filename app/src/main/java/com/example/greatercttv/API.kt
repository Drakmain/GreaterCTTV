package com.example.greatercttv

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request

fun getUserID(token: String): String {
    val request: Request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
        .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
        .url("https://api.twitch.tv/helix/users").build()

    val client = OkHttpClient()

    val response = client.newCall(request).execute()

    val body = response.body?.string() ?: throw Throwable("body response is null")

    val jsonObject = JsonParser.parseString(body).asJsonObject

    return jsonObject.getAsJsonArray("data")[0].asJsonObject.get("id").asString
}

fun getUserFollow(id: String, token: String): JsonArray {
    val request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
        .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
        .url("https://api.twitch.tv/helix/channels/followed?user_id=$id").build()

    val client = OkHttpClient()

    val response = client.newCall(request).execute()

    val body = response.body?.string() ?: throw Throwable("body response is null")

    val jsonObject = JsonParser.parseString(body).asJsonObject

    return jsonObject.getAsJsonArray("data").asJsonArray
}
