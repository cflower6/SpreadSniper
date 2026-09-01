package application.executor

import domain.interfaces.ArbitrageExecutionStrategy
import domain.interfaces.ExecutionIdempotencyStore
import domain.models.ArbitrageExecutionResult
import domain.models.ArbitrageOpportunity

class ArbitrageExecutor(
    private val executionEvaluator: ExecutionEvaluator,
    private val executionStrategy: ArbitrageExecutionStrategy,
    private val idempotencyStore: ExecutionIdempotencyStore
) {

    suspend fun execute(
        opportunity: ArbitrageOpportunity
    ): ArbitrageExecutionResult {

        if (!executionEvaluator.approve(opportunity)) {
            return ArbitrageExecutionResult.Failed(
                reason = "Opportunity rejected by execution policy"
            )
        }

        val claimed =
            idempotencyStore.tryClaim(
                opportunity.opportunityKey
            )

        if (!claimed) {
            return ArbitrageExecutionResult.Failed(
                reason = "Opportunity already claimed"
            )
        }

        return executionStrategy.execute(
            opportunity
        )
    }
}