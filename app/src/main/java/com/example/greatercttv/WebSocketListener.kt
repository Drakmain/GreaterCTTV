package com.example.greatercttv

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import okhttp3.*
import okhttp3.WebSocketListener
import kotlin.random.Random


class WebSocketListener(
    private val _messages: SnapshotStateList<String>
) : WebSocketListener() {

    private lateinit var webSocket: WebSocket
    private lateinit var channel: String
    private lateinit var anon: String


    private var i = 0

    fun openWebSocket(channel: String) {
        this.channel = channel

        val client = OkHttpClient()

        val request: Request = Request.Builder().url("ws://irc-ws.chat.twitch.tv:80").build()

        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket", "Connected to twitch WebSocket")

        this.anon = "justinfan" + Random.nextInt(1000, 80000)

        Log.d("WebSocket", "onOpen send NICK")
        webSocket.send("NICK $anon")

        Log.d("WebSocket", "onOpen send JOIN")
        webSocket.send("JOIN #$channel")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        if (i == 0 && text.contains(":Welcome, GLHF!")) {
            i++
        } else if (i == 1 && text.contains("JOIN #$channel")) {
            i++
        } else if (i == 2 && text.contains("#$channel :End of /NAMES list")) {
            i++
        } else if (i == 3) {
            val user = text.split('!').first().drop(1)
            val msg = text.split(':').last()

            this._messages.add(0, "$user : $msg")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        Log.d("WebSocket", "Closing : $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d("WebSocket", "Error : " + t.message)
    }

    fun closeWebSocket() {
        /*
        mainActivity.runOnUiThread {
            Toast.makeText(mainActivity, "Close", Toast.LENGTH_SHORT).show()
        }

         */

        this.webSocket.close(NORMAL_CLOSURE_STATUS, null)
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}