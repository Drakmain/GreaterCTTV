package com.example.greatercttv

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _messages = mutableStateListOf<String>()
    val messages: List<String> = _messages

    private val _channels = mutableStateListOf<String>()
    val channels: List<String> = _channels

    private val webSocketListener = WebSocketListener(_messages)

    fun addChannel(channel: String) {
        _channels.add(channel)
    }

    fun removeChannel(channel: String) {
        _channels.remove(channel)
    }

    fun openWebSocket(channel: String) {
        this.webSocketListener.openWebSocket(channel)
    }

    fun closeWebSocket() {
        this.webSocketListener.closeWebSocket()
    }
}