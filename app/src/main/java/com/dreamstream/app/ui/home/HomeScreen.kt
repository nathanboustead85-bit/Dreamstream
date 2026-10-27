package com.dreamstream.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dreamstream.app.ui.theme.*

private data class DashboardItem(
    val symbol: String,
    val title: String,
    val description: String,
    val enabled: Boolean,
    val action: () -> Unit
)

@Composable
fun HomeScreen(
    onLiveTv: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit
) {
    val items = listOf(
        DashboardItem(
            "▶", "LIVE TV", "Watch live channels",
            true, onLiveTv
        ),
        DashboardItem(
            "◆", "MOVIES", "Discover films",
            true, onMovies
        ),
        DashboardItem(
            "▣", "SERIES", "Explore episodes",
            true, onSeries
        ),
        DashboardItem(
            "★", "FAVOURITES", "Coming soon",
            false, {}
        ),
        DashboardItem(
            "⚙", "SETTINGS", "Coming soon",
            false, {}
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF111B39),
                        UltraBackground,
                        Color(0xFF150E2D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(
                    "DREAMSTREAM",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = UltraBlue
                )

                Text(
                    "ULTRA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = UltraPurple
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Your entertainment. Your universe.",
                    color = Color(0xFFB6C6E7)
                )
            }

            Text(
                "EXPLORE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items) { item ->
                    DashboardCard(item)
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(item: DashboardItem) {
    var focused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(20.dp)

    val borderColor = when {
        focused -> UltraBlue
        item.enabled -> Color(0xFF364769)
        else -> Color(0xFF252A3C)
    }

    Button(
        onClick = item.action,
        enabled = item.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .onFocusChanged {
                focused = it.isFocused
            }
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = borderColor,
                shape = shape
            ),
        shape = shape,
        contentPadding = PaddingValues(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) {
                Color(0xFF253A62)
            } else {
                UltraSurface
            },
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF121625),
            disabledContentColor = Color(0xFF67718A)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                item.symbol,
                style = MaterialTheme.typography.headlineLarge,
                color = if (focused) Color.White
                else UltraBlue
            )

            Spacer(Modifier.height(10.dp))

            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(5.dp))

            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
