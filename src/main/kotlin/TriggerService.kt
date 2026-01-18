import configurations.AppConfig
import models.DexPair
import org.slf4j.LoggerFactory
import services.DetectedSpread
import utils.toHuman

object TriggerService {
    private val logger = LoggerFactory.getLogger(TriggerService::class.java)
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
        val adjustedProfit: Double // after DEX fees + gas
    )

    fun findBestOpportunity(
        opportunities: List<Opportunity>,
        minProfitUSD: Double
    ): Opportunity? {
        /**
         * Filter such that each opportunity objects adjustedProfile is Greater than our minProfit
         *
         * then find the LARGEST difference or null (safety)
         *
         * also only initiates (look at the ?.) if the previous chain is NOT null
         */
        return opportunities
            .maxByOrNull { it.adjustedProfit }
            ?.takeIf { it.adjustedProfit > minProfitUSD }
            ?.also {
                logger.info("Best opportunity: {} | Net Profit: \${} | Spread: {} | Buy: {} | Sell: {}",
                    it.pair.label,
                    "%.4f".format(it.adjustedProfit),
                    "%.6f".format(it.spread),
                    "%.6f".format(it.buyPrice),
                    "%.6f".format(it.sellPrice))
            }
    }

    fun toOpportunity(pair: DexPair, snap: DetectedSpread, gasCostUsd: Double = AppConfig.gasCostEstimate): Opportunity? {
        if (snap.quotes.size < 2) return null

        val worst = snap.quotes.minByOrNull { it.amountOutRaw } ?: return null
        val best = snap.quotes.maxByOrNull { it.amountOutRaw } ?: return null

        val amountInHuman = toHuman(snap.amountInRaw, snap.tokenIn).toDouble()
        if (!amountInHuman.isFinite() || amountInHuman <= 0.0) return null

        val amountOutWorstHuman = toHuman(worst.amountOutRaw, snap.tokenOut).toDouble()
        val amountOutBestHuman  = toHuman(best.amountOutRaw, snap.tokenOut).toDouble()

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

        return Opportunity(
            pair = pair,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            spread = rawSpread,
            adjustedProfit = netProfit
        )
    }



    fun logNoOpportunities(opportunities: List<Opportunity>) {
        if (opportunities.isEmpty()) {
            logger.debug("No opportunities detected")
            return
        }
        logger.debug("No profitable opportunities. Spreads: {}",
            opportunities.joinToString { "${it.pair.label}=${"%.6f".format(it.spread)}" })
    }
}