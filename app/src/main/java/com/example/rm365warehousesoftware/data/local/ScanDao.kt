package com.example.rm365warehousesoftware.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    /** Queues a scan for later upload. Returns the generated row id. */
    @Insert
    suspend fun insert(scan: ScanEntity): Long

    /** Observes all queued scans, oldest first, for display in the UI. */
    @Query("SELECT * FROM barcode_scans ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ScanEntity>>

    /** Returns the pending scans to be synced (oldest first). */
    @Query("SELECT * FROM barcode_scans ORDER BY timestamp ASC")
    suspend fun getPending(): List<ScanEntity>

    /**
     * Scans eligible for upload: those that have never failed, oldest first.
     * Rows already flagged with a [ScanEntity.syncError] are held back for review.
     */
    @Query("SELECT * FROM barcode_scans WHERE syncError IS NULL ORDER BY timestamp ASC")
    suspend fun getSyncable(): List<ScanEntity>

    /** Scans the server rejected, surfaced for the warehouse worker to review. */
    @Query("SELECT * FROM barcode_scans WHERE syncError IS NOT NULL ORDER BY timestamp ASC")
    fun observeFailed(): Flow<List<ScanEntity>>

    /** Number of scans currently queued offline. */
    @Query("SELECT COUNT(*) FROM barcode_scans")
    suspend fun count(): Int

    /** Removes a scan once it has been successfully uploaded. */
    @Delete
    suspend fun delete(scan: ScanEntity)

    /** Bulk-removes scans that were successfully processed by the server. */
    @Query("DELETE FROM barcode_scans WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Flags a scan that the server rejected so it can be reviewed later. */
    @Query(
        "UPDATE barcode_scans SET syncError = :error, attempts = attempts + 1 WHERE id = :id"
    )
    suspend fun markError(id: Long, error: String?)

    /** Clears the error flag and attempt count so the scan becomes syncable again. */
    @Query("UPDATE barcode_scans SET syncError = NULL, attempts = 0 WHERE id = :id")
    suspend fun resetError(id: Long)

    /** Removes a single scan by its row id. */
    @Query("DELETE FROM barcode_scans WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Clears the entire queue. */
    @Query("DELETE FROM barcode_scans")
    suspend fun clear()
}
