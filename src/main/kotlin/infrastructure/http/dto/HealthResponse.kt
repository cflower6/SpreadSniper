package infrastructure.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val scanner: String,
    val redis: String,
    val markets: Int,
    val executionEnabled: Boolean,
    val lastProcessedBlock: Long?,
    val lastScanDurationMs: Long?
)