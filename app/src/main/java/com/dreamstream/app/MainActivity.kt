package com.dreamstream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.dreamstream.app.ui.login.LoginScreen
import com.dreamstream.app.ui.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var loggedIn by remember {
                mutableStateOf(false)
            }

            if (loggedIn) {
                HomeScreen()
            } else {
                LoginScreen(
                    onLoginSuccess = {
                        loggedIn = true
                    }
                )
            }
        }
    }
}
