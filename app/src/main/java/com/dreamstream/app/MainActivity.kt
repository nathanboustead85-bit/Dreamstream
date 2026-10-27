package com.dreamstream.app

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Surface

import com.dreamstream.app.ui.theme.DreamStreamTheme
import com.dreamstream.app.ui.login.LoginScreen
import com.dreamstream.app.ui.home.HomeScreen
import com.dreamstream.app.ui.home.TvHomeScreen
import com.dreamstream.app.ui.live.LiveTvScreen
import com.dreamstream.app.ui.vod.MoviesScreen
import com.dreamstream.app.ui.vod.SeriesScreen
import com.dreamstream.app.ui.player.VideoPlayer
import com.dreamstream.app.ui.player.LivePlaybackChannel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTvDevice =
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_LEANBACK
            ) ||
            (
                resources.configuration.uiMode and
                Configuration.UI_MODE_TYPE_MASK
            ) == Configuration.UI_MODE_TYPE_TELEVISION

        setContent {
            DreamStreamTheme {
                Surface {
                    var screen by rememberSaveable {
                        mutableStateOf("login")
                    }

                    var returnScreen by rememberSaveable {
                        mutableStateOf("live")
                    }

                    var server by rememberSaveable {
                        mutableStateOf("")
                    }

                    var username by rememberSaveable {
                        mutableStateOf("")
                    }

                    var password by rememberSaveable {
                        mutableStateOf("")
                    }

                    var streamUrl by rememberSaveable {
                        mutableStateOf("")
                    }

                    var liveIndex by rememberSaveable {
                        mutableIntStateOf(0)
                    }

                    var livePlaylist by remember {
                        mutableStateOf(
                            emptyList<LivePlaybackChannel>()
                        )
                    }

                    BackHandler(
                        enabled = screen != "login" &&
                            screen != "home" &&
                            screen != "player"
                    ) {
                        screen = "home"
                    }

                    when (screen) {
                        "login" -> LoginScreen(
                            onLoginSuccess = { s, u, p ->
                                server = s
                                username = u
                                password = p
                                screen = "home"
                            }
                        )

                        "home" -> {
                            if (isTvDevice) {
                                TvHomeScreen(
                                    onLiveTv = {
                                        screen = "live"
                                    },
                                    onMovies = {
                                        screen = "movies"
                                    },
                                    onSeries = {
                                        screen = "series"
                                    }
                                )
                            } else {
                                HomeScreen(
                                    onLiveTv = {
                                        screen = "live"
                                    },
                                    onMovies = {
                                        screen = "movies"
                                    },
                                    onSeries = {
                                        screen = "series"
                                    }
                                )
                            }
                        }

                        "live" -> LiveTvScreen(
                            server = server,
                            username = username,
                            password = password,
                            onPlay = { playlist, index ->
                                livePlaylist = playlist
                                liveIndex = index
                                returnScreen = "live"
                                screen = "player"
                            },
                            onBack = {
                                screen = "home"
                            }
                        )

                        "movies" -> MoviesScreen(
                            server = server,
                            username = username,
                            password = password,
                            onPlay = { url ->
                                streamUrl = url
                                livePlaylist = emptyList()
                                returnScreen = "movies"
                                screen = "player"
                            },
                            onBack = {
                                screen = "home"
                            }
                        )

                        "series" -> SeriesScreen(
                            server = server,
                            username = username,
                            password = password,
                            onPlay = { url ->
                                streamUrl = url
                                livePlaylist = emptyList()
                                returnScreen = "series"
                                screen = "player"
                            },
                            onBack = {
                                screen = "home"
                            }
                        )

                        "player" -> VideoPlayer(
                            streamUrl = streamUrl,
                            liveChannels = if (
                                returnScreen == "live"
                            ) livePlaylist else emptyList(),
                            liveIndex = liveIndex,
                            onLiveIndexChange = { index ->
                                liveIndex = index
                            },
                            onBack = {
                                livePlaylist = emptyList()
                                screen = returnScreen
                            }
                        )
                    }
                }
            }
        }
    }
}
