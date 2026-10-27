package com.dreamstream.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.max

data class LivePlaybackChannel(
    val name: String,
    val logo: String,
    val url: String,
    val category: String = "Live TV"
)

private val NeonBlue = Color(0xFF00C8FF)
private val DarkPanel = Color(0xE6101526)

private fun displayTime(ms: Long): String {
    val seconds = max(0L, ms) / 1000L
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val remaining = seconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(
            hours, minutes, remaining
        )
    } else {
        "%02d:%02d".format(
            minutes, remaining
        )
    }
}

@Composable
fun VideoPlayer(
    streamUrl: String,
    liveChannels: List<LivePlaybackChannel> = emptyList(),
    liveIndex: Int = 0,
    onLiveIndexChange: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isLive = liveChannels.isNotEmpty()
    val selectedChannel = liveChannels.getOrNull(liveIndex)
    val url = selectedChannel?.url ?: streamUrl

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var controlsVisible by remember {
        mutableStateOf(true)
    }

    var playing by remember {
        mutableStateOf(false)
    }

    var buffering by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var retryCount by remember {
        mutableIntStateOf(0)
    }

    var retryToken by remember {
        mutableIntStateOf(0)
    }

    var position by remember {
        mutableLongStateOf(0L)
    }

    var duration by remember {
        mutableLongStateOf(0L)
    }

    var seekable by remember {
        mutableStateOf(false)
    }

    var aspectIndex by remember {
        mutableIntStateOf(0)
    }

    val aspectNames = listOf(
        "FIT", "FILL", "ZOOM"
    )

    val aspectModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

    val focusRequester = remember {
        FocusRequester()
    }

    fun switchChannel(direction: Int) {
        if (liveChannels.isEmpty()) return

        val size = liveChannels.size
        val next = (
            liveIndex + direction + size
        ) % size

        controlsVisible = true
        onLiveIndexChange(next)
    }

    BackHandler {
        onBack()
    }

    DisposableEffect(activity) {
        val previousOrientation =
            activity?.requestedOrientation

        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation =
                    previousOrientation
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(
                state: Int
            ) {
                buffering =
                    state == Player.STATE_BUFFERING ||
                    state == Player.STATE_IDLE
            }

            override fun onPlayerError(
                exception: PlaybackException
            ) {
                error = exception.errorCodeName
                buffering = false
                controlsVisible = true
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(url) {
        retryCount = 0
        retryToken = 0
        position = 0L
        duration = 0L
        controlsVisible = true
    }

    LaunchedEffect(url, retryToken) {
        error = ""
        buffering = true

        player.setMediaItem(
            MediaItem.fromUri(url)
        )

        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(error, retryCount, url) {
        if (
            error.isNotBlank() &&
            retryCount < 2
        ) {
            delay(2500L)
            retryCount++
            retryToken++
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = max(
                0L,
                player.currentPosition
            )

            val d = player.duration

            duration = if (
                d == C.TIME_UNSET || d < 0L
            ) {
                0L
            } else {
                d
            }

            seekable =
                player.isCurrentMediaItemSeekable

            delay(500L)
        }
    }

    LaunchedEffect(
        controlsVisible,
        playing,
        url
    ) {
        if (
            controlsVisible &&
            playing &&
            error.isBlank()
        ) {
            delay(5000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->

                if (
                    event.type != KeyEventType.KeyDown
                ) {
                    return@onPreviewKeyEvent false
                }

                when (event.key) {

                    Key.DirectionUp -> {
                        if (isLive) {
                            switchChannel(-1)
                            true
                        } else {
                            controlsVisible = true
                            false
                        }
                    }

                    Key.DirectionDown -> {
                        if (isLive) {
                            switchChannel(1)
                            true
                        } else {
                            controlsVisible = true
                            false
                        }
                    }

                    Key.MediaPlayPause -> {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }

                        controlsVisible = true
                        true
                    }

                    Key.DirectionCenter,
                    Key.Enter -> {
                        if (!controlsVisible) {
                            controlsVisible = true
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode =
                    aspectModes[aspectIndex]
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        controlsVisible =
                            !controlsVisible
                    }
                }
        )

        if (
            buffering &&
            error.isBlank()
        ) {
            CircularProgressIndicator(
                color = NeonBlue,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }

        if (
            controlsVisible ||
            error.isNotBlank()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkPanel)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack
                    ) {
                        Text("← BACK")
                    }

                    if (
                        selectedChannel != null
                    ) {
                        if (
                            selectedChannel.logo
                                .startsWith("http")
                        ) {
                            AsyncImage(
                                model =
                                    selectedChannel.logo,
                                contentDescription =
                                    "Channel logo",
                                modifier =
                                    Modifier.size(38.dp)
                            )
                        }

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                selectedChannel.name,
                                color = Color.White,
                                fontWeight =
                                    FontWeight.Bold,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )

                            Text(
                                selectedChannel.category,
                                color = NeonBlue,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    } else {
                        Text(
                            "DREAMSTREAM ULTRA",
                            color = NeonBlue,
                            fontWeight =
                                FontWeight.Black,
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    if (isLive) {
                        Text(
                            "● LIVE",
                            color = Color.Red,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = {
                            aspectIndex =
                                (aspectIndex + 1) %
                                    aspectNames.size
                        }
                    ) {
                        Text(
                            aspectNames[aspectIndex],
                            color = NeonBlue
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkPanel)
                        .padding(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    if (
                        error.isNotBlank()
                    ) {
                        Text(
                            if (
                                retryCount < 2
                            ) {
                                "Reconnecting... " +
                                    "(${retryCount + 1}/2)"
                            } else {
                                "Playback error: $error"
                            },
                            color =
                                Color(0xFFFF7777)
                        )

                        if (
                            retryCount >= 2
                        ) {
                            TextButton(
                                onClick = {
                                    retryCount = 0
                                    retryToken++
                                }
                            ) {
                                Text("↻ TRY AGAIN")
                            }
                        }
                    }

                    if (
                        !isLive &&
                        seekable &&
                        duration > 0L
                    ) {
                        Slider(
                            value = position
                                .coerceIn(
                                    0L,
                                    duration
                                )
                                .toFloat(),
                            onValueChange = {
                                position =
                                    it.toLong()
                            },
                            onValueChangeFinished = {
                                player.seekTo(
                                    position
                                )
                            },
                            valueRange =
                                0f..duration.toFloat(),
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                displayTime(position),
                                color = Color.White
                            )

                            Text(
                                displayTime(duration),
                                color =
                                    Color.LightGray
                            )
                        }
                    }

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        if (isLive) {
                            TextButton(
                                onClick = {
                                    switchChannel(-1)
                                }
                            ) {
                                Text("◀ CH−")
                            }
                        } else if (seekable) {
                            TextButton(
                                onClick = {
                                    player.seekTo(
                                        max(
                                            0L,
                                            player.currentPosition -
                                                10000L
                                        )
                                    )
                                }
                            ) {
                                Text("↶ 10s")
                            }
                        }

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Button(
                            onClick = {
                                if (
                                    player.isPlaying
                                ) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            NeonBlue,
                                        contentColor =
                                            Color.Black
                                    ),
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        ) {
                            Text(
                                if (playing) {
                                    "Ⅱ PAUSE"
                                } else {
                                    "▶ PLAY"
                                },
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        if (isLive) {
                            TextButton(
                                onClick = {
                                    switchChannel(1)
                                }
                            ) {
                                Text("CH+ ▶")
                            }
                        } else if (seekable) {
                            TextButton(
                                onClick = {
                                    player.seekTo(
                                        (
                                            player.currentPosition +
                                                30000L
                                        ).coerceAtMost(
                                            duration
                                        )
                                    )
                                }
                            ) {
                                Text("30s ↷")
                            }
                        }
                    }
                }
            }
        }
    }
}
