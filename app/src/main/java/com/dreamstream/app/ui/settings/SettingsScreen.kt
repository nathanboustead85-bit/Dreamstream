package com.dreamstream.app.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.dreamstream.app.data.FavouritesStore

@Composable
fun SettingsScreen(
    server: String,
    username: String,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val serverName = when {
        server.contains("dndnscloud.ru", true) -> "Dino"
        server.contains("liondnscloud.ru", true) -> "Lion"
        server.contains("esxtrapro.info", true) -> "Eagle"
        else -> "Custom server"
    }

    var showClearFavourites by remember {
        mutableStateOf(false)
    }

    var showClearLogin by remember {
        mutableStateOf(false)
    }

    var showLogout by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    val loginPrefs = remember(context) {
        context.getSharedPreferences(
            "dreamstream_secure_login",
            Context.MODE_PRIVATE
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B16))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("BACK")
        }

        Text(
            "⚙ SETTINGS",
            color = Color(0xFF00C8FF),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Text(
            "ACCOUNT",
            color = Color(0xFF985BFF),
            fontWeight = FontWeight.Bold
        )

        Text(
            "Server: $serverName",
            color = Color.White
        )

        Text(
            "Username: $username",
            color = Color.White
        )

        HorizontalDivider()

        Text(
            "PRIVACY & STORAGE",
            color = Color(0xFF985BFF),
            fontWeight = FontWeight.Bold
        )

        Text(
            "Saved login: " +
                if (loginPrefs.contains("credentials"))
                    "Stored on this device"
                else
                    "Not saved"
        )

        OutlinedButton(
            onClick = {
                showClearLogin = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CLEAR SAVED LOGIN")
        }

        OutlinedButton(
            onClick = {
                showClearFavourites = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CLEAR ALL FAVOURITES FOR THIS ACCOUNT")
        }

        HorizontalDivider()

        Text(
            "APP",
            color = Color(0xFF985BFF),
            fontWeight = FontWeight.Bold
        )

        Text("DreamStream Ultra Mobile")

        Text(
            "Live TV • Movies • Series • Favourites",
            color = Color.LightGray
        )

        Button(
            onClick = {
                showLogout = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LOG OUT / CHANGE ACCOUNT")
        }

        if (message.isNotEmpty()) {
            Text(
                message,
                color = Color(0xFF00C8FF)
            )
        }
    }

    if (showClearLogin) {
        AlertDialog(
            onDismissRequest = {
                showClearLogin = false
            },
            title = {
                Text("Clear saved login?")
            },
            text = {
                Text(
                    "The next login will require you to enter your credentials again."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        loginPrefs.edit()
                            .remove("credentials")
                            .apply()

                        showClearLogin = false
                        message = "Saved login cleared"
                    }
                ) {
                    Text("CLEAR")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearLogin = false
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showClearFavourites) {
        AlertDialog(
            onDismissRequest = {
                showClearFavourites = false
            },
            title = {
                Text("Clear favourites?")
            },
            text = {
                Text(
                    "Remove all saved Live TV, Movie and Series favourites for this account?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        FavouritesStore.clear(
                            context,
                            server,
                            username
                        )

                        showClearFavourites = false
                        message = "Favourites cleared"
                    }
                ) {
                    Text("CLEAR")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearFavourites = false
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = {
                showLogout = false
            },
            title = {
                Text("Log out?")
            },
            text = {
                Text(
                    "Return to the login screen? Your saved login is retained unless cleared separately."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogout = false
                        onLogout()
                    }
                ) {
                    Text("LOG OUT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogout = false
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

