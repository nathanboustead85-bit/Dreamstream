package com.dreamstream.app.ui.favourites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FavouritesScreen(
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF111B39),
                        Color(0xFF080B16),
                        Color(0xFF150E2D)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("BACK")
        }

        Text(
            "★ FAVOURITES",
            color = Color(0xFF00C8FF),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Text(
            "Choose a collection to view your saved favourites.",
            color = Color(0xFFB6C6E7)
        )

        FavouriteDestination(
            "▶",
            "LIVE TV",
            "Your saved channels",
            onLive
        )

        FavouriteDestination(
            "◆",
            "MOVIES",
            "Your saved films",
            onMovies
        )

        FavouriteDestination(
            "▣",
            "SERIES",
            "Your saved shows",
            onSeries
        )
    }
}

@Composable
private fun FavouriteDestination(
    symbol: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF192641)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                symbol,
                color = Color(0xFF00C8FF),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.width(20.dp))

            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    color = Color(0xFFB6C6E7),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

