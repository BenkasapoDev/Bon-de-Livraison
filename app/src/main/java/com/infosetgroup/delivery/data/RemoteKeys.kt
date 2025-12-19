package com.infosetgroup.delivery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// RemoteKeys used by Paging RemoteMediator to store paging state for history cache
@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val repoId: String, // use a constant key e.g., "HISTORY" when storing global keys
    val prevKey: Int?,
    val nextKey: Int?
)

