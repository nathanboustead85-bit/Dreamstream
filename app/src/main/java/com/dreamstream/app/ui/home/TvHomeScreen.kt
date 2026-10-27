package com.dreamstream.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val TvBlue = Color(0xFF00C8FF)
private val TvPurple = Color(0xFF985BFF)

@Composable
fun TvHomeScreen(
    onLiveTv: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF070B18),
                        Color(0xFF152047),
                        Color(0xFF120D27)
                    )
                )
            )
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        Column(
            modifier = Modifier
                .width(245.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "DREAMSTREAM",
                    color = TvBlue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "ULTRA",
                    color = TvPurple,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "YOUR ENTERTAINMENT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Browse live channels, movies and series using your TV remote.",
                    color = Color(0xFFB6C6E7)
                )
            }
            Text(
                "UNIVERSAL EDITION  •  v1.8",
                color = Color(0xFF8593B8),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "WHAT WOULD YOU LIKE TO WATCH?",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(22.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TvTile(
                    "▶", "LIVE TV", "Channels & favourites",
                    Modifier.weight(1f), onLiveTv
                )
                TvTile(
                    "◆", "MOVIES", "Films on demand",
                    Modifier.weight(1f), onMovies
                )
                TvTile(
                    "▣", "SERIES", "Your favourite shows",
                    Modifier.weight(1f), onSeries
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Use ◀ ▶ to navigate and OK to select",
                color = Color(0xFFB6C6E7)
            )
        }
    }
}

@Composable
private fun TvTile(
    symbol: String,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var focused by remember {
        mutableStateOf(false)
    }
    val shape = RoundedCornerShape(22.dp)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(205.dp)
            .onFocusChanged {
                focused = it.hasFocus
            }
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) TvBlue
                else Color(0xFF384A73),
                shape
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused)
                Color(0xFF274C7C)
            else Color(0xFF18223C),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                symbol,
                color = TvBlue,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                color = Color(0xFFB6C6E7),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
