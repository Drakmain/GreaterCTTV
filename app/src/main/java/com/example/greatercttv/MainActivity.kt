package com.example.greatercttv

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.greatercttv.ui.theme.GreaterCTTVTheme
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val authViewModel = AuthViewModel.getInstance()

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreaterCTTVTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val state = remember { mutableStateOf(0) }

                    val openDialog = remember { mutableStateOf(false) }

                    val openDialogEmotes = remember { mutableStateOf(false) }

                    val channelList = mainViewModel.channelList

                    var messages: List<ParsedMessage?> = emptyList()

                    var text by remember { mutableStateOf("") }

                    val imageLoader = ImageLoader.Builder(LocalContext.current)
                        .components {
                            if (SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()

                    val offsetY = remember { Animatable(0f) }

                    val scale = Resources.getSystem().displayMetrics.density

                    val widthScreen = Resources.getSystem().displayMetrics.widthPixels / scale

                    val heightStream = widthScreen * 9 / 16

                    if (channelList.isNotEmpty()) {
                        messages = channelList[state.value].second.messages
                    }

                    Scaffold(topBar = {
                        Pannel(
                            channelList,
                            state,
                            channelList.map { it.first },
                            mainViewModel,
                            authViewModel,
                            openDialog,
                            offsetY,
                        )
                    }, bottomBar = {
                        if (channelList.isNotEmpty()) {
                            if (authViewModel.connected && channelList[state.value].second.showToast == "Connecté") {
                                Box(
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    TextField(
                                        value = text,
                                        onValueChange = { newText ->
                                            text = newText
                                        },
                                        label = { Text(getString(R.string.send_message)) },
                                        modifier = Modifier.fillMaxWidth().height(55.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions {
                                            mainViewModel.sendMessage(channelList[state.value].first, text)
                                            text = ""
                                        }
                                    )
                                }
                            }
                        }
                    }, content = {
                        val coroutineScope = rememberCoroutineScope()
                        Box(
                            modifier = Modifier
                                .padding(
                                    top = ((offsetY.value / scale + heightStream).dp + 103.dp),
                                    bottom = if (authViewModel.connected) {
                                        57.dp
                                    } else {
                                        0.dp
                                    }
                                )
                                .fillMaxSize()
                                .draggable(
                                    state = rememberDraggableState { },
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = {
                                        if (it < 0 && channelList.getOrNull(state.value + 1) != null) {
                                            state.value++
                                        } else if (it > 0 && channelList.getOrNull(state.value - 1) != null) {
                                            state.value--
                                        }
                                    })
                        ) {
                            if (channelList.isEmpty()) {
                                NoChannelText(resources)
                            } else {
                                if (channelList[state.value].second.showToast != "Connecté") {
                                    Spinner()
                                }

                                if (messages.isNotEmpty()) {
                                    ChatLazyColumn(messages, authViewModel, imageLoader)
                                }

                                if (authViewModel.connected && channelList[state.value].second.showToast == "Connecté") {
                                    FloatingActionButton(
                                        onClick = { openDialogEmotes.value = true },
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                            .absolutePadding(right = 10.dp, bottom = 10.dp)
                                    ) {
                                        Icon(Icons.Default.EmojiEmotions, contentDescription = "emotes")
                                    }
                                }
                            }
                        }

                        if (openDialogEmotes.value) {
                            DiagEmotes(
                                openDialogEmotes, mainViewModel, state, imageLoader,
                                onChange = { newText ->
                                    text += " $newText "
                                }, this.resources
                            )
                        }

                        if (openDialog.value) {
                            Diag(openDialog, mainViewModel, authViewModel, state, this.resources)
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagEmotes(
    openDialogEmotes: MutableState<Boolean>,
    mainViewModel: MainViewModel,
    state: MutableState<Int>,
    imageLoader: ImageLoader,
    onChange: (String) -> Unit,
    resources: Resources
) {
    val emotesTV = mainViewModel.channelList[state.value].second.allEmotesTV.toList()
    val emotes7TV = mainViewModel.channelList[state.value].second.allEmotes7TV.toList()
    val emotesBTTV = mainViewModel.channelList[state.value].second.allEmotesBTTV.toList()
    val emotesFFZ = mainViewModel.channelList[state.value].second.allEmotesFFZ.toList()

    var selectedItem by remember { mutableStateOf(0) }

    val TV = painterResource(R.drawable.tv)
    val STV = painterResource(R.drawable.stv)
    val BTTV = painterResource(R.drawable.bttv)
    val FFZ = painterResource(R.drawable.ffz)

    val items = mutableListOf<String>()

    if (emotesTV.isNotEmpty()) {
        items.add("TV")
    }

    if (emotes7TV.isNotEmpty()) {
        items.add("7TV")
    }

    if (emotesBTTV.isNotEmpty()) {
        items.add("BTTV")
    }

    if (emotesFFZ.isNotEmpty()) {
        items.add("FFZ")
    }
    AlertDialog(onDismissRequest = {
        openDialogEmotes.value = false
    }, title = {
        Text(resources.getString(R.string.choose_emote))
    }, text = {
        Column {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            when (item) {
                                "TV" -> Icon(TV, contentDescription = item, modifier = Modifier.size(24.dp))
                                "7TV" -> Icon(STV, contentDescription = item, modifier = Modifier.size(24.dp))
                                "BTTV" -> Icon(BTTV, contentDescription = item, modifier = Modifier.size(24.dp))
                                "FFZ" -> Icon(FFZ, contentDescription = item, modifier = Modifier.size(24.dp))
                            }
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }

            val emotes = when (selectedItem) {
                0 -> emotesTV
                1 -> emotes7TV
                2 -> emotesBTTV
                3 -> emotesFFZ
                else -> {
                    listOf()
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 50.dp)
            ) {
                items(emotes) { emote ->
                    Card(
                        onClick = {
                            onChange(switchName(selectedItem, emote))
                            openDialogEmotes.value = false
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.padding(5.dp).align(Alignment.CenterHorizontally)

                    ) {

                        val model = switchURL(selectedItem, emote)

                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(model)
                                .size(Size.ORIGINAL)
                                .build(),
                            imageLoader = imageLoader
                        )

                        Image(
                            painter = painter,
                            contentDescription = "emote",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .absolutePadding(top = 5.dp, bottom = 5.dp)
                        )
                    }
                }
            }
        }
    }, confirmButton = {

    }, dismissButton = {
        TextButton(onClick = {
            openDialogEmotes.value = false
        }) {
            Text(resources.getString(R.string.cancel))
        }
    })
}

fun switchName(selectedItem: Int, emote: JsonElement): String {
    return when (selectedItem) {
        0, 1, 3 -> {
            emote.asJsonObject.get("name").asString
        }

        2 -> {
            emote.asJsonObject.get("code").asString
        }

        else -> {
            ""
        }
    }
}

fun switchURL(selectedItem: Int, emote: JsonElement): String {
    return when (selectedItem) {
        0 -> {
            emote.asJsonObject.get("images").asJsonObject.get("url_4x").asString
        }

        1 -> {
            val id = emote.asJsonObject.get("id").asString
            "https://cdn.7tv.app/emote/$id/3x.webp"
        }

        2 -> {
            val id = emote.asJsonObject.get("id").asString
            "https://cdn.betterttv.net/emote/$id/3x"
        }

        3 -> {
            val id = emote.asJsonObject.get("id").asString
            "https://cdn.frankerfacez.com/emote/$id/4"
        }

        else -> {
            ""
        }
    }
}

@Composable
fun NoChannelText(resources: Resources) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = resources.getString(R.string.no_channel_add)
        )
    }
}

@Composable
fun ChatLazyColumn(messages: List<ParsedMessage?>, authViewModel: AuthViewModel, imageLoader: ImageLoader) {
    val scrollState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        scrollState.animateScrollToItem(messages.size - 1)
    }

    LazyColumn(state = scrollState) {
        items(messages) { message ->
            MessageAnnotated(message, authViewModel, imageLoader)
        }
    }
}

@Composable
fun MessageAnnotated(message: ParsedMessage?, authViewModel: AuthViewModel, imageLoader: ImageLoader) {

    val inlineContentMap = mutableMapOf<String, InlineTextContent>()

    val annotatedString = buildAnnotatedString {
        if (authViewModel.connected) {
            withStyle(style = SpanStyle(color = Color(MaterialTheme.colorScheme.primary.toArgb()))) {
                append(message?.tags?.get("display-name")?.toString() ?: "oue")
            }
        } else {
            withStyle(style = SpanStyle(color = Color(MaterialTheme.colorScheme.primary.toArgb()))) {
                append(message!!.source!!.nick.toString())
            }
        }

        append(": ")

        if (message != null) {
            for (word in message.split!!) {
                if (word.contains("https://cdn.7tv.app/emote/") || word.contains("https://cdn.betterttv.net/emote/") || word.contains(
                        "https://static-cdn.jtvnw.net/emoticons"
                    ) || word.contains("https://cdn.frankerfacez.com/emote")
                ) {
                    appendInlineContent(id = word)

                    inlineContentMap[word] = InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)
                    ) {
                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(word)
                                .size(Size.ORIGINAL) // Set the target size to load the image at.
                                .build(),
                            imageLoader = imageLoader
                        )

                        Image(
                            painter = painter,
                            contentDescription = ""
                        )
                    }
                } else {
                    append(word)
                }
                append(" ")
            }
        }
    }

    Text(annotatedString, inlineContent = inlineContentMap)
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

fun panelSlideTo(
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
    authViewModel: AuthViewModel,
    openDialog: MutableState<Boolean>,
    offsetY: Animatable<Float, AnimationVector1D>
) {
    val scale = Resources.getSystem().displayMetrics.density
    val widthScreen = Resources.getSystem().displayMetrics.widthPixels / scale
    val heightStream = widthScreen * 9 / 16
    val coroutineScope = rememberCoroutineScope()
    var dragUporDown = 0
    var iconChevron: ImageVector by remember { mutableStateOf(Icons.Default.ExpandLess) }
    val context = LocalContext.current

    if (channelList.isEmpty()) {
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
        .offset { IntOffset(0, offsetY.value.roundToInt()) }
    ) {
        if (channelList.isNotEmpty()) {
            Stream(
                channel = channelList[state.value].first,
                heightStream = heightStream,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                //.background(MaterialTheme.colorScheme.scrim)
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
                                panelSlideTo(-heightStream * scale, offsetY, coroutineScope)
                            else
                                panelSlideTo(0f, offsetY, coroutineScope)
                        else if (dragUporDown == -1)
                            if (offsetY.value > -50 * scale)
                                panelSlideTo(0f, offsetY, coroutineScope)
                            else
                                panelSlideTo(-heightStream * scale, offsetY, coroutineScope)
                        else
                            if (offsetY.value < -heightStream * scale / 2)
                                panelSlideTo(-heightStream * scale, offsetY, coroutineScope)
                            else
                                panelSlideTo(0f, offsetY, coroutineScope)
                    })
        ) {
            if (channelList.isNotEmpty()) {
                Icon(
                    iconChevron,
                    contentDescription = "chevron vers le bas",
                    modifier = Modifier
                        .height(50.dp)
                        .align(Alignment.Center)
                )
            }
            IconButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = {
                if (authViewModel.connected) {
                    authViewModel.onDisconnect()
                } else {
                    val authUrl =
                        "https://id.twitch.tv/oauth2/authorize?client_id=ayyfine4dksduojooctr26hbt3zms7&redirect_uri=https://gcttv.samste-vault.net/twitch_auth_redirect&response_type=code" +
                                "&scope=user:read:follows" +
                                "+chat:edit" +
                                "+chat:read"

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                    context.startActivity(intent)
                }
            }) {
                if (authViewModel.connected) {
                    Icon(
                        Icons.Default.Logout, contentDescription = "Se déconnecter"
                    )
                } else {
                    Icon(
                        Icons.Default.Login, contentDescription = "Se connecter"
                    )
                }
            }

            if (authViewModel.connected) {
                AsyncImage(
                    model = authViewModel.userPP, contentDescription = "PP",
                    modifier = Modifier.size(45.dp).padding(5.dp)
                        .clip(CircleShape),
                    alignment = Alignment.Center
                )
            }
        }

        Tab(
            state,
            keys,
            mainViewModel,
            openDialog,
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Diag(
    openDialog: MutableState<Boolean>,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    state: MutableState<Int>,
    resources: Resources
) {

    var channel by remember {
        mutableStateOf("")
    }

    AlertDialog(onDismissRequest = {
        openDialog.value = false
    }, title = {
        Text(text = resources.getString(R.string.add_channel))
    }, text = {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = channel, onValueChange = { text ->
                    channel = text
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    openDialog.value = false
                    mainViewModel.openWebSocket(channel, state)
                })
            )

            if (authViewModel.connected) {
                Text(text = resources.getString(R.string.followed_channel), style = MaterialTheme.typography.bodyLarge)

                LazyColumn {
                    items(authViewModel.streams.toList()) { channelInfo ->

                        val channelName =
                            channelInfo.asJsonObject.get("user_name").toString().replace("\"", "")

                        ListItem(
                            modifier = Modifier.clickable {
                                openDialog.value = false
                                mainViewModel.openWebSocket(channelName, state)
                            },
                            headlineText = { Text(channelName) },
                            trailingContent = { Text(channelInfo.asJsonObject.get("viewer_count").toString()) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Favorite,
                                    "Icone"
                                )
                            }
                        )
                        Divider()
                    }
                }

            }
        }
    }, confirmButton = {
        TextButton(onClick = {
            openDialog.value = false
            mainViewModel.openWebSocket(channel, state)
        }) {
            Text(resources.getString(R.string.add))
        }
    }, dismissButton = {
        TextButton(onClick = {
            openDialog.value = false
        }) {
            Text(resources.getString(R.string.cancel))
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
        //containerColor = MaterialTheme.colorScheme.scrim,
        //contentColor = MaterialTheme.colorScheme.secondary,
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
