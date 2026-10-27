package com.dreamstream.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlin.math.max

private val PlayerBlue = Color(0xFF00C8FF)
private val PlayerBackground = Color(0xFF080B16)
private val PlayerPanel = Color(0xDD151B30)

private fun formatTime(milliseconds: Long): String {
    val seconds = max(0L, milliseconds) / 1000L
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val remaining = seconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, remaining)
    } else {
        "%02d:%02d".format(minutes, remaining)
    }
}

@Composable
fun VideoPlayer(
    streamUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isLive = remember(streamUrl) {
        streamUrl.contains("/live/", ignoreCase = true)
    }

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var isBuffering by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var position by remember {
        mutableLongStateOf(0L)
    }

    var duration by remember {
        mutableLongStateOf(0L)
    }

    var canSeek by remember {
        mutableStateOf(false)
    }

    var controlsVisible by remember {
        mutableStateOf(true)
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
                playing: Boolean
            ) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                isBuffering =
                    playbackState == Player.STATE_BUFFERING ||
                    playbackState == Player.STATE_IDLE
            }

            override fun onPlayerError(
                exception: PlaybackException
            ) {
                error = "Playback failed: " +
                    exception.errorCodeName
                isBuffering = false
            }

            override fun onEvents(
                player: Player,
                events: Player.Events
            ) {
                val d = player.duration

                duration = if (
                    d == C.TIME_UNSET || d < 0L
                ) 0L else d

                canSeek = player.isCurrentMediaItemSeekable
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(streamUrl, player) {
        error = ""
        position = 0L
        duration = 0L
        isBuffering = true

        player.setMediaItem(
            MediaItem.fromUri(streamUrl)
        )
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(player) {
        while (true) {
            position = max(0L, player.currentPosition)

            val d = player.duration
            duration = if (
                d == C.TIME_UNSET || d < 0L
            ) 0L else d

            canSeek = player.isCurrentMediaItemSeekable

            delay(500L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    resizeMode =
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setOnClickListener {
                        controlsVisible = !controlsVisible
                    }
                }
            },
            update = { view ->
                view.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && error.isBlank()) {
            CircularProgressIndicator(
                modifier = Modifier.align(
                    Alignment.Center
                ),
                color = PlayerBlue
            )
        }

        if (controlsVisible || error.isNotBlank()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlayerPanel)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack
                    ) {
                        Text("← BACK")
                    }

                    Text(
                        "DREAMSTREAM ULTRA",
                        color = PlayerBlue,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isLive) {
                        Text(
                            "● LIVE",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlayerPanel)
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    if (error.isNotBlank()) {
                        Text(
                            error,
                            color = Color(0xFFFF7777)
                        )
                    }

                    if (!isLive &&
                        canSeek &&
                        duration > 0L
                    ) {
                        Slider(
                            value = position
                                .coerceIn(0L, duration)
                                .toFloat(),
                            onValueChange = { value ->
                                position = value.toLong()
                            },
                            onValueChangeFinished = {
                                player.seekTo(position)
                            },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(position),
                                color = Color.White
                            )
                            Text(
                                formatTime(duration),
                                color = Color.LightGray
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        if (!isLive && canSeek) {
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

                        Spacer(Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                            colors = ButtonDefaults
                                .buttonColors(
                                    containerColor = PlayerBlue,
                                    contentColor =
                                        PlayerBackground
                                ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isPlaying) "Ⅱ PAUSE"
                                else "▶ PLAY",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        if (!isLive && canSeek) {
                            TextButton(
                                onClick = {
                                    player.seekTo(
                                        (
                                            player.currentPosition +
                                                30000L
                                        ).coerceAtMost(duration)
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
