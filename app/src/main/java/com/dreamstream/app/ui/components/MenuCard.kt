package com.dreamstream.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamstream.app.ui.theme.TeslaBorder
import com.dreamstream.app.ui.theme.TeslaCard
import com.dreamstream.app.ui.theme.TeslaWhite

@Composable
fun MenuCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() },

        colors = CardDefaults.cardColors(
            containerColor = TeslaCard
        ),

        border = BorderStroke(1.dp, TeslaBorder),

        shape = RoundedCornerShape(24.dp)
    ) {
        Text(
            text = title,
            color = TeslaWhite,
            modifier = Modifier.padding(24.dp)
        )
    }
}
