package com.example.greatercttv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class AuthViewModel : ViewModel() {

    var connected by mutableStateOf(false)

    var userToken by mutableStateOf("")

    var userInfo by mutableStateOf(JsonObject())

    var userName by mutableStateOf("")

    var userPP by mutableStateOf("")

    var userFollow by mutableStateOf(JsonArray())

    var streams by mutableStateOf(JsonArray())

    fun fetchUserInformation(token: String) {
        userToken = token

        userInfo = getUserInfo(token)

        val userID = userInfo.get("id").asString

        userName = userInfo.get("login").toString().replace("\"", "")

        userPP = userInfo.get("profile_image_url").asString

        var cursor = ""

        do {
            cursor = getUserFollow(userID, token, userFollow, cursor)
        } while (cursor != "")

        streams = getStreams(token, userFollow)

        connected = true
    }

    fun onDisconnect() {
        userPP = ""

        userInfo = JsonObject()

        userName = ""

        userFollow = JsonArray()

        streams = JsonArray()

        connected = false
    }

    companion object {
        private var instance: AuthViewModel? = null

        fun getInstance(): AuthViewModel {
            if (instance == null) {
                instance = AuthViewModel()
            }
            return instance as AuthViewModel
        }
    }
}

