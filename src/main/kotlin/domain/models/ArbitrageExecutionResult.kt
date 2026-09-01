package domain.models

/**
 * Result of an arbitrage execution.
 */
sealed interface ArbitrageExecutionResult {

    data class Success(
        val buyTxHash: String,
        val sellTxHash: String,
        val actualProfitUsd: Double?
    ) : ArbitrageExecutionResult

    data class Failed(
        val reason: String,
        val buyTxHash: String? = null
    ) : ArbitrageExecutionResult

    data object DryRun : ArbitrageExecutionResult
}
