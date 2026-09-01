package domain.models

import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class ArbitrageOpportunity @OptIn(ExperimentalTime::class) constructor(
    val id: UUID = UUID.randomUUID(),

    val pair: DexPair,

    val firstLeg: TradeLeg,
    val secondLeg: TradeLeg,

    val grossProfitUsd: Double,
    val estimatedGasUsd: Double,
    val estimatedFeesUsd: Double,
    val estimatedNetProfitUsd: Double,

    val grossSpreadBps: Double,

    val blockNumber: BigInteger,
    val observedAt: Instant
) {

    val chain: Chain
        get() = pair.chain

    val amountInRaw: BigInteger
        get() = firstLeg.amountInRaw

    val finalAmountRaw: BigInteger
        get() = secondLeg.amountOutRaw

    val opportunityKey: OpportunityKey =
        OpportunityKey(generateOpportunityKey())

    private fun generateOpportunityKey(): String {
        val canonical = listOf(
            chain.name,
            blockNumber.toString(),

            firstLeg.dex.name,
            firstLeg.tokenIn.address.lowercase(),
            firstLeg.tokenOut.address.lowercase(),
            firstLeg.amountInRaw.toString(),

            secondLeg.dex.name,
            secondLeg.tokenIn.address.lowercase(),
            secondLeg.tokenOut.address.lowercase(),
            secondLeg.amountInRaw.toString()
        ).joinToString(":")

        return MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

@JvmInline
value class OpportunityKey(
    val value: String
)

fun selectBestOpportunity(
    opportunities: List<ArbitrageOpportunity>,
    minProfitUsd: Double
): ArbitrageOpportunity? {
    return opportunities
        .maxByOrNull { it.estimatedNetProfitUsd }
        ?.takeIf {
            it.estimatedNetProfitUsd > minProfitUsd
        }
}