package domain.interfaces

import domain.models.ArbitrageExecutionResult
import domain.models.ArbitrageOpportunity
/**
 * Executor Repository interface so that we can swap in different executor types on the fly.
 */
interface ArbitrageExecutionStrategy {
    suspend fun execute(opportunity: ArbitrageOpportunity): ArbitrageExecutionResult
}

