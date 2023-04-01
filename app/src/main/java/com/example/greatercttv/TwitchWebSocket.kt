package com.example.greatercttv

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import okhttp3.*
import kotlin.random.Random


class TwitchWebSocket : WebSocketListener() {

    private lateinit var webSocket: WebSocket
    private lateinit var client: OkHttpClient
    private lateinit var channel: String
    private lateinit var anon: String

    private lateinit var allEmotesTV: List<JsonElement>
    private lateinit var allEmotes7TV: List<JsonElement>
    private lateinit var allEmotesBTTV: List<JsonElement>

    private lateinit var token: String

    private val _messages = mutableStateListOf<List<String>>()
    val messages: List<List<String>> = _messages

    var showToast by mutableStateOf("")

    fun openWebSocket(channel: String) {
        this.channel = channel

        this.client = OkHttpClient()

        val request: Request = Request.Builder().url("ws://irc-ws.chat.twitch.tv:80").build()

        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket onOpen", "Connected to twitch WebSocket")

        this.anon = "justinfan" + Random.nextInt(1000, 80000)

        Log.d("WebSocket onOpen", "onOpen send NICK")
        webSocket.send("NICK $anon")

        Log.d("WebSocket onOpen", "onOpen send JOIN #$channel")
        webSocket.send("JOIN #$channel")

        //TV API Calls

        Log.d("Twitch API", "getting App com.example.greatercttv.Token")
        this.token = getToken()

        Log.d("Twitch API", "getting $channel ID")
        val id = this.getID(token)

        Log.d("Twitch API", "getting $channel Twitch Emotes")
        val emotesTV = getEmotes(token, id)

        Log.d("Twitch API", "getting Twitch global Emotes")
        val globalEmotesTV = getEmotesGlobal(token)

        this.allEmotesTV = emotesTV + globalEmotesTV

        //7TV API Calls

        Log.d("Twitch API", "getting $channel 7TV Emotes")
        var emotes7TV = emptyList<JsonElement>()
        try {
            emotes7TV = get7TVEmote(id)
        } catch (t: Throwable) {
            Log.d("getting $channel 7TV Emotes error", t.toString())
        }

        Log.d("Twitch API", "getting 7TV global Emotes")
        var globalEmotes7TV = emptyList<JsonElement>()
        try {
            globalEmotes7TV = get7TVGlobalEmote()
        } catch (t: Throwable) {
            Log.d("getting $channel 7TV Emotes error", t.toString())
        }

        this.allEmotes7TV = emotes7TV + globalEmotes7TV

        //BTTV API Calls

        Log.d("Twitch API", "getting $channel BTTV Emotes")
        var emotesBTTV = emptyList<JsonElement>()
        try {
            emotesBTTV = getBTTVEmote(id)
        } catch (t: Throwable) {
            Log.d("getting $channel BTTV Emotes error", t.toString())
        }

        Log.d("Twitch API", "getting BTTV global Emotes")
        var globalEmotesBTTV = emptyList<JsonElement>()
        try {
            globalEmotesBTTV = getBTTVGlobalEmote()
        } catch (t: Throwable) {
            Log.d("getting BTTV global Emotes error", t.toString())
        }

        this.allEmotesBTTV = emotesBTTV + globalEmotesBTTV

        showToast = "Connecté"
    }

    private fun getToken(): String {
        val formBody: RequestBody = FormBody.Builder().add("grant_type", "client_credentials")
            .add("client_id", "ayyfine4dksduojooctr26hbt3zms7").add("client_secret", "i231pmhq1986whu08lxzib2br6k77a")
            .build()

        val request: Request =
            Request.Builder().header("Content-Type", "application/json").url("https://id.twitch.tv/oauth2/token")
                .post(formBody).build()

        val client = OkHttpClient()

        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        return jsonObject.get("access_token").asString
    }

    private fun getID(token: String): String {
        val request: Request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
            .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
            .url("https://api.twitch.tv/helix/users?login=" + this.channel).build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        return jsonObject.getAsJsonArray("data")[0].asJsonObject.get("id").asString
    }

    private fun getEmotesGlobal(token: String): List<JsonElement> {
        val request: Request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
            .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
            .url("https://api.twitch.tv/helix/chat/emotes/global").build()
        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        return jsonObject.get("data").asJsonArray.asList()
    }


    private fun getEmotes(token: String, id: String): List<JsonElement> {
        val request: Request = Request.Builder().header("Client-ID", "ayyfine4dksduojooctr26hbt3zms7")
            .header("Accept", "application/vnd.twitchtv.v5+json").header("Authorization", "Bearer $token")
            .url("https://api.twitch.tv/helix/chat/emotes?broadcaster_id=$id").build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        return jsonObject.get("data").asJsonArray.asList()
    }

    private fun getBTTVGlobalEmote(): List<JsonElement> {
        val request: Request = Request.Builder().url("https://api.betterttv.net/3/cached/emotes/global").build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonArray

        return jsonObject.toList()
    }

    private fun getBTTVEmote(id: String): List<JsonElement> {
        val request: Request = Request.Builder().url("https://api.betterttv.net/3/cached/users/twitch/$id").build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        return try {
            val jsonObject = JsonParser.parseString(body).asJsonObject

            val channelEmotes = jsonObject.get("channelEmotes").asJsonArray
            val sharedEmotes = jsonObject.get("sharedEmotes").asJsonArray

            channelEmotes.addAll(sharedEmotes)

            channelEmotes.toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun get7TVGlobalEmote(): List<JsonElement> {
        val request: Request = Request.Builder().url("https://7tv.io/v3/emote-sets/62cdd34e72a832540de95857").build()

        return try {
            val response = this.client.newCall(request).execute()

            val body = response.body?.string() ?: throw Throwable("body response is null")

            val jsonObject = JsonParser.parseString(body).asJsonObject

            jsonObject.get("emotes").asJsonArray.asList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun get7TVEmote(id: String): List<JsonElement> {
        val request: Request = Request.Builder().url("https://7tv.io/v3/users/twitch/$id").build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        return try {
            jsonObject.get("emote_set").asJsonObject.get("emotes").asJsonArray.asList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        //Log.d("Message", text)

        if (!text.contains(":tmi.twitch.tv") && !text.contains(":$anon")) {
            val user = text.split('!').first().drop(1)
            val msg = text.split(':').last()

            val words = msg.split(" ").toMutableList()

            for (i in words.indices) {
                var emote: JsonElement? = null

                //allEmotesTV

                try {
                    if (this.allEmotesTV.isNotEmpty()) {
                        emote = this.allEmotesTV.first {
                            words[i].contains(it.asJsonObject.get("name").asString)
                        }
                    }
                } catch (_: Throwable) {
                }


                if (emote != null) {
                    words[i] = emote.asJsonObject.get("images").asJsonObject.get("url_2x").asString
                    break
                }

                emote = null

                //allEmotesBTTV

                try {
                    if (this.allEmotesBTTV.isNotEmpty()) {
                        emote = this.allEmotesBTTV.first {
                            words[i].contains(it.asJsonObject.get("code").asString)
                        }
                    }
                } catch (_: Throwable) {
                }


                if (emote != null) {
                    val id = emote.asJsonObject.get("id").asString
                    words[i] = "https://cdn.betterttv.net/emote/$id/2x"
                    break
                }

                emote = null

                //allEmotes7TV

                try {
                    if (this.allEmotes7TV.isNotEmpty()) {
                        emote = this.allEmotes7TV.first {
                            words[i].contains(it.asJsonObject.get("name").asString)
                        }
                    }
                } catch (_: Throwable) {
                }

                if (emote != null) {
                    val id = emote.asJsonObject.get("id").asString
                    words[i] = "https://cdn.7tv.app/emote/$id/2x.webp"
                    break
                }
            }

            words.add(0, user)

            if (this._messages.size > 250) {
                this._messages.removeRange(0, 100)
            }

            this._messages.add(words)
        }

        //onMessageReceived?.invoke(text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        Log.d("WebSocket onClosing", "Closing : $code / $reason")

        showToast = "Déconnecté"
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d("WebSocket onFailure", "Error : " + t.message)
        Log.d("WebSocket onFailure", "Response : " + response.toString())

        showToast = "Déconnecté"
    }

    fun closeWebSocket() {
        this.webSocket.close(NORMAL_CLOSURE_STATUS, null)

        showToast = "Déconnecté"
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}
