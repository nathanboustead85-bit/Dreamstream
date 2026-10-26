package com.dreamstream.app.ui.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LiveCategoryItem(
    val id: String,
    val name: String
)

data class LiveChannelItem(
    val id: Int,
    val name: String,
    val categoryId: String,
    val extension: String
)

@Composable
fun LiveTvScreen(
    server: String,
    username: String,
    password: String,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    var categories by remember {
        mutableStateOf<List<LiveCategoryItem>>(emptyList())
    }

    var channels by remember {
        mutableStateOf<List<LiveChannelItem>>(emptyList())
    }

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(server, username, password) {
        loading = true
        error = ""

        try {
            val result = withContext(Dispatchers.IO) {
                val categoryData = apiRequest(
                    server, username, password,
                    "get_live_categories"
                )

                val channelData = apiRequest(
                    server, username, password,
                    "get_live_streams"
                )

                val categoryList = mutableListOf<LiveCategoryItem>()
                val channelList = mutableListOf<LiveChannelItem>()

                for (i in 0 until categoryData.length()) {
                    val obj = categoryData.getJSONObject(i)

                    categoryList.add(
                        LiveCategoryItem(
                            id = obj.optString("category_id"),
                            name = obj.optString(
                                "category_name",
                                "Unnamed category"
                            )
                        )
                    )
                }

                for (i in 0 until channelData.length()) {
                    val obj = channelData.getJSONObject(i)
                    val id = obj.optInt("stream_id", -1)

                    if (id < 0) continue

                    channelList.add(
                        LiveChannelItem(
                            id = id,
                            name = obj.optString(
                                "name",
                                "Unnamed channel"
                            ),
                            categoryId = obj.optString("category_id"),
                            extension = obj.optString(
                                "container_extension",
                                "ts"
                            )
                        )
                    )
                }

                Pair(categoryList, channelList)
            }

            categories = result.first
            channels = result.second
        } catch (e: Exception) {
            error = "Unable to load Live TV: " +
                (e.localizedMessage ?: "Unknown error")
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = {
            if (selectedCategory != null) {
                selectedCategory = null
            } else {
                onBack()
            }
        }) {
            Text("BACK")
        }

        Text(
            if (selectedCategory == null) "LIVE TV CATEGORIES"
            else "CHANNELS",
            style = MaterialTheme.typography.headlineSmall
        )

        if (loading) {
            CircularProgressIndicator()
        } else if (error.isNotEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )
        } else if (selectedCategory == null) {
            LazyColumn {
                item {
                    Text(
                        "All channels (${channels.size})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = "ALL" }
                            .padding(14.dp)
                    )
                    HorizontalDivider()
                }

                items(categories) { category ->
                    Text(
                        category.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = category.id
                            }
                            .padding(14.dp)
                    )
                    HorizontalDivider()
                }
            }
        } else {
            val visibleChannels = if (selectedCategory == "ALL") {
                channels
            } else {
                channels.filter {
                    it.categoryId == selectedCategory
                }
            }

            Text("${visibleChannels.size} channels")

            LazyColumn {
                items(visibleChannels) { channel ->
                    Text(
                        channel.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val extension =
                                    channel.extension
                                        .ifBlank { "ts" }
                                        .lowercase()
                                        .let {
                                            if (it == "m3u8") "m3u8"
                                            else "ts"
                                        }

                                val encodedUser =
                                    URLEncoder.encode(
                                        username, "UTF-8"
                                    ).replace("+", "%20")

                                val encodedPass =
                                    URLEncoder.encode(
                                        password, "UTF-8"
                                    ).replace("+", "%20")

                                val url =
                                    server.trimEnd('/') +
                                    "/live/" +
                                    encodedUser + "/" +
                                    encodedPass + "/" +
                                    channel.id + "." +
                                    extension

                                onPlay(url)
                            }
                            .padding(14.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun apiRequest(
    server: String,
    username: String,
    password: String,
    action: String
): JSONArray {
    val endpoint =
        server.trimEnd('/') + "/player_api.php" +
        "?username=" + URLEncoder.encode(username, "UTF-8") +
        "&password=" + URLEncoder.encode(password, "UTF-8") +
        "&action=" + URLEncoder.encode(action, "UTF-8")

    val connection = URL(endpoint).openConnection()
        as HttpURLConnection

    try {
        connection.connectTimeout = 15000
        connection.readTimeout = 20000

        if (connection.responseCode != 200) {
            throw Exception("Server returned HTTP ${connection.responseCode}")
        }

        val response = connection.inputStream
            .bufferedReader().use { it.readText() }

        return JSONArray(response)
    } finally {
        connection.disconnect()
    }
}
