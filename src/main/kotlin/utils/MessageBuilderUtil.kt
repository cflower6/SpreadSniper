package utils

import domain.models.ArbitrageOpportunity
import infrastructure.blockchain.Web3Utils
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun messageBuilder(
    opportunity: ArbitrageOpportunity
): String {

    val firstLeg = opportunity.firstLeg
    val secondLeg = opportunity.secondLeg

    val firstAmountIn =
        Web3Utils()
            .toHuman(
                firstLeg.amountInRaw,
                firstLeg.tokenIn
            )

    val firstAmountOut =
        Web3Utils()
            .toHuman(
                firstLeg.amountOutRaw,
                firstLeg.tokenOut
            )

    val secondAmountOut =
        Web3Utils()
            .toHuman(
                secondLeg.amountOutRaw,
                secondLeg.tokenOut
            )

    return """
        🚨 ARBITRAGE DETECTED 🚨

        Chain: ${opportunity.chain}
        Pair: ${opportunity.pair.label}

        Route:
        ${firstLeg.dex}: ${firstLeg.tokenIn.symbol} → ${firstLeg.tokenOut.symbol}
        ${secondLeg.dex}: ${secondLeg.tokenIn.symbol} → ${secondLeg.tokenOut.symbol}

        Trade:
        $firstAmountIn ${firstLeg.tokenIn.symbol}
        → $firstAmountOut ${firstLeg.tokenOut.symbol}
        → $secondAmountOut ${secondLeg.tokenOut.symbol}

        Gross Profit: $${"%.4f".format(opportunity.grossProfitUsd)}
        Est. Fees: $${"%.4f".format(opportunity.estimatedFeesUsd)}
        Est. Gas: $${"%.4f".format(opportunity.estimatedGasUsd)}
        Net Profit: $${"%.4f".format(opportunity.estimatedNetProfitUsd)}

        Spread: ${"%.2f".format(opportunity.grossSpreadBps)} bps

        Block: ${opportunity.blockNumber}
        Observed: ${opportunity.observedAt}

        Opportunity: ${opportunity.opportunityKey.value.take(12)}...
    """.trimIndent()
}