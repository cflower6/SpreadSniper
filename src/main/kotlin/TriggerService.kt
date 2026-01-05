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
        // You’ll want to pick the best buy vs sell based on which gives you more tokenOut per tokenIn
        val sorted = snap.quotes.sortedBy { it.amountOutRaw } // ascending
        val worst = sorted.first()
        val best = sorted.last()

        // Convert to human prices (tokenOut per tokenIn) so existing profit math works
        val amountOutWorstHuman = toHuman(worst.amountOutRaw, snap.tokenOut).toDouble()
        val amountOutBestHuman  = toHuman(best.amountOutRaw, snap.tokenOut).toDouble()
        val amountInHuman       = toHuman(snap.amountInRaw, snap.tokenIn).toDouble()

        // Price here is tokenOut per tokenIn
        val buyPrice = amountOutWorstHuman / amountInHuman
        val sellPrice = amountOutBestHuman / amountInHuman

        val rawSpread = sellPrice - buyPrice

        // Gross profit in USD (since buy/sell prices are $/WETH)
        val grossProfitUsd = rawSpread * amountInHuman

        // Approx two-leg fee model: pay fee on notional twice
        val avgNotionalUsd = ((buyPrice + sellPrice) / 2.0) * amountInHuman
        val dexFeeLossUsd = 2.0 * AppConfig.dexFeeRate * avgNotionalUsd

        val netProfit = grossProfitUsd - dexFeeLossUsd - AppConfig.gasCostEstimate

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