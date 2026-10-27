package com.dreamstream.app.ui.live

import android.util.Base64
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

data class EpgProgramme(
    val title: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long
)

data class EpgResult(
    val programmes: List<EpgProgramme>,
    val source: String,
    val message: String = ""
)

private data class CachedEpg(
    val result: EpgResult,
    val savedAt: Long
)

object EpgRepository {

    private const val CACHE_DURATION = 30 * 60 * 1000L

    private val cache = ConcurrentHashMap<String, CachedEpg>()

    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")

    private fun credentials(
        username: String,
        password: String
    ): String =
        "username=${enc(username)}&password=${enc(password)}"

    suspend fun load(
        server: String,
        username: String,
        password: String,
        streamId: Int,
        epgChannelId: String,
        forceRefresh: Boolean = false
    ): EpgResult = withContext(Dispatchers.IO) {

        val cacheKey = listOf(
            server, username, password,
            streamId.toString(), epgChannelId
        ).joinToString("|")

        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            val existing = cache[cacheKey]
            if (existing != null &&
                now - existing.savedAt < CACHE_DURATION
            ) {
                return@withContext existing.result
            }
        }

        val shortResult = try {
            loadShortEpg(
                server, username, password, streamId
            )
        } catch (_: Exception) {
            emptyList()
        }

        val usableShort = chooseNowNext(shortResult)

        if (usableShort.isNotEmpty()) {
            val result = EpgResult(
                programmes = usableShort,
                source = "Xtream EPG"
            )
            cache[cacheKey] = CachedEpg(result, now)
            return@withContext result
        }

        val xmlResult = try {
            loadXmltv(
                server, username, password, epgChannelId
            )
        } catch (_: Exception) {
            emptyList()
        }

        val usableXml = chooseNowNext(xmlResult)

        val result = if (usableXml.isNotEmpty()) {
            EpgResult(
                programmes = usableXml,
                source = "XMLTV"
            )
        } else {
            EpgResult(
                programmes = emptyList(),
                source = "",
                message = if (epgChannelId.isBlank()) {
                    "No matching EPG channel ID was supplied. " +
                    "The provider may use a different mapping."
                } else {
                    "No current or upcoming programmes found. " +
                    "The provider may use a different EPG format " +
                    "or channel mapping."
                }
            )
        }

        cache[cacheKey] = CachedEpg(result, now)
        result
    }

    private fun loadShortEpg(
        server: String,
        username: String,
        password: String,
        streamId: Int
    ): List<EpgProgramme> {
        val endpoint = server.trimEnd('/') +
            "/player_api.php?" +
            credentials(username, password) +
            "&action=get_short_epg" +
            "&stream_id=$streamId&limit=10"

        val connection = openConnection(endpoint)

        try {
            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val listings = JSONObject(response)
                .optJSONArray("epg_listings")
                ?: return emptyList()

            val programmes = mutableListOf<EpgProgramme>()

            for (i in 0 until listings.length()) {
                val item = listings.optJSONObject(i)
                    ?: continue

                val start = item.optLong(
                    "start_timestamp", 0L
                ) * 1000L

                val end = item.optLong(
                    "stop_timestamp", 0L
                ) * 1000L

                val startTime = if (start > 0L) {
                    start
                } else {
                    parseXtreamDate(item.optString("start"))
                }

                val endTime = if (end > 0L) {
                    end
                } else {
                    parseXtreamDate(item.optString("end"))
                }

                if (startTime <= 0L || endTime <= startTime) {
                    continue
                }

                programmes.add(
                    EpgProgramme(
                        title = decodeText(
                            item.optString("title")
                        ),
                        description = decodeText(
                            item.optString("description")
                        ),
                        startMillis = startTime,
                        endMillis = endTime
                    )
                )
            }

            return programmes
        } finally {
            connection.disconnect()
        }
    }

    private fun loadXmltv(
        server: String,
        username: String,
        password: String,
        epgChannelId: String
    ): List<EpgProgramme> {
        if (epgChannelId.isBlank()) return emptyList()

        val endpoint = server.trimEnd('/') +
            "/xmltv.php?" +
            credentials(username, password)

        val connection = openConnection(endpoint)
        connection.setRequestProperty(
            "Accept-Encoding", "gzip"
        )

        try {
            val raw = connection.inputStream

            val input = if (
                connection.contentEncoding
                    ?.contains("gzip", ignoreCase = true) == true
            ) {
                GZIPInputStream(raw)
            } else {
                raw
            }

            input.use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")

                val programmes =
                    mutableListOf<EpgProgramme>()

                val wanted = epgChannelId
                    .trim().lowercase(Locale.ROOT)

                var matched = false
                var title = ""
                var description = ""
                var start = 0L
                var end = 0L

                val now = System.currentTimeMillis()
                val futureLimit = now + 24 * 60 * 60 * 1000L

                var event = parser.eventType

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "programme" -> {
                                    val channel = parser
                                        .getAttributeValue(
                                            null, "channel"
                                        )
                                        .orEmpty()
                                        .trim()
                                        .lowercase(Locale.ROOT)

                                    matched = channel == wanted

                                    title = ""
                                    description = ""

                                    if (matched) {
                                        start = parseXmltvDate(
                                            parser.getAttributeValue(
                                                null, "start"
                                            ).orEmpty()
                                        )

                                        end = parseXmltvDate(
                                            parser.getAttributeValue(
                                                null, "stop"
                                            ).orEmpty()
                                        )

                                        matched =
                                            end > now &&
                                            start < futureLimit
                                    }
                                }

                                "title" -> {
                                    if (matched) {
                                        title = parser.nextText()
                                    }
                                }

                                "desc" -> {
                                    if (matched) {
                                        description =
                                            parser.nextText()
                                    }
                                }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            if (parser.name == "programme") {
                                if (matched &&
                                    end > start &&
                                    title.isNotBlank()
                                ) {
                                    programmes.add(
                                        EpgProgramme(
                                            title = title,
                                            description = description,
                                            startMillis = start,
                                            endMillis = end
                                        )
                                    )
                                }
                                matched = false
                            }
                        }
                    }

                    event = parser.next()
                }

                return programmes
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        endpoint: String
    ): HttpURLConnection {
        val connection = URL(endpoint)
            .openConnection() as HttpURLConnection

        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.setRequestProperty(
            "User-Agent", "DreamStreamUltra/1.5"
        )

        if (connection.responseCode != 200) {
            val status = connection.responseCode
            connection.disconnect()
            throw Exception("EPG HTTP $status")
        }

        return connection
    }

    private fun decodeText(value: String): String {
        if (value.isBlank()) return ""

        return try {
            val decoded = String(
                Base64.decode(value, Base64.DEFAULT),
                Charsets.UTF_8
            )

            if (decoded.any {
                    it.isISOControl() &&
                    it != '\n' &&
                    it != '\t'
                }
            ) value else decoded
        } catch (_: Exception) {
            value
        }
    }

    private fun parseXtreamDate(
        value: String
    ): Long {
        if (value.isBlank()) return 0L

        return try {
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
            ).parse(value)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseXmltvDate(
        value: String
    ): Long {
        return try {
            val normalized = value.trim()
            val parts = normalized.split(
                Regex("\\s+")
            )

            val datePart = parts.firstOrNull()
                ?.take(14)
                ?: return 0L

            if (datePart.length != 14) return 0L

            val zone = parts.getOrNull(1) ?: "+0000"

            SimpleDateFormat(
                "yyyyMMddHHmmss Z",
                Locale.US
            ).apply {
                isLenient = false
            }.parse("$datePart $zone")?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun chooseNowNext(
        programmes: List<EpgProgramme>
    ): List<EpgProgramme> {
        val now = System.currentTimeMillis()

        return programmes
            .filter { it.endMillis > now }
            .sortedBy { it.startMillis }
            .take(2)
    }
}
