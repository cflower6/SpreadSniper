object TriggerService {
    data class Opportunity(
        val pair: DexPair,
        val buyPrice: Double,
        val sellPrice: Double,
        val spread: Double,
        val adjustedProfit: Double // 💰 after DEX fees + gas
    )

    fun findBestOpportunity(
        opportunities: List<Opportunity>,
        minProfitUSD: Double
    ): Opportunity? {
        return opportunities
            .filter { it.adjustedProfit > minProfitUSD }
            .maxByOrNull { it.adjustedProfit }
            ?.also {
                println("💰 Best opportunity: ${it.pair.label}")
                println("    ➤ Net Profit: $${"%.4f".format(it.adjustedProfit)}")
                println("    ➤ Spread: ${"%.6f".format(it.spread)} | Buy = ${"%.6f".format(it.buyPrice)} | Sell = ${"%.6f".format(it.sellPrice)}")
            }
    }

    fun logNoOpportunities(opportunities: List<Opportunity>) {
        println("🔴 No profitable opportunities found.")
        opportunities.forEach {
            println("    ❌ ${it.pair.label} → Spread: ${"%.6f".format(it.spread)}")
        }
    }
}