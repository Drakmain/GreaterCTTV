package com.example.greatercttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.greatercttv.ui.theme.GreaterCTTVTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreaterCTTVTheme {
                val mainViewModel: MainViewModel by viewModels()

                val state = remember { mutableStateOf(0) }

                val openDialog = remember { mutableStateOf(false) }

                val channelList = mainViewModel.channelList

                var messages: List<String> = listOf("")

                if (channelList.isNotEmpty()) {
                    messages = channelList[state.value].second.messages.toList()
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    IconButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Default.ExpandMore, contentDescription = "Ajouter une chaine")
                    }

                    Tab(state, channelList.map { it.first }, mainViewModel, openDialog)

                    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            when {
                                dragAmount > 0 -> {
                                    if (channelList.getOrNull(state.value - 1) != null) {
                                        state.value--
                                    }
                                }

                                dragAmount < 0 -> {
                                    if (channelList.getOrNull(state.value + 1) != null) {
                                        state.value++
                                    }
                                }
                            }
                        }
                    }) {
                        LazyColumn {
                            items(messages) { message ->
                                Text(text = message)
                                /*
                                AsyncImage(
                                    model = "https://cdn.7tv.app/emote/6133b422d6b0df560a6525b2/3x.webp",
                                    contentDescription = "Translated description of what the image contains"
                                )
                                */
                            }
                        }
                        //ToastShow(channelList[state.value].second)
                    }

                    if (openDialog.value) {
                        Diag(openDialog, mainViewModel, state)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Diag(openDialog: MutableState<Boolean>, mainViewModel: MainViewModel, state: MutableState<Int>) {

    var channel by remember {
        mutableStateOf("")
    }

    AlertDialog(onDismissRequest = {
        openDialog.value = false
    }, title = {
        Text(text = "Ajouter une chaine")
    }, text = {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(value = channel, onValueChange = { text ->
                channel = text
            })
        }
    }, confirmButton = {
        TextButton(onClick = {
            openDialog.value = false
            mainViewModel.openWebSocket(channel, state)
        }) {
            Text("Ajouter")
        }
    }, dismissButton = {
        TextButton(onClick = {
            openDialog.value = false
        }) {
            Text("Annuler")
        }
    })
}

@Composable
fun Tab(
    state: MutableState<Int>,
    keys: List<String>,
    mainViewModel: MainViewModel,
    openDialog: MutableState<Boolean>,
) {
    ScrollableTabRow(
        selectedTabIndex = state.value, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        keys.forEachIndexed { index, channel ->
            Tab(selected = state.value == index, onClick = {
                state.value = index
            }, text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = channel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(modifier = Modifier.size(32.dp), onClick = {
                        mainViewModel.closeWebSocket(channel, state)
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Supprimer une chaine",
                        )
                    }
                }
            })
        }
        IconButton(onClick = { openDialog.value = true }) {
            Icon(
                Icons.Default.Add, contentDescription = "Ajouter une chaine", modifier = Modifier.size(32.dp)
            )
        }
    }
}
