package com.dreamstream.app.ui.login

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private data class ServerOption(
    val name: String,
    val url: String
)

private val servers = listOf(
    ServerOption(
        "Dino",
        "http://line.dndnscloud.ru"
    ),
    ServerOption(
        "Lion",
        "http://vpn.liondnscloud.ru:80"
    ),
    ServerOption(
        "Eagle",
        "http://vpn-esxtrapro.info:80"
    )
)

private const val PREFS = "dreamstream_secure_login"
private const val KEY_ALIAS = "dreamstream_login_aes_v1"

private fun secretKey(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    val existing = store.getKey(
        KEY_ALIAS,
        null
    ) as? SecretKey

    if (existing != null) {
        return existing
    }

    return KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore"
    ).apply {
        init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()
        )
    }.generateKey()
}

private fun encrypt(value: String): String {
    val cipher = Cipher.getInstance(
        "AES/GCM/NoPadding"
    )

    cipher.init(
        Cipher.ENCRYPT_MODE,
        secretKey()
    )

    val combined = cipher.iv +
        cipher.doFinal(
            value.toByteArray(Charsets.UTF_8)
        )

    return Base64.encodeToString(
        combined,
        Base64.NO_WRAP
    )
}

private fun decrypt(value: String): String {
    val bytes = Base64.decode(
        value,
        Base64.NO_WRAP
    )

    require(bytes.size > 12)

    val cipher = Cipher.getInstance(
        "AES/GCM/NoPadding"
    )

    cipher.init(
        Cipher.DECRYPT_MODE,
        secretKey(),
        GCMParameterSpec(
            128,
            bytes.copyOfRange(0, 12)
        )
    )

    return String(
        cipher.doFinal(
            bytes.copyOfRange(12, bytes.size)
        ),
        Charsets.UTF_8
    )
}

private data class SavedLogin(
    val serverName: String,
    val username: String,
    val password: String
)

private fun loadLogin(
    context: Context
): SavedLogin? {
    val prefs = context.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE
    )

    val payload = prefs.getString(
        "credentials",
        null
    ) ?: return null

    return try {
        val json = JSONObject(
            decrypt(payload)
        )

        SavedLogin(
            json.getString("server"),
            json.getString("username"),
            json.getString("password")
        )
    } catch (_: Exception) {
        prefs.edit()
            .remove("credentials")
            .apply()

        null
    }
}

private fun saveLogin(
    context: Context,
    login: SavedLogin
) {
    val json = JSONObject().apply {
        put("server", login.serverName)
        put("username", login.username)
        put("password", login.password)
    }

    context.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE
    )
        .edit()
        .putString(
            "credentials",
            encrypt(json.toString())
        )
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String) -> Unit
) {
    val context = LocalContext.current

    val saved = remember(context) {
        loadLogin(context)
    }

    var selected by remember {
        mutableStateOf(
            servers.firstOrNull {
                it.name == saved?.serverName
            } ?: servers.first()
        )
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var username by remember {
        mutableStateOf(saved?.username ?: "")
    }

    var password by remember {
        mutableStateOf(saved?.password ?: "")
    }

    var rememberLogin by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "DREAMSTREAM ULTRA",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                "SIGN IN",
                style =
                    MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {
                OutlinedTextField(
                    value = selected.name,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Select Server")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults
                            .TrailingIcon(expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    servers.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(option.name)
                            },
                            onClick = {
                                selected = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                },
                label = {
                    Text("Username")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Password")
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberLogin,
                    onCheckedChange = {
                        rememberLogin = it
                        if (!it) {
                            context.getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                            ).edit()
                                .remove("credentials")
                                .apply()
                        }
                    }
                )

                Text("Remember my login")
            }

            if (error.isNotEmpty()) {
                Text(
                    error,
                    color =
                        MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (
                        username.isBlank() ||
                        password.isBlank()
                    ) {
                        error =
                            "Enter your username and password"
                    } else {
                        loading = true
                        error = ""

                        scope.launch {
                            try {
                                val valid = withContext(
                                    Dispatchers.IO
                                ) {
                                    validateLogin(
                                        selected.url,
                                        username.trim(),
                                        password
                                    )
                                }

                                if (valid) {
                                    if (rememberLogin) {
                                        try {
                                            saveLogin(
                                                context,
                                                SavedLogin(
                                                    selected.name,
                                                    username.trim(),
                                                    password
                                                )
                                            )
                                        } catch (_: Exception) {
                                            error =
                                                "Connected, but could not save login on this device"
                                        }
                                    } else {
                                        context.getSharedPreferences(
                                            PREFS,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .remove("credentials")
                                            .apply()
                                    }

                                    onLoginSuccess(
                                        selected.url,
                                        username.trim(),
                                        password
                                    )
                                } else {
                                    error =
                                        "Invalid login or inactive account"
                                }
                            } catch (_: Exception) {
                                error =
                                    "Connection failed. Check your internet or server"
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (loading)
                        "CONNECTING..."
                    else
                        "CONNECT"
                )
            }
        }
    }
}

private fun validateLogin(
    server: String,
    username: String,
    password: String
): Boolean {
    val endpoint =
        server.trimEnd('/') +
            "/player_api.php" +
            "?username=" +
            URLEncoder.encode(
                username,
                "UTF-8"
            ) +
            "&password=" +
            URLEncoder.encode(
                password,
                "UTF-8"
            )

    val connection =
        URL(endpoint).openConnection()
            as HttpURLConnection

    return try {
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode != 200) {
            return false
        }

        val response =
            connection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        val info =
            JSONObject(response)
                .optJSONObject("user_info")
                ?: return false

        info.optInt("auth", 0) == 1 &&
            info.optString("status")
                .equals(
                    "Active",
                    ignoreCase = true
                )
    } finally {
        connection.disconnect()
    }
}
