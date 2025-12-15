package com.infosetgroup.delivery.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.repository.DeliveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PendingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val list = remember { mutableStateListOf<DeliveryEntity>() }
    val isLoading = remember { mutableStateOf(true) }
    val syncing = remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = DeliveryRepository.getInstance(context)

    LaunchedEffect(Unit) {
        // load pending
        isLoading.value = true
        withContext(Dispatchers.IO) {
            val dao = com.infosetgroup.delivery.data.AppDatabase.getInstance(context).deliveryDao()
            val pendingList = mutableListOf<DeliveryEntity>()
            dao.getAllPending().collect { pendingList.addAll(it) } // Collect the flow data into a list
            list.clear()
            list.addAll(pendingList) // Add the collected list to the state list
            isLoading.value = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        if (isLoading.value) {
            CircularProgressIndicator()
            return@Column
        }

        // Back + Sync row
        Button(onClick = { onBack() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "← Retour")
        }

        Button(onClick = {
            syncing.value = true
            CoroutineScope(Dispatchers.IO).launch {
                val res = repo.syncPending()
                // reload list
                val pendingList = mutableListOf<DeliveryEntity>()
                com.infosetgroup.delivery.data.AppDatabase.getInstance(context).deliveryDao().getAllPending().collect { pendingList.addAll(it) } // Collect the flow data into a list
                list.clear()
                list.addAll(pendingList) // Add the collected list to the state list
                syncing.value = false
                withContext(Dispatchers.Main) {
                    when (res) {
                        is com.infosetgroup.delivery.repository.SyncResult.Success -> Toast.makeText(context, "Synchronisé ${res.syncedCount}", Toast.LENGTH_SHORT).show()
                        is com.infosetgroup.delivery.repository.SyncResult.NothingToSync -> Toast.makeText(context, "Rien à synchroniser", Toast.LENGTH_SHORT).show()
                        is com.infosetgroup.delivery.repository.SyncResult.Failure -> Toast.makeText(context, "Échec: ${res.error}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            val label = if (syncing.value) "Synchronisation..." else "Sync pending"
            Text(text = label)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(list) { item ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = item.item)
                        Text(text = "N° série: ${item.serialNumber}")
                        Text(text = "Magasin: ${item.shop}")
                    }
                }
            }
        }
    }
}
