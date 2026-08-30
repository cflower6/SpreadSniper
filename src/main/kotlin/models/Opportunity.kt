package models

import java.math.BigInteger
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Dex pair - Decent. Exchanges
 * Buy Price - Price of crypto on the Dex
 * Sell Price - Price of crypto on the other Dex
 * Spread - (Sell Price - Buy Price)
 * AdjustedProfit - Spread with fees and gas subtracted
 */
data class Opportunity(
    val pair: DexPair,
    val buyPrice: Double,
    val sellPrice: Double,
    val spread: Double,
    val adjustedProfit: Double, // after DEX fees + gas
    val chain: Chain? = null,
)

data class ArbitrageOpportunity @OptIn(ExperimentalTime::class) constructor(
    val id: UUID,
    val chain: String,
    val tokenIn: String,
    val tokenOut: String,
    val buyDex: String,
    val sellDex: String,

    val amountIn: Double,

    val buyQuote: Double,
    val sellQuote: Double,

    val grossProfitUsd: Double,
    val estimatedGasUsd: Double,
    val estimatedFeesUsd: Double,
    val estimatedNetProfitUsd: Double,

    val grossSpreadBps: Double,

    val blockNumber: BigInteger,
    val observedAt: Instant
)


fun findBestOpportunity(
    opportunities: List<ArbitrageOpportunity>,
    minProfitUSD: Double
): ArbitrageOpportunity? {
    return opportunities
        .maxByOrNull { it.estimatedNetProfitUsd }
        ?.takeIf { it.estimatedNetProfitUsd > minProfitUSD }
}


