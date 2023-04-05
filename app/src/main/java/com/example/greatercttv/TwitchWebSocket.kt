package com.example.greatercttv

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.*
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import okhttp3.*
import java.util.*
import kotlin.random.Random


@SuppressLint("MutableCollectionMutableState")
class TwitchWebSocket : WebSocketListener() {

    private lateinit var webSocket: WebSocket
    private lateinit var client: OkHttpClient
    private lateinit var channel: String
    private var anon: String = "justinfan" + Random.nextInt(1000, 80000)

    lateinit var allEmotesTV: List<JsonElement>
    lateinit var allEmotes7TV: List<JsonElement>
    lateinit var allEmotesBTTV: List<JsonElement>
    lateinit var allEmotesFFZ: List<JsonElement>

    private lateinit var token: String

    private val _messages = mutableStateListOf<ParsedMessage?>()
    val messages: List<ParsedMessage?> = _messages

    var showToast by mutableStateOf("")

    fun openWebSocket(channel: String) {
        this.channel = channel

        this.client = OkHttpClient()

        val request: Request = Request.Builder().url("wss://irc-ws.chat.twitch.tv:443").build()

        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket onOpen", "Connected to twitch WebSocket")

        val authViewModel = AuthViewModel.getInstance()

        if (authViewModel.connected) {

            Log.d("WebSocket onOpen", "send CAP REQ")
            webSocket.send("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership")

            Log.d("WebSocket onOpen", "send PASS")
            webSocket.send("PASS oauth:${authViewModel.userToken}")

            Log.d("WebSocket onOpen", "send NICK " + authViewModel.userName)
            webSocket.send("NICK " + authViewModel.userName)

            Log.d("WebSocket onOpen", "send JOIN #$channel")
            webSocket.send("JOIN #$channel")
        } else {
            Log.d("WebSocket onOpen", "send NICK")
            webSocket.send("NICK $anon")

            Log.d("WebSocket onOpen", "send JOIN #$channel")
            webSocket.send("JOIN #$channel")
        }

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
            Log.e("getting $channel 7TV Emotes", t.toString())
        }

        Log.d("Twitch API", "getting 7TV global Emotes")
        var globalEmotes7TV = emptyList<JsonElement>()
        try {
            globalEmotes7TV = get7TVGlobalEmote()
        } catch (t: Throwable) {
            Log.e("getting $channel 7TV Emotes", t.toString())
        }

        this.allEmotes7TV = emotes7TV + globalEmotes7TV

        //BTTV API Calls

        Log.d("Twitch API", "getting $channel BTTV Emotes")
        var emotesBTTV = emptyList<JsonElement>()
        try {
            emotesBTTV = getBTTVEmote(id)
        } catch (t: Throwable) {
            Log.e("getting $channel BTTV Emotes", t.toString())
        }

        Log.d("Twitch API", "getting BTTV global Emotes")
        var globalEmotesBTTV = emptyList<JsonElement>()
        try {
            globalEmotesBTTV = getBTTVGlobalEmote()
        } catch (t: Throwable) {
            Log.e("getting BTTV global Emotes", t.toString())
        }

        this.allEmotesBTTV = emotesBTTV + globalEmotesBTTV

        //FFZ API Calls

        Log.d("Twitch API", "getting $channel FFZ Emotes")
        var emotesFFZ = emptyList<JsonElement>()
        try {
            emotesFFZ = getFFZEmote(id)
        } catch (t: Throwable) {
            Log.e("getting $channel FFZ Emotes", t.toString())
        }

        Log.d("Twitch API", "getting FFZ global Emotes")
        var globalEmotesFFZ = emptyList<JsonElement>()
        try {
            globalEmotesFFZ = getFFZGlobalEmote()
        } catch (t: Throwable) {
            Log.e("getting FFZ global Emotes", t.toString())
        }

        this.allEmotesFFZ = emotesFFZ + globalEmotesFFZ

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

    private fun getFFZGlobalEmote(): List<JsonElement> {
        val request: Request = Request.Builder().url("https://api.frankerfacez.com/v1/set/global").build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        return jsonObject.get("sets").asJsonObject.get("3").asJsonObject.get("emoticons").asJsonArray.asList()
    }

    private fun getFFZEmote(id: String): List<JsonElement> {
        val request: Request = Request.Builder().url("https://api.frankerfacez.com/v1/room/id/$id").build()

        val response = this.client.newCall(request).execute()

        val body = response.body?.string() ?: throw Throwable("body response is null")

        val jsonObject = JsonParser.parseString(body).asJsonObject

        val set = jsonObject.get("room").asJsonObject.get("set").asString

        return jsonObject.get("sets").asJsonObject.get(set).asJsonObject.get("emoticons").asJsonArray.asList()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

         val parsed = try {
            parseMessage(text)
        } catch (t: Throwable){
            Log.e("parseMessage(text)", t.toString())
             null
        }

        if (parsed != null && parsed.command?.command == "PRIVMSG") {
            parsed.split = parsed.parameters!!.split(" ").toMutableList()

            searchEmote(parsed)

            this._messages.add(parsed)
        }

    }

    private fun searchEmote(parsed: ParsedMessage) {
        for (i in parsed.split!!.indices) {
            var emote: JsonElement? = null

            //allEmotesTV

            try {
                if (this.allEmotesTV.isNotEmpty()) {
                    emote = this.allEmotesTV.firstOrNull {
                        parsed.split!![i].contains(it.asJsonObject.get("name").asString)
                    }
                }

                if (emote != null) {
                    parsed.split!![i] = emote.asJsonObject.get("images").asJsonObject.get("url_4x").asString
                }

            } catch (t: Throwable) {
                Log.e("allEmotesTV Error", "$t")
                Log.e("allEmotesTV emote", "$emote")
            }

            emote = null

            //allEmotesBTTV

            try {
                if (this.allEmotesBTTV.isNotEmpty()) {
                    emote = this.allEmotesBTTV.firstOrNull {
                        parsed.split!![i].contains(it.asJsonObject.get("code").asString)
                    }
                }

                if (emote != null) {
                    val id = emote.asJsonObject.get("id").asString
                    parsed.split!![i] = "https://cdn.betterttv.net/emote/$id/3x"
                }
            } catch (t: Throwable) {
                Log.e("allEmotesBTTV Error", "$t")
                Log.e("allEmotesTV emote", "$emote")
            }

            emote = null

            //allEmotes7TV

            try {
                if (this.allEmotes7TV.isNotEmpty()) {
                    emote = this.allEmotes7TV.firstOrNull {
                        parsed.split!![i].contains(it.asJsonObject.get("name").asString)
                    }
                }

                if (emote != null) {
                    val id = emote.asJsonObject.get("id").asString
                    parsed.split!![i] = "https://cdn.7tv.app/emote/$id/3x.webp"
                }
            } catch (t: Throwable) {
                Log.e("allEmotes7TV Error", "$t")
                Log.e("allEmotesTV emote", "$emote")
            }

            emote = null

            //allEmotesFFZ

            try {
                if (this.allEmotesFFZ.isNotEmpty()) {
                    emote = this.allEmotesFFZ.firstOrNull {
                        parsed.split!![i].contains(it.asJsonObject.get("name").asString)
                    }
                }

                if (emote != null) {
                    val id = emote.asJsonObject.get("id").asString
                    parsed.split!![i] = "https://cdn.frankerfacez.com/emote/$id/4"
                }
            } catch (t: Throwable) {
                Log.e("allEmotesFFZ Error", "$t")
                Log.e("allEmotesTV emote", "$emote")
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        Log.d("WebSocket onClosing", "Closing : $code / $reason")

        showToast = "Déconnecté"
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d("WebSocket onFailure", "Error : $t")
        Log.d("WebSocket onFailure", "Response : " + response.toString())

        showToast = "Déconnecté"
    }

    fun closeWebSocket() {
        this.webSocket.close(NORMAL_CLOSURE_STATUS, null)

        showToast = "Déconnecté"
    }

    fun sendMessage(msg: String) {
        if (msg.isNotEmpty()) {
            val authViewModel = AuthViewModel.getInstance()

            this.webSocket.send("PRIVMSG #$channel :$msg")

            val parsed = ParsedMessage(mutableMapOf(), null, null, null, msg.split(" ").toMutableList())

            parsed.tags!!["display-name"] = authViewModel.userName

            searchEmote(parsed)

            this._messages.add(parsed)
        }
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}
