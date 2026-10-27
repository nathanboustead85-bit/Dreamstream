package com.dreamstream.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onLiveTv: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "DREAMSTREAM ULTRA",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Your entertainment dashboard")

        Button(
            onClick = onLiveTv,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LIVE TV")
        }

        Button(
            onClick = onMovies,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("MOVIES")
        }

        Button(
            onClick = onSeries,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SERIES")
        }

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("FAVOURITES - COMING SOON")
        }

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SETTINGS - COMING SOON")
        }
    }
}
