package utils

import models.ArbitrageOpportunity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun messageBuilder(opportunity: ArbitrageOpportunity): String {
    val msg = """
                        🚨 ARBITRAGE DETECTED 🚨

                        Buy: ${opportunity.buyDex}
                        Sell: ${opportunity.sellDex}

                        TokenIn: ${opportunity.tokenIn}
                        TokenOut: ${opportunity.tokenOut}

                        Net Profit: ${opportunity.estimatedNetProfitUsd}

                        Spread: ${opportunity.grossSpreadBps}
                        Time: ${Clock.System.now()}
                        """.trimIndent()
    return msg
}