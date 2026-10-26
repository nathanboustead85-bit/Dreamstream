package com.dreamstream.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String) -> Unit
) {
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "DREAMSTREAM ULTRA",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (error.isNotEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = !loading,
            onClick = {
                if (
                    server.isBlank() ||
                    username.isBlank() ||
                    password.isBlank()
                ) {
                    error = "Please complete all fields"
                } else {
                    loading = true
                    error = ""

                    scope.launch {
                        try {
                            val base = normalizeServer(server)

                            val valid = withContext(Dispatchers.IO) {
                                validateLogin(
                                    base,
                                    username.trim(),
                                    password
                                )
                            }

                            if (valid) {
                                onLoginSuccess(
                                    base,
                                    username.trim(),
                                    password
                                )
                            } else {
                                error = "Invalid login or inactive account"
                            }
                        } catch (e: Exception) {
                            error = "Connection failed. Check server and internet."
                        } finally {
                            loading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "CONNECTING..." else "CONNECT")
        }
    }
}

private fun normalizeServer(server: String): String {
    val value = server.trim().trimEnd('/')

    return if (
        value.startsWith("http://", true) ||
        value.startsWith("https://", true)
    ) {
        value
    } else {
        "http://$value"
    }
}

private fun validateLogin(
    server: String,
    username: String,
    password: String
): Boolean {
    val endpoint = server + "/player_api.php" +
        "?username=" + URLEncoder.encode(username, "UTF-8") +
        "&password=" + URLEncoder.encode(password, "UTF-8")

    val connection = URL(endpoint).openConnection()
        as HttpURLConnection

    try {
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode != 200) {
            return false
        }

        val response = connection.inputStream
            .bufferedReader().use { it.readText() }

        val info = JSONObject(response)
            .optJSONObject("user_info") ?: return false

        return info.optInt("auth", 0) == 1 &&
            info.optString("status")
                .equals("Active", ignoreCase = true)
    } finally {
        connection.disconnect()
    }
}
