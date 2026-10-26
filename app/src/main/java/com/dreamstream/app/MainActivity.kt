package com.dreamstream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import com.dreamstream.app.ui.login.LoginScreen
import com.dreamstream.app.ui.home.HomeScreen
import com.dreamstream.app.ui.live.LiveTvScreen
import com.dreamstream.app.ui.player.VideoPlayer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var screen by remember { mutableStateOf("login") }

            var server by remember { mutableStateOf("") }
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            var streamUrl by remember { mutableStateOf("") }

            BackHandler(
                enabled = screen == "live" || screen == "player"
            ) {
                screen = if (screen == "player") "live" else "home"
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
                    onLiveTv = { screen = "live" }
                )

                "live" -> LiveTvScreen(
                    server = server,
                    username = username,
                    password = password,
                    onPlay = { url ->
                        streamUrl = url
                        screen = "player"
                    },
                    onBack = { screen = "home" }
                )

                "player" -> VideoPlayer(
                    streamUrl = streamUrl
                )
            }
        }
    }
}
