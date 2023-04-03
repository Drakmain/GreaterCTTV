package com.example.greatercttv

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request

fun getUserInfo(token: String): JsonObject {
    val request: Request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
        .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
        .url("https://api.twitch.tv/helix/users").build()

    val client = OkHttpClient()

    val response = client.newCall(request).execute()

    val body = response.body?.string() ?: throw Throwable("body response is null")

    val jsonObject = JsonParser.parseString(body).asJsonObject

    return jsonObject.getAsJsonArray("data")[0].asJsonObject
}

fun getUserFollow(id: String, token: String, userFollow: JsonArray, cursor: String): String {

    val request = if (cursor.isEmpty()) {
        Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
            .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
            .url("https://api.twitch.tv/helix/channels/followed?user_id=$id&first=100").build()
    } else {
        Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
            .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
            .url("https://api.twitch.tv/helix/channels/followed?user_id=$id&first=100&after=$cursor").build()
    }

    val client = OkHttpClient()

    val response = client.newCall(request).execute()

    val body = response.body?.string() ?: throw Throwable("body response is null")

    val jsonObject = JsonParser.parseString(body).asJsonObject

    userFollow.addAll(jsonObject.getAsJsonArray("data"))

    return try {
        jsonObject.get("pagination").asJsonObject.get("cursor").asString
    } catch (_: Throwable) {
        ""
    }
}


fun getStreams(token: String, userFollow: JsonArray): JsonArray {

    val streams = JsonArray()

    var userIds = ""

    val client = OkHttpClient()

    for (i in 0 until userFollow.size()) {
        val channel = userFollow[i].asJsonObject.get("broadcaster_id").asString
        userIds += "user_id=$channel&"

        if (i % 100 == 0) {
            getStreamsRequest(token, userIds, client, streams)

            userIds = ""
        }
    }

    getStreamsRequest(token, userIds, client, streams)

    return streams
}

fun getStreamsRequest(token: String, userIds: String, client: OkHttpClient, streams: JsonArray) {
    val request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
        .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
        .url("https://api.twitch.tv/helix/streams?$userIds" + "first=100").build()

    val response = client.newCall(request).execute()

    val body = response.body?.string() ?: throw Throwable("body response is null")

    val jsonObject = JsonParser.parseString(body).asJsonObject

    streams.addAll(jsonObject.getAsJsonArray("data"))
}
