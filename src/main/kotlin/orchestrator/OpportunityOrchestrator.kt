package orchestrator

import configurations.AppConfig
import events.EventBus
import events.OpportunityEvent
import interfaces.DexQuoter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import models.ArbitrageOpportunity
import models.DexPair
import models.findBestOpportunity
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import models.registries.Tokens
import services.DetectedSpread
import services.Detector
import utils.GasEstimator
import utils.Web3Utils
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class OpportunityOrchestrator(private val eventBus: EventBus) {
    private val logger = LoggerFactory.getLogger("OpportunityOrchestrator")
    /**
     * Core opportunity detection and notification logic.
     * Shared between WebSocket and polling modes.
     */
    suspend fun processOpportunities(
        dexPairs: List<DexPair>,
        web3: Web3j,
        quoters: List<DexQuoter>,
        lastEmailMs: Long
    ): Long {
        // Fetch current gas cost or default
        val gasCostUsd = if (AppConfig.dynamicGasEnabled) GasEstimator.estimateGasCostUsd(web3, AppConfig.gasLimit) else AppConfig.gasCostEstimate

        val found = detectPairsInParallel(dexPairs, web3, quoters, gasCostUsd)
        logger.info("Found: {}", found)

        findBestOpportunity(found, AppConfig.profitThresholdUSD)?.let { opp ->
            logger.info("Best opportunity: {} | Net Profit: \${} | Spread: {} | Buy: {} | Sell: {}",
                "buyOn: "+ opp.buyDex + " -> sellOn: " + opp.sellDex,
                "%.4f".format(opp.estimatedNetProfitUsd),
                "%.6f".format(opp.grossSpreadBps),
                "%.6f".format(opp.buyQuote),
                "%.6f".format(opp.sellQuote))

            val now = System.currentTimeMillis()
            if (now - lastEmailMs > AppConfig.emailCooldownMs) eventBus.emit(OpportunityEvent.OpportunityFound(opp))
            else logger.debug("Notification event was skipped")
        } ?: run {
            logNoOpportunities(found)
        }

        return lastEmailMs
    }

    private suspend fun detectPairsInParallel(
        dexPairs: List<DexPair>,
        web3: Web3j,
        quoters: List<DexQuoter>,
        gasCostUsd: Double
    ): List<ArbitrageOpportunity> = coroutineScope {
        dexPairs.map { pair ->
            async {
                try {
                    val tokenIn = Tokens.byAddress(pair.buyOn.path.first())
                    val tokenOut = Tokens.byAddress(pair.buyOn.path.last())
                    val amountInRaw = AppConfig.tradeAmount

                    val snap = Detector.detectOnce(web3, tokenIn, tokenOut, amountInRaw, quoters)
                    if (snap != null) {
                        logger.debug("Detector block: {}", snap.blockNumber)
                        toOpportunity(pair, snap, gasCostUsd)
                    } else null
                } catch (e: Exception) {
                    logger.warn("Error detecting {}: {}", pair.label, e.message)
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    @OptIn(ExperimentalTime::class)
    private fun toOpportunity(pair: DexPair, snap: DetectedSpread, gasCostUsd: Double = AppConfig.gasCostEstimate): ArbitrageOpportunity? {
        if (snap.quotes.size < 2) return null

        val worst = snap.quotes.minByOrNull { it.amountOutRaw } ?: return null
        val best = snap.quotes.maxByOrNull { it.amountOutRaw } ?: return null

        val amountInHuman = Web3Utils().toHuman(snap.amountInRaw, snap.tokenIn).toDouble()
        if (!amountInHuman.isFinite() || amountInHuman <= 0.0) return null

        val amountOutWorstHuman = Web3Utils().toHuman(worst.amountOutRaw, snap.tokenOut).toDouble()
        val amountOutBestHuman  = Web3Utils().toHuman(best.amountOutRaw, snap.tokenOut).toDouble()

        // Guard zero/NaN quotes
        if (!amountOutWorstHuman.isFinite() || !amountOutBestHuman.isFinite()) return null
        if (amountOutWorstHuman <= 0.0 || amountOutBestHuman <= 0.0) return null

        // Only treat as USD if tokenOut is a stablecoin (otherwise units mismatch with gasCostEstimate)
        if (snap.tokenOut.symbol !in AppConfig.stableCoins) return null

        val buyPrice = amountOutWorstHuman / amountInHuman
        val sellPrice = amountOutBestHuman / amountInHuman
        if (!buyPrice.isFinite() || !sellPrice.isFinite()) return null

        // rawSpread = sellPrice - buyPrice = $/WETH
        // so if buyPrice (WETH = 3100) and sellPrice (WETH = 3110) then we get the rawSpread which is 10 dollars
        val rawSpread = sellPrice - buyPrice

        // the raw spread would be 10 times the amount of WETH's we bought (so let's say 5 WETH) meaning our profit is 50
        val grossProfit = rawSpread * amountInHuman  // in USDC ≈ USD

        // Per-DEX fee calculation: buy on worst DEX, sell on best DEX
        val buyNotional = buyPrice * amountInHuman
        val sellNotional = sellPrice * amountInHuman
        val buyFee = worst.feeRate * buyNotional
        val sellFee = best.feeRate * sellNotional
        val totalDexFees = buyFee + sellFee

        // our NET is the updated gross, the dexFee and the gasCost
        val netProfit = grossProfit - totalDexFees - gasCostUsd
        if (!netProfit.isFinite()) return null

        return ArbitrageOpportunity(
            UUID.randomUUID(),
            pair.chain,
            snap.tokenIn.toString(),
            snap.tokenOut.toString(),
            pair.buyOn.dexName,
            pair.sellOn.dexName,
            pair,
            snap,
            amountInHuman,
            buyPrice,
            sellPrice,
            grossProfit,
            gasCostUsd,
            totalDexFees,
            netProfit,
            rawSpread,
            snap.blockNumber,
            Clock.System.now()
        )
    }

    private fun logNoOpportunities(opportunities: List<ArbitrageOpportunity>) {
        if (opportunities.isEmpty()) {
            logger.debug("No opportunities detected")
            return
        }
        logger.debug("No profitable opportunities. Spreads: {}",
            opportunities.joinToString { "buyOn: ${it.buyDex + " -> sellOn: " + it.sellDex}=${"%.6f".format(it.grossSpreadBps)}" })
    }
}