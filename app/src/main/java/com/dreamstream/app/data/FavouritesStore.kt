package com.dreamstream.app.data

import android.content.Context
import java.security.MessageDigest

object FavouritesStore {

    private const val PREFS = "dreamstream_favourites"

    private fun account(
        server: String,
        username: String
    ): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(
                "$server|$username"
                    .toByteArray(Charsets.UTF_8)
            )

        return bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun key(
        server: String,
        username: String,
        type: String
    ): String {
        val base = account(server, username)

        return if (type == "live") {
            base
        } else {
            "${base}_$type"
        }
    }

    fun get(
        context: Context,
        server: String,
        username: String,
        type: String
    ): Set<String> {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getStringSet(
            key(server, username, type),
            emptySet()
        )?.toSet() ?: emptySet()
    }

    fun toggle(
        context: Context,
        server: String,
        username: String,
        type: String,
        id: String
    ): Set<String> {
        val current = get(
            context, server, username, type
        )

        val updated = if (id in current) {
            current - id
        } else {
            current + id
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putStringSet(
                key(server, username, type),
                updated.toSet()
            )
            .apply()

        return updated
    }

    fun clear(
        context: Context,
        server: String,
        username: String
    ) {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .remove(key(server, username, "live"))
            .remove(key(server, username, "movie"))
            .remove(key(server, username, "series"))
            .apply()
    }
}

