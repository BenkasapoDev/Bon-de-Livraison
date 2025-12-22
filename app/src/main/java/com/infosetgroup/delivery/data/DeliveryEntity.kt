package com.infosetgroup.delivery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: String,
    val serialNumber: String,
    val sim: String,
    val merchant: String,
    val shop: String,
    val receiver: String,
    val deliveryAgent: String,
    // store local file path to the saved image instead of Base64 in DB
    val receiverProofPath: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    // server code (optional) - keep nullable for backward compatibility
    val code: String? = "",
    // local status for retries/queue
    val status: String? = "PENDING"
)
