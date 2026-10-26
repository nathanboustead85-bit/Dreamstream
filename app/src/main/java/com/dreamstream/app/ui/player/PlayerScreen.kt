package com.dreamstream.app.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(streamUrl: String) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(
                exception: PlaybackException
            ) {
                error = "Playback failed: ${exception.errorCodeName}"
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

        player.setMediaItem(
            MediaItem.fromUri(streamUrl)
        )
        player.prepare()
        player.playWhenReady = true
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { view ->
                view.player = player
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        if (error.isNotEmpty()) {
            Text(error)
        }
    }
}
