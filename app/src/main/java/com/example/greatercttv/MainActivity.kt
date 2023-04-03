package com.example.greatercttv

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.greatercttv.ui.theme.GreaterCTTVTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreaterCTTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val state = remember { mutableStateOf(0) }

                    val openDialog = remember { mutableStateOf(false) }

                    val channelList = mainViewModel.channelList

                    var messages: List<List<String>> = emptyList()

                    var text by remember { mutableStateOf("") }

                    if (channelList.isNotEmpty()) {
                        messages = channelList[state.value].second.messages
                    }

                    Scaffold(topBar = {
                        Pannel(
                            channelList,
                            state,
                            channelList.map { it.first },
                            mainViewModel,
                            openDialog
                        )
                    }, bottomBar = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            TextField(
                                value = text,
                                onValueChange = { newText -> text = newText },
                                label = { Text("Entre ton message") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }, content = {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
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
                        if (openDialog.value) {
                            Diag(openDialog, mainViewModel, state)
                        }
                    })
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
            val rowPlaceables = mutableListOf<Placeable>()

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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Stream(channel: String, heightStream: Float) {
    var webView: WebView? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webView = this
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                settings.domStorageEnabled = true
                this.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(heightStream.dp)
    )
    LaunchedEffect(channel) {
        webView?.loadUrl("https://gcttv.samste-vault.net/stream?width=100%&height=$heightStream&channel=$channel")
    }
}

fun PanelSlideTo(
    pixel: Float,
    offsetY: Animatable<Float, AnimationVector1D>,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        offsetY.animateTo(
            targetValue = pixel,
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = 0
            )
        )
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun Pannel(
    channelList: List<Pair<String, TwitchWebSocket>>, state: MutableState<Int>,
    keys: List<String>,
    mainViewModel: MainViewModel,
    openDialog: MutableState<Boolean>,
) {
    val scale = Resources.getSystem().displayMetrics.density
    val widthScreen = Resources.getSystem().displayMetrics.widthPixels / scale
    val heightStream = widthScreen * 9 / 16
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var dragUporDown = 0
    var iconChevron: ImageVector by remember { mutableStateOf(Icons.Default.ExpandLess) }
    val context = LocalContext.current

    if(channelList.isEmpty()){
        coroutineScope.launch {
            offsetY.snapTo(0f)
        }
    }

    if (offsetY.value >= 0f)
        iconChevron = Icons.Default.ExpandLess
    if (offsetY.value <= -heightStream * scale)
        iconChevron = Icons.Default.ExpandMore

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background)
        .offset { IntOffset(0, offsetY.value.roundToInt()) }
    ) {
        if (channelList.isNotEmpty()) {
            Stream(
                channel = channelList[state.value].first,
                heightStream = heightStream,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .draggable(
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                offsetY.snapTo(offsetY.value + delta)
                                if (delta > 1)
                                    dragUporDown = 1
                                else if (delta < -1)
                                    dragUporDown = -1
                                else if (delta <= 1 && delta >= -1)
                                    dragUporDown = 0
                                if (offsetY.value > 0f)
                                    offsetY.snapTo(0f)
                                if (offsetY.value < (-heightStream * scale))
                                    offsetY.snapTo(-heightStream * scale)
                            }
                        },
                        orientation = Orientation.Vertical,
                        onDragStopped = {
                            if (dragUporDown == 1)
                                if (offsetY.value < -heightStream * scale + 50 * scale)
                                    PanelSlideTo(-heightStream * scale, offsetY, coroutineScope)
                                else
                                    PanelSlideTo(0f, offsetY, coroutineScope)
                            else if (dragUporDown == -1)
                                if (offsetY.value > -50 * scale)
                                    PanelSlideTo(0f, offsetY, coroutineScope)
                                else
                                    PanelSlideTo(-heightStream * scale, offsetY, coroutineScope)
                            else
                                if (offsetY.value < -heightStream * scale / 2)
                                    PanelSlideTo(-heightStream * scale, offsetY, coroutineScope)
                                else
                                    PanelSlideTo(0f, offsetY, coroutineScope)
                        })
            ) {

                Icon(
                    iconChevron,
                    contentDescription = "chevron vers le bas",
                    modifier = Modifier
                        .height(50.dp)
                        .align(Alignment.Center)
                )

                IconButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = {
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

            Tab(
                state,
                keys,
                mainViewModel,
                openDialog,
            )

        } else
            Tab(state, keys, mainViewModel, openDialog)

    }
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
        selectedTabIndex = state.value, modifier = Modifier
            .fillMaxWidth()
            .heightIn(48.dp)
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
                Icons.Default.Add,
                contentDescription = "Ajouter une chaine",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
