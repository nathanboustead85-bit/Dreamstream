package com.dreamstream.app.ui.vod

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val NeonBlue = Color(0xFF00C8FF)
private val NeonPurple = Color(0xFF985BFF)
private val DarkSurface = Color(0xFF151B30)

private data class Category(
    val id: String,
    val name: String
)

private data class MediaItem(
    val id: Int,
    val name: String,
    val categoryId: String,
    val extension: String,
    val description: String,
    val poster: String
)

private data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val number: Int,
    val extension: String
)

private fun enc(value: String): String =
    URLEncoder.encode(value, "UTF-8")
        .replace("+", "%20")

private fun api(
    server: String,
    username: String,
    password: String,
    action: String,
    extra: String = ""
): String {
    val endpoint = server.trimEnd('/') +
        "/player_api.php?username=${enc(username)}" +
        "&password=${enc(password)}" +
        "&action=${enc(action)}$extra"

    val connection = URL(endpoint)
        .openConnection() as HttpURLConnection

    try {
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        if (connection.responseCode != 200) {
            error("Server returned HTTP ${connection.responseCode}")
        }

        return connection.inputStream.bufferedReader()
            .use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun playbackUrl(
    server: String,
    username: String,
    password: String,
    type: String,
    id: String,
    extension: String
): String {
    val ext = extension.lowercase()
        .takeIf { it in listOf("mp4", "mkv", "avi", "ts", "m3u8") }
        ?: "mp4"

    return server.trimEnd('/') +
        "/$type/${enc(username)}/${enc(password)}/" +
        "${enc(id)}.$ext"
}

@Composable
fun MoviesScreen(
    server: String,
    username: String,
    password: String,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    MediaBrowser(
        server, username, password,
        false, onPlay, onBack
    )
}

@Composable
fun SeriesScreen(
    server: String,
    username: String,
    password: String,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    MediaBrowser(
        server, username, password,
        true, onPlay, onBack
    )
}

@Composable
private fun MediaBrowser(
    server: String,
    username: String,
    password: String,
    isSeries: Boolean,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    var categories by remember {
        mutableStateOf<List<Category>>(emptyList())
    }
    var media by remember {
        mutableStateOf<List<MediaItem>>(emptyList())
    }
    var category by remember { mutableStateOf("ALL") }
    var selected by remember {
        mutableStateOf<MediaItem?>(null)
    }
    var episodes by remember {
        mutableStateOf<List<Episode>>(emptyList())
    }
    var season by remember { mutableIntStateOf(-1) }
    var description by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var detailLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    LaunchedEffect(server, username, password, isSeries) {
        loading = true
        errorText = ""

        try {
            val result = withContext(Dispatchers.IO) {
                val categoryAction =
                    if (isSeries) "get_series_categories"
                    else "get_vod_categories"

                val listAction =
                    if (isSeries) "get_series"
                    else "get_vod_streams"

                val categoryJson = JSONArray(
                    api(server, username, password, categoryAction)
                )
                val mediaJson = JSONArray(
                    api(server, username, password, listAction)
                )

                val parsedCategories = mutableListOf<Category>()
                val parsedMedia = mutableListOf<MediaItem>()

                for (i in 0 until categoryJson.length()) {
                    val obj = categoryJson.optJSONObject(i)
                        ?: continue

                    parsedCategories.add(
                        Category(
                            obj.optString("category_id"),
                            obj.optString("category_name", "Other")
                        )
                    )
                }

                for (i in 0 until mediaJson.length()) {
                    val obj = mediaJson.optJSONObject(i)
                        ?: continue

                    val id = obj.optInt(
                        if (isSeries) "series_id" else "stream_id",
                        -1
                    )
                    if (id < 0) continue

                    parsedMedia.add(
                        MediaItem(
                            id = id,
                            name = obj.optString("name", "Untitled"),
                            categoryId = obj.optString("category_id"),
                            extension = obj.optString(
                                "container_extension", "mp4"
                            ),
                            description = obj.optString(
                                "plot",
                                obj.optString("description", "")
                            ),
                            poster = if (isSeries) {
                                obj.optString("cover")
                            } else {
                                obj.optString("stream_icon")
                            }
                        )
                    )
                }

                Pair(parsedCategories, parsedMedia)
            }

            categories = result.first
            media = result.second
        } catch (e: Exception) {
            errorText = "Unable to load library: " +
                (e.localizedMessage ?: "Unknown error")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selected, isSeries) {
        val item = selected ?: return@LaunchedEffect

        description = item.description
        episodes = emptyList()
        season = -1
        detailLoading = true
        errorText = ""

        try {
            val info = withContext(Dispatchers.IO) {
                JSONObject(
                    api(
                        server,
                        username,
                        password,
                        if (isSeries) "get_series_info"
                        else "get_vod_info",
                        if (isSeries) "&series_id=${item.id}"
                        else "&vod_id=${item.id}"
                    )
                )
            }

            description = info.optJSONObject("info")
                ?.optString("plot", item.description)
                ?: item.description

            if (isSeries) {
                val found = mutableListOf<Episode>()
                val episodeGroups = info.optJSONObject("episodes")

                if (episodeGroups != null) {
                    val keys = episodeGroups.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        val seasonNumber = key.toIntOrNull() ?: 0
                        val array = episodeGroups.optJSONArray(key)
                            ?: continue

                        for (i in 0 until array.length()) {
                            val ep = array.optJSONObject(i)
                                ?: continue
                            val id = ep.optString("id")

                            if (id.isBlank()) continue

                            found.add(
                                Episode(
                                    id = id,
                                    title = ep.optString(
                                        "title", "Episode ${i + 1}"
                                    ),
                                    season = seasonNumber,
                                    number = ep.optInt(
                                        "episode_num", i + 1
                                    ),
                                    extension = ep.optString(
                                        "container_extension", "mp4"
                                    )
                                )
                            )
                        }
                    }
                }

                episodes = found.sortedWith(
                    compareBy<Episode> { it.season }
                        .thenBy { it.number }
                )
                season = episodes.firstOrNull()?.season ?: -1
            }
        } catch (e: Exception) {
            errorText = "Unable to load details: " +
                (e.localizedMessage ?: "Unknown error")
        } finally {
            detailLoading = false
        }
    }

    fun goBack() {
        if (selected != null) {
            selected = null
            errorText = ""
        } else {
            onBack()
        }
    }

    BackHandler(enabled = selected != null) {
        goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B16))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { goBack() }) {
                Text("BACK")
            }

            Spacer(Modifier.width(12.dp))

            Text(
                if (isSeries) "SERIES" else "MOVIES",
                style = MaterialTheme.typography.headlineSmall,
                color = NeonBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        if (loading || detailLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorText.isNotBlank()) {
            Text(
                errorText,
                color = MaterialTheme.colorScheme.error
            )
        }

        val current = selected

        if (current == null) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search titles") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            val options = listOf(Category("ALL", "All")) +
                categories

            LazyRowCategories(
                options = options,
                selectedId = category,
                onSelect = { category = it }
            )

            Spacer(Modifier.height(12.dp))

            val filtered = remember(media, category, search) {
                media.filter {
                    (category == "ALL" ||
                        it.categoryId == category) &&
                        it.name.contains(search, ignoreCase = true)
                }
            }

            Text(
                "${filtered.size} titles",
                color = Color.LightGray
            )

            Spacer(Modifier.height(10.dp))

            if (!loading) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(135.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        count = filtered.size,
                        key = { filtered[it].id }
                    ) { index ->
                        PosterCard(filtered[index]) {
                            selected = filtered[index]
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PosterArtwork(
                        url = current.poster,
                        title = current.name,
                        modifier = Modifier
                            .width(125.dp)
                            .height(185.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            current.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (!isSeries) {
                            Button(
                                onClick = {
                                    onPlay(
                                        playbackUrl(
                                            server, username, password,
                                            "movie",
                                            current.id.toString(),
                                            current.extension
                                        )
                                    )
                                }
                            ) {
                                Text("▶ PLAY MOVIE")
                            }
                        } else {
                            Text(
                                "${episodes.size} episodes",
                                color = NeonPurple
                            )
                        }
                    }
                }

                if (description.isNotBlank()) {
                    Text(
                        description,
                        color = Color(0xFFCED6E8)
                    )
                }

                if (isSeries && !detailLoading) {
                    if (episodes.isEmpty()) {
                        Text("No episodes available.")
                    } else {
                        Text(
                            "SEASONS",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )

                        val seasons = episodes.map {
                            it.season
                        }.distinct()

                        LazyRowCategories(
                            options = seasons.map {
                                Category(it.toString(), "Season $it")
                            },
                            selectedId = season.toString(),
                            onSelect = {
                                season = it.toIntOrNull() ?: season
                            }
                        )

                        Text(
                            "EPISODES",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )

                        episodes.filter {
                            it.season == season
                        }.forEach { episode ->
                            OutlinedButton(
                                onClick = {
                                    onPlay(
                                        playbackUrl(
                                            server, username, password,
                                            "series",
                                            episode.id,
                                            episode.extension
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${episode.number}. ${episode.title}",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyRowCategories(
    options: List<Category>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options.size) { index ->
            val option = options[index]

            FilterChip(
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
                label = { Text(option.name) }
            )
        }
    }
}

@Composable
private fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) NeonBlue else Color(0xFF28334C),
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(DarkSurface)
            .padding(7.dp)
    ) {
        PosterArtwork(
            url = item.poster,
            title = item.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            item.name,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PosterArtwork(
    url: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF263250)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "◆",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonPurple
        )

        if (url.startsWith("http://") ||
            url.startsWith("https://")
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Poster for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
