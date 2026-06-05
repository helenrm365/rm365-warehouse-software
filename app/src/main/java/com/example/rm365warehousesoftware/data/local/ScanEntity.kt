package com.example.rm365warehousesoftware.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single barcode scan captured on the device.
 *
 * Rows are queued here while offline and removed once successfully synced
 * to the remote API.
 */
@Entity(tableName = "barcode_scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val barcode: String,

    /** Epoch milliseconds when the scan was captured. */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Reason the server rejected this scan during the last sync, or null if it
     * has never failed. Rows with a non-null value are flagged for review.
     */
    val syncError: String? = null,

    /** Number of sync attempts that returned an error for this scan. */
    val attempts: Int = 0
)
