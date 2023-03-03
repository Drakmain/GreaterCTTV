package com.example.greatercttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.greatercttv.ui.theme.GreaterCTTVTheme


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreaterCTTVTheme {

                var channel by remember {
                    mutableStateOf("")
                }

                val mainViewModel: MainViewModel by viewModels()

                val messages = mainViewModel.messages

                val channels = mainViewModel.channels

                var state by remember { mutableStateOf(0) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(value = channel, onValueChange = { text ->
                            channel = text
                        })

                        IconButton(onClick = {
                            if (channel.isNotBlank()) {
                                mainViewModel.openWebSocket(channel)
                                mainViewModel.addChannel(channel)
                            }
                        }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Ajouter une chaine")
                        }

                        IconButton(onClick = {
                            mainViewModel.closeWebSocket()
                            mainViewModel.removeChannel(channel)
                        }) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Supprimer une chaine")
                        }
                    }

                    LazyColumn {
                        items(messages) { message ->
                            Text(text = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}
