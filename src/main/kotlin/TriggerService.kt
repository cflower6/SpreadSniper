import configurations.AppConfig
import models.Chain
import models.DexPair
import services.DetectedSpread
import utils.toHuman

object TriggerService {
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

    data class OpportunityV2(
        val chain: Chain,              // "base"
        val dexBuy: String,
        val dexSell: String,
        val tokenIn: String,
        val tokenOut: String,
        val amountIn: String,           // raw units as string
        val quotedOutBuy: String,
        val quotedOutSell: String,
        val blockNumber: String,
        val ttlMs: Long,
        val minNetProfitUsd: String
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
            .filter { it.adjustedProfit > minProfitUSD }
            .maxByOrNull { it.adjustedProfit }
            ?.also {
                println("💰 Best opportunity: ${it.pair.label}")
                println("    ➤ Net Profit: $${"%.4f".format(it.adjustedProfit)}")
                println("    ➤ Spread: ${"%.6f".format(it.spread)} | Buy = ${"%.6f".format(it.buyPrice)} | Sell = ${"%.6f".format(it.sellPrice)}")
            }
    }

    fun toOpportunity(pair: DexPair, snap: DetectedSpread): Opportunity? {
        if (snap.quotes.size < 2) return null

        val sorted = snap.quotes.sortedBy { it.amountOutRaw }
        val worst = sorted.first()
        val best = sorted.last()

        val amountInHuman = toHuman(snap.amountInRaw, snap.tokenIn).toDouble()
        if (!amountInHuman.isFinite() || amountInHuman <= 0.0) return null

        val amountOutWorstHuman = toHuman(worst.amountOutRaw, snap.tokenOut).toDouble()
        val amountOutBestHuman  = toHuman(best.amountOutRaw, snap.tokenOut).toDouble()

        // Guard zero/NaN quotes
        if (!amountOutWorstHuman.isFinite() || !amountOutBestHuman.isFinite()) return null
        if (amountOutWorstHuman <= 0.0 || amountOutBestHuman <= 0.0) return null

        // Only treat as USD if tokenOut is USDC (otherwise units mismatch with gasCostEstimate)
        if (snap.tokenOut.symbol != "USDC") return null

        val buyPrice = amountOutWorstHuman / amountInHuman
        val sellPrice = amountOutBestHuman / amountInHuman
        if (!buyPrice.isFinite() || !sellPrice.isFinite()) return null

        val rawSpread = sellPrice - buyPrice

        val grossProfit = rawSpread * amountInHuman  // in USDC ≈ USD
        val avgNotional = ((buyPrice + sellPrice) / 2.0) * amountInHuman
        val dexFeeLoss = 2.0 * AppConfig.dexFeeRate * avgNotional

        val netProfit = grossProfit - dexFeeLoss - AppConfig.gasCostEstimate
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
        println("🔴 No profitable opportunities found.")
        opportunities.forEach {
            println("    ❌ ${it.pair.label} → Spread: ${"%.6f".format(it.spread)}")
        }
    }
}