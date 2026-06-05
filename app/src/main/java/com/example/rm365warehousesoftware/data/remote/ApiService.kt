package com.example.rm365warehousesoftware.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit definition of the remote warehouse API.
 *
 * These endpoints are placeholders showing the expected patterns; adjust the
 * paths, request bodies and response types to match your backend contract.
 */
interface ApiService {

    /** Exchanges credentials for a JWT. Does not require the Authorization header. */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /** Looks up product details for a scanned barcode. */
    @GET("products/{barcode}")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): Response<ProductResponse>

    /** Uploads a queued/offline scan to the server. */
    @POST("scans")
    suspend fun uploadScan(@Body request: ScanUploadRequest): Response<Unit>

    /**
     * Uploads a batch of queued scans in a single request.
     *
     * The backend processes each scan independently and returns a per-item
     * result list, so a single bad scan does not fail the whole batch.
     *
     * Uses an absolute path so it resolves to the exact endpoint regardless of
     * the Retrofit base URL's path segment.
     */
    @POST("/api/v1/inventory/update/batch")
    suspend fun uploadScanBatch(@Body request: List<ScanUploadRequest>): Response<BatchScanResponse>
}

// ---- Request / response models (adjust to your API schema) ----

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

data class ProductResponse(
    val barcode: String,
    val name: String,
    val quantityOnHand: Int
)

data class ScanUploadRequest(
    /** Local Room row id, echoed back by the server to correlate results. */
    val clientId: Long,
    val barcode: String,
    val timestamp: Long
)

/** Response wrapper for the batch inventory update endpoint. */
data class BatchScanResponse(
    val results: List<BatchScanResult>
)

/** Per-scan outcome returned by the batch endpoint. */
data class BatchScanResult(
    /** Echo of [ScanUploadRequest.clientId] used to match the local row. */
    val clientId: Long,
    /** One of [STATUS_APPLIED], [STATUS_DUPLICATE] or [STATUS_ERROR]. */
    val status: String,
    /** Optional human-readable reason, populated for error results. */
    val message: String? = null
) {
    companion object {
        const val STATUS_APPLIED = "applied"
        const val STATUS_DUPLICATE = "duplicate"
        const val STATUS_ERROR = "error"
    }
}
