package application.executor

import configurations.AppConfig
import domain.models.ArbitrageOpportunity
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class ExecutionEvaluator {

    @OptIn(ExperimentalTime::class)
    fun approve(
        opportunity: ArbitrageOpportunity
    ): Boolean {

        if (!AppConfig.executionEnabled) {
            return false
        }

        if (
            opportunity.estimatedNetProfitUsd <
            AppConfig.minProfitForExecution
        ) {
            return false
        }

        if (
            opportunity.estimatedGasUsd >
            opportunity.grossProfitUsd
        ) {
            return false
        }

        val age =
            Clock.System.now() -
                    opportunity.observedAt

        if (age > 5.seconds) {
            return false
        }

        return true
    }
}