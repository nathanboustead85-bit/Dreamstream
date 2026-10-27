package com.dreamstream.app

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
import com.dreamstream.app.ui.live.LiveTvScreen
import com.dreamstream.app.ui.vod.MoviesScreen
import com.dreamstream.app.ui.vod.SeriesScreen
import com.dreamstream.app.ui.player.VideoPlayer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DreamStreamTheme {
                Surface {

                    // Save current navigation screen
                    var screen by rememberSaveable {
                        mutableStateOf("login")
                    }

                    // Remember which section opened the player
                    var returnScreen by rememberSaveable {
                        mutableStateOf("live")
                    }

                    // Preserve IPTV connection details
                    var server by rememberSaveable {
                        mutableStateOf("")
                    }

                    var username by rememberSaveable {
                        mutableStateOf("")
                    }

                    var password by rememberSaveable {
                        mutableStateOf("")
                    }

                    // Preserve the selected video stream
                    var streamUrl by rememberSaveable {
                        mutableStateOf("")
                    }

                    // Handle Back outside the video player
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

                        "home" -> HomeScreen(
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

                        "live" -> LiveTvScreen(
                            server = server,
                            username = username,
                            password = password,

                            onPlay = { url ->
                                streamUrl = url
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
                                returnScreen = "series"
                                screen = "player"
                            },

                            onBack = {
                                screen = "home"
                            }
                        )

                        "player" -> VideoPlayer(
                            streamUrl = streamUrl,

                            onBack = {
                                screen = returnScreen
                            }
                        )
                    }
                }
            }
        }
    }
}
