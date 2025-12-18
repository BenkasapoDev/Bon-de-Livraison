package com.infosetgroup.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.infosetgroup.delivery.DeliveryColors

@Composable
fun SyncBottomBar(pendingCount: Int, onOpenPending: () -> Unit, onSync: () -> Unit, syncing: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val pillText = if (pendingCount > 0) "$pendingCount en attente" else "Aucun en attente"
            Text(
                text = pillText,
                modifier = Modifier.clickable { onOpenPending() },
                color = DeliveryColors.TextPrimary
            )

            Button(
                onClick = onSync,
                enabled = !syncing,
                colors = ButtonDefaults.buttonColors(containerColor = DeliveryColors.PrimaryCorral)
            ) {
                val label = if (syncing) "Synchronisation..." else "Sync"
                Text(text = label, color = Color.White)
            }
        }
    }
}

