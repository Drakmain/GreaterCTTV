package com.example.greatercttv

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {
    private val _channelList = mutableStateListOf<Pair<String, TwitchWebSocket>>()
    val channelList: List<Pair<String, TwitchWebSocket>> = _channelList

    fun openWebSocket(channel: String, state: MutableState<Int>) {
        viewModelScope.launch {
            val webSocketListener = TwitchWebSocket()
            webSocketListener.openWebSocket(channel.lowercase())

            _channelList.add(Pair(channel, webSocketListener))

            state.value = _channelList.size - 1
        }
    }

    fun sendMessage(channel: String, msg: String) {
        val pair = _channelList.find { it.first == channel }
        pair!!.second.sendMessage(msg)
    }

    fun closeWebSocket(channel: String, state: MutableState<Int>) {
        val pair = _channelList.find { it.first == channel }
        pair!!.second.closeWebSocket()
        _channelList.remove(pair)

        if (_channelList.isEmpty()) {
            state.value = 0
        } else {
            state.value = _channelList.size - 1
        }
    }
}