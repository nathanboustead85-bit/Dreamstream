
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
fun LoginScreen(onLoginSuccess: () -> Unit) {
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
        Text("DREAMSTREAM ULTRA")

        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = !loading,
            onClick = {
                if (server.isBlank() ||
                    username.isBlank() ||
                    password.isBlank()
                ) {
                    error = "Please complete all fields"
                } else {
                    loading = true
                    error = ""

                    scope.launch {
                        try {
                            val valid = withContext(Dispatchers.IO) {
                                validateLogin(
                                    server.trim(),
                                    username.trim(),
                                    password
                                )
                            }

                            if (valid) {
                                onLoginSuccess()
                            } else {
                                error = "Invalid login or inactive account"
                            }
                        } catch (e: Exception) {
                            error = "Connection failed: " +
                                (e.localizedMessage ?: "Unknown error")
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

private fun validateLogin(
    server: String,
    username: String,
    password: String
): Boolean {
    val base = if (
        server.startsWith("http://") ||
        server.startsWith("https://")
    ) server else "http://$server"

    val endpoint = base.trimEnd('/') + "/player_api.php" +
        "?username=" + URLEncoder.encode(username, "UTF-8") +
        "&password=" + URLEncoder.encode(password, "UTF-8")

    val connection = URL(endpoint).openConnection()
        as HttpURLConnection

    try {
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"

        if (connection.responseCode != 200) {
            return false
        }

        val response = connection.inputStream
            .bufferedReader().use { it.readText() }

        val json = JSONObject(response)
        val auth = json.optJSONObject("user_info")
            ?: return false

        return auth.optInt("auth", 0) == 1 &&
            auth.optString("status")
                .equals("Active", ignoreCase = true)
    } finally {
        connection.disconnect()
    }
}
