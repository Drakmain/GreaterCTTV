package com.example.greatercttv

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.greatercttv.ui.theme.GreaterCTTVTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView


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
                    Test(openDialog, mainViewModel, state)

                    IconButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
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

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun Test2(
    openDialog: MutableState<Boolean>,
    mainViewModel: MainViewModel,
    state: MutableState<Int>
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                isVerticalScrollBarEnabled = true
                webViewClient = WebViewClient()
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                loadUrl("http://player.twitch.tv/?channel=sakor_&parent=twitch.tv")
            }
        },
        update = { view ->
            view.loadUrl("http://player.twitch.tv/?channel=sakor_&parent=twitch.tv")
        },
    modifier = Modifier.fillMaxWidth()
        .height(400.dp))
}
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun Test(
    openDialog: MutableState<Boolean>,
    mainViewModel: MainViewModel,
    state: MutableState<Int>
) {
    val offsetY  =  remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val scale = Resources.getSystem().displayMetrics.density

    coroutineScope.launch {
        offsetY.snapTo(-350 * scale)
    }

    val html = "<iframe src=\"https://player.twitch.tv/?channel=otplol_&parent=twitch.tv\" height=\"360\" width=\"640\" allowfullscreen/>"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true

                //loadDataWithBaseURL("twitch.tv", html, "text/html", "UTF-8", null)
                //loadUrl("http://samste-vault.net/embed")
                loadUrl("https://player.twitch.tv/?video=v1773530048&parent=twitch.tv&height=400&width=300")
            }
        },
        /*update = { view ->
            view.loadUrl("https://player.twitch.tv/?channel=sakor_&parent=twitch.tv")
        },*/
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .size(50.dp)
            .draggable(
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        offsetY.snapTo(offsetY.value + delta)
                        if (offsetY.value > 0f)
                            offsetY.snapTo(0f)
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (offsetY.value < -150 * scale)
                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = -350 * scale,
                                animationSpec = tween(
                                    durationMillis = 100,
                                    delayMillis = 0
                                )
                            )
                        }
                    else
                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 100,
                                    delayMillis = 0
                                )
                            )
                        }
                }
            )
            .background(Color.Blue)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Diag(
    openDialog: MutableState<Boolean>,
    mainViewModel: MainViewModel,
    state: MutableState<Int>
) {

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
