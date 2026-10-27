package com.dreamstream.app.ui.vod

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class VodCategory(
    val id: String,
    val name: String
)

private data class VodItem(
    val id: Int,
    val name: String,
    val categoryId: String,
    val extension: String,
    val description: String
)

private data class EpisodeItem(
    val id: String,
    val title: String,
    val season: Int,
    val number: Int,
    val extension: String
)

private fun encode(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private fun xtreamRequest(
    server: String,
    username: String,
    password: String,
    action: String,
    extra: String = ""
): String {
    val endpoint = server.trimEnd('/') +
        "/player_api.php?username=${encode(username)}" +
        "&password=${encode(password)}" +
        "&action=${encode(action)}" + extra

    val connection = URL(endpoint).openConnection()
        as HttpURLConnection

    try {
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        if (connection.responseCode != 200) {
            throw Exception(
                "Server returned HTTP ${connection.responseCode}"
            )
        }

        return connection.inputStream.bufferedReader()
            .use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun streamUrl(
    server: String,
    username: String,
    password: String,
    type: String,
    id: String,
    extension: String
): String {
    val safeExtension = extension.lowercase()
        .takeIf {
            it in listOf("mp4", "mkv", "avi", "ts", "m3u8")
        } ?: "mp4"

    return server.trimEnd('/') +
        "/$type/${encode(username)}/${encode(password)}/" +
        "${encode(id)}.$safeExtension"
}

@Composable
fun MoviesScreen(
    server: String,
    username: String,
    password: String,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    VodBrowser(
        server = server,
        username = username,
        password = password,
        isSeries = false,
        onPlay = onPlay,
        onBack = onBack
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
    VodBrowser(
        server = server,
        username = username,
        password = password,
        isSeries = true,
        onPlay = onPlay,
        onBack = onBack
    )
}

@Composable
private fun VodBrowser(
    server: String,
    username: String,
    password: String,
    isSeries: Boolean,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    var categories by remember {
        mutableStateOf<List<VodCategory>>(emptyList())
    }
    var entries by remember {
        mutableStateOf<List<VodItem>>(emptyList())
    }
    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }
    var selectedItem by remember {
        mutableStateOf<VodItem?>(null)
    }
    var episodes by remember {
        mutableStateOf<List<EpisodeItem>>(emptyList())
    }
    var selectedSeason by remember {
        mutableIntStateOf(-1)
    }
    var details by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var detailLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(server, username, password, isSeries) {
        loading = true
        error = ""
        selectedCategory = null
        selectedItem = null
        episodes = emptyList()

        try {
            val data = withContext(Dispatchers.IO) {
                val categoryAction = if (isSeries) {
                    "get_series_categories"
                } else {
                    "get_vod_categories"
                }
                val listAction = if (isSeries) {
                    "get_series"
                } else {
                    "get_vod_streams"
                }

                val categoryJson = JSONArray(
                    xtreamRequest(
                        server, username, password,
                        categoryAction
                    )
                )
                val itemJson = JSONArray(
                    xtreamRequest(
                        server, username, password,
                        listAction
                    )
                )

                val parsedCategories = mutableListOf<VodCategory>()
                val parsedItems = mutableListOf<VodItem>()

                for (i in 0 until categoryJson.length()) {
                    val obj = categoryJson.optJSONObject(i)
                        ?: continue

                    parsedCategories.add(
                        VodCategory(
                            id = obj.optString("category_id"),
                            name = obj.optString(
                                "category_name", "Other"
                            )
                        )
                    )
                }

                for (i in 0 until itemJson.length()) {
                    val obj = itemJson.optJSONObject(i)
                        ?: continue

                    val id = obj.optInt(
                        if (isSeries) "series_id" else "stream_id",
                        -1
                    )
                    if (id < 0) continue

                    parsedItems.add(
                        VodItem(
                            id = id,
                            name = obj.optString(
                                "name", "Untitled"
                            ),
                            categoryId = obj.optString(
                                "category_id"
                            ),
                            extension = obj.optString(
                                "container_extension", "mp4"
                            ),
                            description = obj.optString(
                                "plot",
                                obj.optString("description", "")
                            )
                        )
                    )
                }

                Pair(parsedCategories, parsedItems)
            }

            categories = data.first
            entries = data.second
        } catch (e: Exception) {
            error = "Unable to load content: " +
                (e.localizedMessage ?: "Unknown error")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selectedItem, isSeries) {
        val item = selectedItem ?: return@LaunchedEffect

        details = item.description
        episodes = emptyList()
        selectedSeason = -1
        detailLoading = true
        error = ""

        try {
            val result = withContext(Dispatchers.IO) {
                val action = if (isSeries) {
                    "get_series_info"
                } else {
                    "get_vod_info"
                }

                JSONObject(
                    xtreamRequest(
                        server, username, password,
                        action,
                        if (isSeries) {
                            "&series_id=${item.id}"
                        } else {
                            "&vod_id=${item.id}"
                        }
                    )
                )
            }

            val info = result.optJSONObject("info")
            details = info?.optString(
                "plot",
                item.description
            ) ?: item.description

            if (isSeries) {
                val episodeObject = result.optJSONObject("episodes")
                val found = mutableListOf<EpisodeItem>()

                if (episodeObject != null) {
                    val seasonKeys = episodeObject.keys()

                    while (seasonKeys.hasNext()) {
                        val key = seasonKeys.next()
                        val seasonNumber = key.toIntOrNull() ?: 0
                        val array = episodeObject.optJSONArray(key)
                            ?: continue

                        for (i in 0 until array.length()) {
                            val ep = array.optJSONObject(i)
                                ?: continue
                            val episodeId = ep.optString("id")

                            if (episodeId.isBlank()) continue

                            found.add(
                                EpisodeItem(
                                    id = episodeId,
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
                    compareBy<EpisodeItem> { it.season }
                        .thenBy { it.number }
                )

                selectedSeason = episodes.firstOrNull()?.season ?: -1
            }
        } catch (e: Exception) {
            error = "Unable to load details: " +
                (e.localizedMessage ?: "Unknown error")
        } finally {
            detailLoading = false
        }
    }

    fun goBack() {
        when {
            selectedItem != null -> {
                selectedItem = null
                error = ""
            }
            selectedCategory != null -> {
                selectedCategory = null
                query = ""
            }
            else -> onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { goBack() }) {
            Text("BACK")
        }

        Text(
            when {
                selectedItem != null -> selectedItem!!.name
                isSeries -> "TV SERIES"
                else -> "MOVIES"
            },
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        if (loading || detailLoading) {
            CircularProgressIndicator()
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!loading && selectedItem == null) {
            if (selectedCategory == null) {
                LazyColumn {
                    item {
                        ListEntry("ALL CONTENT (${entries.size})") {
                            selectedCategory = "ALL"
                        }
                    }

                    items(categories) { category ->
                        ListEntry(category.name) {
                            selectedCategory = category.id
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search titles") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val filtered = entries.filter {
                    (selectedCategory == "ALL" ||
                        it.categoryId == selectedCategory) &&
                        it.name.contains(query, ignoreCase = true)
                }

                Text("${filtered.size} titles")

                LazyColumn {
                    items(filtered) { item ->
                        ListEntry(item.name) {
                            selectedItem = item
                        }
                    }
                }
            }
        }

        val item = selectedItem

        if (item != null && !detailLoading) {
            if (details.isNotBlank()) {
                Text(details)
                Spacer(Modifier.height(12.dp))
            }

            if (!isSeries) {
                Button(
                    onClick = {
                        onPlay(
                            streamUrl(
                                server, username, password,
                                "movie",
                                item.id.toString(),
                                item.extension
                            )
                        )
                    }
                ) {
                    Text("PLAY MOVIE")
                }
            } else {
                if (episodes.isEmpty()) {
                    Text("No episodes available for this series.")
                } else {
                    val seasons = episodes.map { it.season }.distinct()

                    Text("SELECT SEASON")

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 140.dp)
                    ) {
                        items(seasons) { season ->
                            ListEntry(
                                "Season $season" +
                                    if (selectedSeason == season) " ✓"
                                    else ""
                            ) {
                                selectedSeason = season
                            }
                        }
                    }

                    Text("EPISODES")

                    LazyColumn {
                        items(
                            episodes.filter {
                                it.season == selectedSeason
                            }
                        ) { episode ->
                            ListEntry(
                                "${episode.number}. ${episode.title}"
                            ) {
                                onPlay(
                                    streamUrl(
                                        server,
                                        username,
                                        password,
                                        "series",
                                        episode.id,
                                        episode.extension
                                    )
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
private fun ListEntry(
    title: String,
    onClick: () -> Unit
) {
    Column {
        Text(
            title,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 15.dp)
        )
        HorizontalDivider()
    }
}
