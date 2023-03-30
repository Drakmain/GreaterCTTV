package com.example.greatercttv

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    fun fetchUserInformation(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userID = getUserID(token)

            val userFollow = getUserFollow(userID, token)

            Log.d("userFollow", userFollow.toString())
        }
    }
}