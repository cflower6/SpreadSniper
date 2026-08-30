package models

import services.DetectedSpread
import java.math.BigInteger
import java.security.MessageDigest
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
    val chain: Chain,
    val tokenIn: String,
    val tokenOut: String,
    val buyDex: String,
    val sellDex: String,

    val pair: DexPair,
    val snapshot: DetectedSpread,

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
) {
    val opportunityKey: OpportunityKey = OpportunityKey(
        generateOpportunityKey()
    )

    private fun generateOpportunityKey(): String {
        val raw = listOf(
            blockNumber,
            tokenIn.lowercase(),
            tokenOut.lowercase(),
            buyDex.lowercase(),
            sellDex.lowercase(),
            amountIn.toString()
        ).joinToString(":")

        return MessageDigest
            .getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

@JvmInline
value class OpportunityKey(
    val value: String
)

/**
 * Creates a key that helps with idempotency - we don't want events to hit twice for our execution bot
 */



fun findBestOpportunity(
    opportunities: List<ArbitrageOpportunity>,
    minProfitUSD: Double
): ArbitrageOpportunity? {
    return opportunities
        .maxByOrNull { it.estimatedNetProfitUsd }
        ?.takeIf { it.estimatedNetProfitUsd > minProfitUSD }
}


