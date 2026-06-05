package com.example.rm365warehousesoftware.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.rm365warehousesoftware.data.local.ScanDao
import com.example.rm365warehousesoftware.data.local.ScanEntity
import com.example.rm365warehousesoftware.data.remote.ApiService
import com.example.rm365warehousesoftware.data.remote.BatchScanResult
import com.example.rm365warehousesoftware.data.remote.ScanUploadRequest
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Coordinates barcode capture between the remote [ApiService] and the local
 * Room offline queue ([ScanDao]).
 *
 * Strategy:
 *  - When a scan arrives, try to update it online immediately.
 *  - If the device is offline (an [IOException] is thrown), queue the scan.
 *  - [syncPendingScans] flushes the queue in a single batch when back online.
 */
class ScanRepository(
    private val appContext: Context,
    private val apiService: ApiService,
    private val scanDao: ScanDao
) {

    /** All scans currently queued offline, observed for the UI. */
    val pendingScans: Flow<List<ScanEntity>> = scanDao.observeAll()

    /** Scans the server rejected, surfaced for the warehouse worker to review. */
    val failedScans: Flow<List<ScanEntity>> = scanDao.observeFailed()

    /**
     * Processes a freshly captured barcode.
     *
     * Attempts an online inventory update first; on a network failure the scan
     * is persisted to the offline queue for later sync.
     */
    suspend fun processScan(barcode: String): ScanResult {
        val timestamp = System.currentTimeMillis()
        // clientId 0 -> not yet persisted; the single-scan endpoint doesn't need it.
        val request = ScanUploadRequest(clientId = 0, barcode = barcode, timestamp = timestamp)
        return try {
            val response = apiService.uploadScan(request)
            if (response.isSuccessful) {
                ScanResult.Uploaded(barcode)
            } else {
                // The server was reachable but rejected the request. Queue it so
                // the scan is not lost, and surface the error to the caller.
                val id = scanDao.insert(ScanEntity(barcode = barcode, timestamp = timestamp))
                ScanResult.ServerError(barcode, response.code(), queuedId = id)
            }
        } catch (e: IOException) {
            // Offline / network failure -> queue locally.
            val id = scanDao.insert(ScanEntity(barcode = barcode, timestamp = timestamp))
            ScanResult.Queued(barcode, queuedId = id)
        }
    }

    /**
     * Uploads queued scans to the batch endpoint when connectivity is available.
     *
     * The backend returns a per-scan result list, so the batch never fails as a
     * whole. Scans reported as applied or duplicate are deleted locally; scans
     * reported as errors are flagged (kept in the queue with their reason) so a
     * warehouse worker can review them.
     */
    suspend fun syncPendingScans(): SyncResult {
        if (!isOnline()) {
            return SyncResult.Offline
        }

        // Only sync scans that haven't already been flagged as failed.
        val syncable = scanDao.getSyncable()
        if (syncable.isEmpty()) {
            return SyncResult.Success(uploaded = 0, failed = 0)
        }

        val batch = syncable.map {
            ScanUploadRequest(clientId = it.id, barcode = it.barcode, timestamp = it.timestamp)
        }

        return try {
            val response = apiService.uploadScanBatch(batch)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                // Transport-level failure: leave the whole queue intact for retry.
                return SyncResult.Failure(response.code(), remaining = syncable.size)
            }

            // Index results by clientId so we can match them to local rows even
            // if the server returns them out of order or omits some entries.
            val resultsByClientId = body.results.associateBy { it.clientId }

            val appliedIds = mutableListOf<Long>()
            var failedCount = 0

            for (scan in syncable) {
                when (val result = resultsByClientId[scan.id]) {
                    null -> {
                        // Server did not report this scan; leave it queued to retry.
                    }
                    else -> when (result.status) {
                        BatchScanResult.STATUS_APPLIED,
                        BatchScanResult.STATUS_DUPLICATE -> appliedIds.add(scan.id)

                        BatchScanResult.STATUS_ERROR -> {
                            failedCount++
                            scanDao.markError(
                                scan.id,
                                result.message ?: "Rejected by server"
                            )
                        }

                        else -> {
                            // Unknown status -> keep it queued rather than lose it.
                        }
                    }
                }
            }

            if (appliedIds.isNotEmpty()) {
                scanDao.deleteByIds(appliedIds)
            }

            SyncResult.Success(uploaded = appliedIds.size, failed = failedCount)
        } catch (e: IOException) {
            // Lost connectivity mid-sync; leave the queue intact for a retry.
            SyncResult.Offline
        }
    }

    /** Number of scans waiting to be synced. */
    suspend fun pendingCount(): Int = scanDao.count()

    /** Permanently removes a scan from the queue (e.g. a dismissed bad scan). */
    suspend fun dismissScan(id: Long) = scanDao.deleteById(id)

    /** Clears a scan's error flag so it is picked up by the next sync. */
    suspend fun retryScan(id: Long) = scanDao.resetError(id)

    /** Returns true when the device currently has a validated internet connection. */
    fun isOnline(): Boolean {
        val connectivityManager = appContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/** Outcome of processing a single scan. */
sealed interface ScanResult {
    /** Successfully sent to the server. */
    data class Uploaded(val barcode: String) : ScanResult

    /** Device offline; scan stored in the queue with the given row id. */
    data class Queued(val barcode: String, val queuedId: Long) : ScanResult

    /** Server reachable but returned an error; scan was queued as a fallback. */
    data class ServerError(val barcode: String, val code: Int, val queuedId: Long) : ScanResult
}

/** Outcome of a batch sync. */
sealed interface SyncResult {
    /**
     * The batch was processed. [uploaded] scans were applied/deduplicated and
     * removed locally; [failed] scans were flagged for review and kept.
     */
    data class Success(val uploaded: Int, val failed: Int) : SyncResult

    /** Transport-level failure; the queue was left untouched. */
    data class Failure(val code: Int, val remaining: Int) : SyncResult

    /** No connectivity; nothing was uploaded. */
    data object Offline : SyncResult
}
