package com.dreamstream.app.ui.live

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private val Blue = Color(0xFF00C8FF)
private val Purple = Color(0xFF985BFF)
private val Background = Color(0xFF080B16)
private val Panel = Color(0xFF151B30)

data class LiveCategoryItem(
    val id: String,
    val name: String
)

data class LiveChannelItem(
    val id: Int,
    val name: String,
    val categoryId: String,
    val extension: String,
    val logo: String = "",
    val epgId: String = ""
)

private fun encode(value: String): String =
    URLEncoder.encode(value, "UTF-8")
        .replace("+", "%20")

private fun apiRequest(
    server: String,
    username: String,
    password: String,
    action: String
): JSONArray {
    val endpoint = server.trimEnd('/') +
        "/player_api.php?username=${encode(username)}" +
        "&password=${encode(password)}" +
        "&action=${encode(action)}"

    val connection = URL(endpoint)
        .openConnection() as HttpURLConnection

    try {
        connection.connectTimeout = 15000
        connection.readTimeout = 20000

        if (connection.responseCode != 200) {
            error("Server returned HTTP ${connection.responseCode}")
        }

        return JSONArray(
            connection.inputStream.bufferedReader()
                .use { it.readText() }
        )
    } finally {
        connection.disconnect()
    }
}

private fun channelUrl(
    server: String,
    username: String,
    password: String,
    channel: LiveChannelItem
): String {
    val extension =
        if (channel.extension.lowercase() == "m3u8") {
            "m3u8"
        } else {
            "ts"
        }

    return server.trimEnd('/') +
        "/live/${encode(username)}/${encode(password)}/" +
        "${channel.id}.$extension"
}

private fun favouriteKey(
    server: String,
    username: String
): String {
    val value = "$server|$username"
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))

    return digest.joinToString("") {
        "%02x".format(it.toInt() and 0xff)
    }
}

@Composable
fun LiveTvScreen(
    server: String,
    username: String,
    password: String,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(
            "dreamstream_favourites",
            Context.MODE_PRIVATE
        )
    }

    val key = remember(server, username) {
        favouriteKey(server, username)
    }

    var favourites by remember(key) {
        mutableStateOf(
            prefs.getStringSet(key, emptySet())
                ?.toSet() ?: emptySet()
        )
    }

    var categories by remember {
        mutableStateOf<List<LiveCategoryItem>>(emptyList())
    }
    var channels by remember {
        mutableStateOf<List<LiveChannelItem>>(emptyList())
    }
    var selectedCategory by remember {
        mutableStateOf("ALL")
    }
    var selectedChannel by remember {
        mutableStateOf<LiveChannelItem?>(null)
    }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf("") }

    fun toggleFavourite(channel: LiveChannelItem) {
        val id = channel.id.toString()

        val updated = if (id in favourites) {
            favourites - id
        } else {
            favourites + id
        }

        favourites = updated
        prefs.edit()
            .putStringSet(key, updated.toSet())
            .apply()
    }

    LaunchedEffect(server, username, password) {
        loading = true
        errorText = ""

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

                val categoryList =
                    mutableListOf<LiveCategoryItem>()
                val channelList =
                    mutableListOf<LiveChannelItem>()

                for (i in 0 until categoryData.length()) {
                    val obj = categoryData.optJSONObject(i)
                        ?: continue

                    categoryList.add(
                        LiveCategoryItem(
                            obj.optString("category_id"),
                            obj.optString(
                                "category_name", "Other"
                            )
                        )
                    )
                }

                for (i in 0 until channelData.length()) {
                    val obj = channelData.optJSONObject(i)
                        ?: continue

                    val id = obj.optInt("stream_id", -1)
                    if (id < 0) continue

                    channelList.add(
                        LiveChannelItem(
                            id = id,
                            name = obj.optString(
                                "name", "Unnamed channel"
                            ),
                            categoryId = obj.optString(
                                "category_id"
                            ),
                            extension = obj.optString(
                                "container_extension", "ts"
                            ),
                            logo = obj.optString("stream_icon"),
                            epgId = obj.optString(
                                "epg_channel_id"
                            )
                        )
                    )
                }

                Pair(categoryList, channelList)
            }

            categories = result.first
            channels = result.second
        } catch (e: Exception) {
            errorText = "Unable to load Live TV: " +
                (e.localizedMessage ?: "Unknown error")
        } finally {
            loading = false
        }
    }

    BackHandler(enabled = selectedChannel != null) {
        selectedChannel = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (selectedChannel != null) {
                        selectedChannel = null
                    } else {
                        onBack()
                    }
                }
            ) {
                Text("BACK")
            }

            Spacer(Modifier.width(12.dp))

            Text(
                "LIVE TV",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Blue
            )
        }

        Spacer(Modifier.height(12.dp))

        val selected = selectedChannel

        if (selected != null) {
            ChannelDetails(
                channel = selected,
                server = server,
                username = username,
                password = password,
                isFavourite =
                    selected.id.toString() in favourites,
                onFavourite = {
                    toggleFavourite(selected)
                },
                onPlay = {
                    onPlay(
                        channelUrl(
                            server, username, password,
                            selected
                        )
                    )
                }
            )
        } else {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search channels") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            val options = listOf(
                LiveCategoryItem("ALL", "All"),
                LiveCategoryItem("FAV", "★ Favourites")
            ) + categories

            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(options, key = { it.id }) { category ->
                    FilterChip(
                        selected =
                            selectedCategory == category.id,
                        onClick = {
                            selectedCategory = category.id
                        },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val filtered = remember(
                channels, selectedCategory,
                search, favourites
            ) {
                channels.filter { channel ->
                    val categoryMatch =
                        when (selectedCategory) {
                            "ALL" -> true
                            "FAV" ->
                                channel.id.toString() in favourites
                            else ->
                                channel.categoryId ==
                                    selectedCategory
                        }

                    categoryMatch &&
                        channel.name.contains(
                            search, ignoreCase = true
                        )
                }
            }

            Text(
                "${filtered.size} CHANNELS",
                color = Purple,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (errorText.isNotBlank()) {
                Text(
                    errorText,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp),
                    contentPadding =
                        PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        filtered,
                        key = { it.id }
                    ) { channel ->
                        ChannelRow(
                            channel = channel,
                            isFavourite =
                                channel.id.toString() in favourites,
                            onFavourite = {
                                toggleFavourite(channel)
                            },
                            onSelect = {
                                selectedChannel = channel
                            },
                            onPlay = {
                                onPlay(
                                    channelUrl(
                                        server, username,
                                        password, channel
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(
    url: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF263250)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "TV",
            color = Blue,
            fontWeight = FontWeight.Black
        )

        if (
            url.startsWith("https://") ||
            url.startsWith("http://")
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Channel logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: LiveChannelItem,
    isFavourite: Boolean,
    onFavourite: () -> Unit,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    var focused by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                focused = it.hasFocus
            }
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Blue
                else Color(0xFF293451),
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .clickable(onClick = onSelect)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        ChannelLogo(
            channel.logo,
            Modifier.size(56.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                channel.name,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "Live channel",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }

        TextButton(onClick = onFavourite) {
            Text(
                if (isFavourite) "★" else "☆",
                color = if (isFavourite) Purple else Blue,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Button(
            onClick = onPlay,
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        ) {
            Text("▶")
        }
    }
}

@Composable
private fun ChannelDetails(
    channel: LiveChannelItem,
    server: String,
    username: String,
    password: String,
    isFavourite: Boolean,
    onFavourite: () -> Unit,
    onPlay: () -> Unit
) {
    var epg by remember(channel.id) {
        mutableStateOf<EpgResult?>(null)
    }
    var loading by remember(channel.id) {
        mutableStateOf(true)
    }
    var refreshCount by remember(channel.id) {
        mutableIntStateOf(0)
    }

    LaunchedEffect(
        channel.id, server, username,
        password, refreshCount
    ) {
        loading = true

        try {
            epg = EpgRepository.load(
                server = server,
                username = username,
                password = password,
                streamId = channel.id,
                epgChannelId = channel.epgId,
                forceRefresh = refreshCount > 0
            )
        } catch (_: Exception) {
            epg = EpgResult(
                programmes = emptyList(),
                source = "",
                message = "Unable to retrieve programme guide."
            )
        } finally {
            loading = false
        }
    }

    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            ChannelLogo(
                channel.logo,
                Modifier.size(88.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "LIVE CHANNEL",
                    color = Blue
                )
            }
        }

        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶ WATCH LIVE")
        }

        OutlinedButton(
            onClick = onFavourite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isFavourite) {
                    "★ REMOVE FROM FAVOURITES"
                } else {
                    "☆ ADD TO FAVOURITES"
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NOW & NEXT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Blue,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = { refreshCount++ },
                enabled = !loading
            ) {
                Text("↻ REFRESH")
            }
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Checking programme guide...",
                color = Color.LightGray
            )
        } else {
            val result = epg

            if (result != null) {
                if (result.source.isNotBlank()) {
                    Text(
                        "Guide source: ${result.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Purple
                    )
                }

                if (result.programmes.isEmpty()) {
                    Text(
                        result.message.ifBlank {
                            "No programme information available."
                        },
                        color = Color.LightGray
                    )
                }

                val now = System.currentTimeMillis()

                result.programmes.forEach { programme ->
                    val isNow =
                        programme.startMillis <= now &&
                        programme.endMillis > now

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Panel)
                            .padding(14.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (isNow) "● NOW PLAYING"
                            else "UP NEXT",
                            color = if (isNow) Blue else Purple,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            programme.title.ifBlank {
                                "Untitled programme"
                            },
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            timeFormatter.format(
                                Date(programme.startMillis)
                            ) + " – " +
                                timeFormatter.format(
                                    Date(programme.endMillis)
                                ),
                            color = Color.LightGray
                        )

                        if (programme.description.isNotBlank()) {
                            Text(programme.description)
                        }
                    }
                }
            }
        }
    }
}
