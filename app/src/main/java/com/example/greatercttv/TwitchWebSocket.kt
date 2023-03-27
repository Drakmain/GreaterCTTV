package com.example.greatercttv

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import okhttp3.*
import kotlin.random.Random


class TwitchWebSocket : WebSocketListener() {

    private lateinit var webSocket: WebSocket
    private lateinit var client: OkHttpClient
    private lateinit var channel: String
    private lateinit var anon: String
    lateinit var emotesTV: List<JsonElement>
    lateinit var emotesBTTV: List<JsonElement>
    lateinit var emotes7TV: List<JsonElement>

    lateinit var globalEmotes7TV: List<JsonElement>


    var showToast by mutableStateOf("")

    private val _messages = mutableStateListOf<String>()
    val messages: List<String> = _messages

    private var i = 0

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

        Log.d("WebSocket onOpen", "onOpen send JOIN")
        webSocket.send("JOIN #$channel")

        Log.d("Twitch API", "getting App Token")
        val token = this.getToken()
        Log.d("API Post token", token)

        Log.d("Twitch API", "getting channel ID")
        val id = this.getID(token)

        Log.d("Twitch API", "getting channel Twitch Emotes")
        this.emotesTV = getEmotes(token, id)

        Log.d("Twitch API", "getting channel 7TV Emotes")
        this.emotes7TV = get7TVEmote(id)

        Log.d("Twitch API", "getting channel BTTV Emotes")
        this.emotesBTTV = getBTTVEmote(id)

        Log.d("Twitch API", "getting channel BTTV global Emotes")
        this.getBTTVGlobalEmote()

        showToast = "Connecté"
    }

    private fun getToken(): String {
        val formBody: RequestBody = FormBody.Builder().add("grant_type", "client_credentials")
            .add("client_id", "ayyfine4dksduojooctr26hbt3zms7").add("client_secret", "i231pmhq1986whu08lxzib2br6k77a")
            .build()

        val request: Request =
            Request.Builder().header("Content-Type", "application/json").url("https://id.twitch.tv/oauth2/token")
                .post(formBody).build()

        val response = this.client.newCall(request).execute()

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

        var jsonObject = JsonParser.parseString(body).asJsonObject

        val channelEmotes = jsonObject.get("channelEmotes").asJsonArray
        val sharedEmotes = jsonObject.get("sharedEmotes").asJsonArray

        channelEmotes.addAll(sharedEmotes)

        return channelEmotes.toList()
    }

    private fun get7TVEmote(id: String): List<JsonElement> {
        val request: Request = Request.Builder().url("https://7tv.io/v3/users/TWITCH/$id").build()

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

        Log.d("Message", text)

        if (!text.contains(":tmi.twitch.tv") && !text.contains(":$anon")) {
            val user = text.split('!').first().drop(1)
            val msg = text.split(':').last()

            val words = msg.split(" ").toMutableList()

            for (i in words.indices) {
                Log.d("Twitch f emotesTV", words[i])

                var inds = emptyList<JsonElement>()

                //emotesTV

                if (this.emotesTV.isNotEmpty()) {
                    inds = this.emotesTV.filter {
                        words[i].contains(it.asJsonObject.get("name").asString)
                    }
                }

                if (inds.isNotEmpty()) {
                    words[i] = inds.toString()
                    Log.d("Twitch f emotesTV", inds.toString())
                }

                //emotesBTTV

                if (this.emotesBTTV.isNotEmpty()) {
                    inds = this.emotesBTTV.filter {
                        words[i].contains(it.asJsonObject.get("code").asString)
                    }
                }


                if (inds.isNotEmpty()) {
                    words[i] = inds.toString()
                    Log.d("Twitch f emotesBTTV", inds.toString())
                }

                //emotes7TV

                if (this.emotes7TV.isNotEmpty()) {
                    inds = this.emotes7TV.filter {
                        words[i].contains(it.asJsonObject.get("name").asString)
                    }
                }

                if (inds.isNotEmpty()) {
                    words[i] = inds.toString()
                    Log.d("Twitch f emotes7TV", inds.toString())
                }
            }

            this._messages.add(0, "$user : $msg")
        }

    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        Log.d("WebSocket onClosing", "Closing : $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d("WebSocket onFailure", "Error : " + t.message)
    }

    fun closeWebSocket() {
        this.webSocket.close(NORMAL_CLOSURE_STATUS, null)

        showToast = "Déconnecté"
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}

@Composable
fun ToastShow(webSocketListenerOue: TwitchWebSocket) {
    if (webSocketListenerOue.showToast != "") {
        Toast.makeText(LocalContext.current, webSocketListenerOue.showToast, Toast.LENGTH_SHORT).show()
    }
}