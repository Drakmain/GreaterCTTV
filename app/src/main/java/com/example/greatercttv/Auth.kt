package com.example.greatercttv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.greatercttv.ui.theme.GreaterCTTVTheme

class Auth : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authViewModel: AuthViewModel by viewModels()

        if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            val accessToken = uri?.getQueryParameter("access_token")
            val scope = uri?.getQueryParameter("scope")
            val error = uri?.getQueryParameter("error")

            Log.d("Token", uri.toString())

            if (accessToken != null) {

                authViewModel.fetchUserInformation(accessToken)

            } else if (error != null) {
                // Handle the error
            }
        }

        setContent {
            GreaterCTTVTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Chargement ..."
                    )
                }
            }
        }
    }

}