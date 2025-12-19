package com.infosetgroup.delivery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_deliveries")
data class HistoryEntity(
    @PrimaryKey val id: String, // use server-provided unique id or code
    val item: String,
    val serialNumber: String,
    val sim: String,
    val merchant: String,
    val shop: String,
    val receiver: String,
    val deliveryAgent: String,
    val code: String,
    val receiverProofPath: String?,
    val rowOrder: Long // ordering field to preserve server order
)

