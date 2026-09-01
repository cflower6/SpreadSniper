package infrastructure.redis.dto

import domain.models.ArbitrageOpportunity
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class RedisOpportunityDto(
    val id: String,
    val opportunityKey: String,

    val chain: String,
    val pairLabel: String,

    val firstLeg: RedisTradeLegDto,
    val secondLeg: RedisTradeLegDto,

    val grossProfitUsd: Double,
    val estimatedGasUsd: Double,
    val estimatedFeesUsd: Double,
    val estimatedNetProfitUsd: Double,

    val grossSpreadBps: Double,

    val blockNumber: String,
    val observedAt: String
)

@Serializable
data class RedisTradeLegDto(
    val dex: String,

    val tokenInSymbol: String,
    val tokenInAddress: String,

    val tokenOutSymbol: String,
    val tokenOutAddress: String,

    val amountInRaw: String,
    val amountOutRaw: String
)

@OptIn(ExperimentalTime::class)
fun ArbitrageOpportunity.toRedisDto(): RedisOpportunityDto =
    RedisOpportunityDto(
        id = id.toString(),
        opportunityKey = opportunityKey.value,

        chain = chain.name,
        pairLabel = pair.label,

        firstLeg = RedisTradeLegDto(
            dex = firstLeg.dex.name,

            tokenInSymbol = firstLeg.tokenIn.symbol,
            tokenInAddress = firstLeg.tokenIn.address,

            tokenOutSymbol = firstLeg.tokenOut.symbol,
            tokenOutAddress = firstLeg.tokenOut.address,

            amountInRaw = firstLeg.amountInRaw.toString(),
            amountOutRaw = firstLeg.amountOutRaw.toString()
        ),

        secondLeg = RedisTradeLegDto(
            dex = secondLeg.dex.name,

            tokenInSymbol = secondLeg.tokenIn.symbol,
            tokenInAddress = secondLeg.tokenIn.address,

            tokenOutSymbol = secondLeg.tokenOut.symbol,
            tokenOutAddress = secondLeg.tokenOut.address,

            amountInRaw = secondLeg.amountInRaw.toString(),
            amountOutRaw = secondLeg.amountOutRaw.toString()
        ),

        grossProfitUsd = grossProfitUsd,
        estimatedGasUsd = estimatedGasUsd,
        estimatedFeesUsd = estimatedFeesUsd,
        estimatedNetProfitUsd = estimatedNetProfitUsd,

        grossSpreadBps = grossSpreadBps,

        blockNumber = blockNumber.toString(),
        observedAt = observedAt.toString()
    )