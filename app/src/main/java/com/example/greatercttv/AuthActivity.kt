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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.greatercttv.ui.theme.GreaterCTTVTheme

class AuthActivity : ComponentActivity() {
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Spinner()
                }
            }
        }
    }
}

@Composable
fun Spinner() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}