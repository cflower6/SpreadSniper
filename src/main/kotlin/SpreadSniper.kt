import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() {

    DotenvLoader.load() // ✅ Loads all .env variables as system properties
    runBlocking {
        val threshold = AppConfig.profitThresholdUSD
        val amountIn = AppConfig.tradeAmount // 1 DAI

        val dexPairs = listOf(
            DexPair.UNI_SUSHI_ETH,
//            DexPair.UNI_BASE_ETH,
//            DexPair.UNI_SUSHI_BASE,
        )

        while (true) {
            val found = mutableListOf<TriggerService.Opportunity>()

            for (pair in dexPairs) {
                try {
                    val web3Buy = getWeb3ForChain(pair.buyOn.chain)
                    val web3Sell = getWeb3ForChain(pair.sellOn.chain)

                    val buyPrice = DexUtils.getPriceFromRouter(web3Buy, pair.buyOn.router, pair.buyOn.path, amountIn)
                    val sellPrice =
                        DexUtils.getPriceFromRouter(web3Sell, pair.sellOn.router, pair.sellOn.path, amountIn)

                    if (buyPrice == null || sellPrice == null) continue

                    val amountInEth = amountIn.toEth(pair.buyOn.outputDecimals)
                    val tradeAmountUSD = buyPrice * amountInEth

                    val rawSpread = sellPrice - buyPrice

                    val grossProfit = rawSpread * tradeAmountUSD
                    val dexFeeLoss = (buyPrice + sellPrice) * 0.5 * AppConfig.dexFeeRate * tradeAmountUSD
                    val netProfit = grossProfit - dexFeeLoss - AppConfig.gasCostEstimate
                    found += TriggerService.Opportunity(
                        pair = pair,
                        buyPrice = buyPrice,
                        sellPrice = sellPrice,
                        spread = rawSpread,
                        adjustedProfit = netProfit
                    )

                } catch (e: Exception) {
                    println("🔥 Error in ${pair.label}: ${e.message}")
                }
            }

            val best = TriggerService.findBestOpportunity(found, threshold)

            if (best != null) {
                // 🔫 Insert trigger logic here (call LoanShot)
                println("🟢 Starting the LoanShot app")
            } else {
                TriggerService.logNoOpportunities(found)
            }

            delay(15000) // Check every 5 seconds
        }
    }
}