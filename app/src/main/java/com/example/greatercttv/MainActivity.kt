package com.example.greatercttv

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.greatercttv.ui.theme.GreaterCTTVTheme

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreaterCTTVTheme {

                val mainViewModel: MainViewModel by viewModels()

                val state = remember { mutableStateOf(0) }

                val openDialog = remember { mutableStateOf(false) }

                val channelList = mainViewModel.channelList

                var messages: List<List<String>> = emptyList()

                if (channelList.isNotEmpty()) {
                    messages = channelList[state.value].second.messages
                }

                val context = LocalContext.current

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.ExpandMore, contentDescription = "Ajouter une chaine")
                            }

                            IconButton(onClick = {
                                val authUrl =
                                    "https://id.twitch.tv/oauth2/authorize" + "?client_id=ayyfine4dksduojooctr26hbt3zms7" + "&redirect_uri=https://gcttv.samste-vault.net/twitch_auth_redirect" + "&response_type=code" + "&scope=user:read:follows"

                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                context.startActivity(intent)
                            }) {
                                Icon(
                                    Icons.Default.Login, contentDescription = "Se connecter"
                                )
                            }
                        }

                        Tab(state, channelList.map { it.first }, mainViewModel, openDialog)

                        Box(modifier = Modifier.pointerInput(Unit) {
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
                            if (channelList.isEmpty()) {
                                NoChannelText()
                            } else {
                                if (channelList[state.value].second.showToast != "Connecté") {
                                    Spinner()
                                }

                                if (messages.isNotEmpty()) {
                                    ChatLazyColumn(messages)
                                }
                            }
                        }

                        //ToastShow(channelList[state.value].second)
                    }
                }

                var text by remember { mutableStateOf("") }
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
                ) {
                    TextField(
                        value = text,
                        onValueChange = { newText -> text = newText },
                        label = { Text("Entre ton message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (openDialog.value) {
                    Diag(openDialog, mainViewModel, state)
                }
            }
        }
    }
}

@Composable
fun NoChannelText() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aucune chaîne ajoutée"
        )
    }
}

@Composable
fun ChatLazyColumn(messages: List<List<String>>) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        scrollState.animateScrollToItem(messages.size - 1)
    }

    LazyColumn(state = scrollState) {
        items(messages) { message ->
            MessageBox(message)
        }
    }
}

@Composable
fun MessageBox(message: List<String>) {
    BoxWithConstraints {
        Layout(content = {
            Text(text = message[0] + ": ")
            for (word in message.subList(1, message.size)) {
                if (word.contains("https://cdn.7tv.app/emote/") || word.contains("https://cdn.betterttv.net/emote/") || word.contains(
                        "https://static-cdn.jtvnw.net/emoticons"
                    )
                ) {
                    AsyncImage(
                        model = word, contentDescription = "emote"
                    )
                } else {
                    Text(text = word)
                }
                Text(text = " ")
            }
        }, measurePolicy = { measurables, constraints ->
            var rowWidth = 0
            var rowHeight = 0
            var rowPlaceables = mutableListOf<Placeable>()

            val rows = mutableListOf<List<Placeable>>()
            var currentRow = mutableListOf<Placeable>()

            measurables.forEach { measurable ->
                val placeable = measurable.measure(constraints)

                if (rowWidth + placeable.width > constraints.maxWidth) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    rowWidth = 0
                    rowHeight += rowPlaceables.maxOfOrNull { it.height } ?: 0
                    rowPlaceables.clear()
                }

                rowWidth += placeable.width
                rowPlaceables.add(placeable)
                currentRow.add(placeable)
            }

            if (currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowHeight += rowPlaceables.maxOfOrNull { it.height } ?: 0
            }

            layout(constraints.maxWidth, rowHeight) {
                var y = 0

                rows.forEach { rowPlaceables ->
                    var x = 0

                    rowPlaceables.forEach { placeable ->
                        placeable.placeRelative(x, y)
                        x += placeable.width
                    }

                    y += rowPlaceables.maxOfOrNull { it.height } ?: 0
                }
            }
        })
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
