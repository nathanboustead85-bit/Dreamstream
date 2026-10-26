@Composable
fun VideoPlayer(streamUrl: String) {

    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(streamUrl) {
        player.setMediaItem(
            MediaItem.fromUri(streamUrl)
        )
        player.prepare()
        player.play()
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
            }
        }
    )
}
