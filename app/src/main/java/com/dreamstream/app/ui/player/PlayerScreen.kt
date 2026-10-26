
package com.dreamstream.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(streamUrl: String) {
    val context = LocalContext.current

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(streamUrl, player) {
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
            }
        }
    )
}
