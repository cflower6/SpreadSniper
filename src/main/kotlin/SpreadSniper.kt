import configurations.AppConfig
import configurations.DotenvLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import models.DexPair
import utils.DexUtils
import utils.getWeb3ForChain
import utils.toEth

fun main() {

    DotenvLoader.load()
    runBlocking {
        val threshold = AppConfig.profitThresholdUSD
        val amountIn = AppConfig.tradeAmount // 1 DAI

        val dexPairs = listOf(
            DexPair.BASE_AERO_UNI_WETH,
        )

        // Start of the check loop
        while (true) {
            val found = mutableListOf<TriggerService.Opportunity>()
            /**
             * For each pair in our dexPairs (we can configure it more)
             *
             * we set the buy and set the sell chain (we set SUSHI or BASE chain)
             *
             * We then get the price
             */
            for (pair in dexPairs) {
                try {
                    // Use the web3 "SDK" to set the chains we want
                    val web3Buy = getWeb3ForChain(pair.buyOn.chain)
                    val web3Sell = getWeb3ForChain(pair.sellOn.chain)

                    // Get the current price for the same crypto on different "Routers" (DEXs)
                    val buyPrice =
                        DexUtils.getPriceFromRouter(web3Buy, pair.buyOn.router, pair.buyOn.path, amountIn, 6)

                    val sellPrice =
                        DexUtils.getPriceFromRouter(web3Sell, pair.sellOn.router, pair.sellOn.path, amountIn, 18)

                    // do our null checks
                    if (buyPrice == null || sellPrice == null) continue

                    // We get the amount in ETH for easier compares
                    val amountInEth = amountIn.toEth(pair.buyOn.outputDecimals)

                    // Find the amount
                    val tradeAmountUSD = buyPrice * amountInEth

                    // simple sellPrice minus buy price
                    val rawSpread = sellPrice - buyPrice

                    //
                    val grossProfit = rawSpread * tradeAmountUSD
                    val dexFeeLoss = (buyPrice + sellPrice) * 0.5 * AppConfig.dexFeeRate * tradeAmountUSD
                    val netProfit = grossProfit - dexFeeLoss - AppConfig.gasCostEstimate

                    // We're building the opportunity object we have
                    found += TriggerService.Opportunity(
                        pair = pair,
                        buyPrice = buyPrice,
                        sellPrice = sellPrice,
                        spread = rawSpread,
                        adjustedProfit = netProfit
                    )

                } catch (e: Exception) {
                    println("Error in ${pair.label}: ${e.message}")
                }
            }

            /**
             * Cool kotlin thing --- because findBestOpportunity is returning an "optional" we can use let on it
             *
             * let executes when findBestOpportunity is not null
             *
             * ?: run executes when best IS null
             */
            TriggerService.findBestOpportunity(found, threshold)?.let {
                try {
                    sendToLoanShot(TriggerPayload(
                        it.pair.label,
                        0.99,
                        1.01,
                        0.8,
                    ))
                } catch (e: Exception) {
                    println("<UNK> Error in ${e.message}")
                } finally {
                    println("🟢 Starting the LoanShot app")
                }
            } ?: run {
                TriggerService.logNoOpportunities(found)
            }

            delay(5000) // Check every 5 seconds
        }
    }
}