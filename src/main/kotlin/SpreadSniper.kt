import TriggerService.toOpportunity
import configurations.AppConfig
import configurations.DotenvLoader
import dex.AerodromeQuoter
import dex.UniV2Quoter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import models.DexPair
import registries.Tokens
import services.Detector
import services.EmailNotifierService
import utils.getWeb3ForChain

fun main() {
    DotenvLoader.load()

    runBlocking {
        val threshold = AppConfig.profitThresholdUSD

        val dexPairs = listOf(DexPair.BASE_AERO_UNI_WETH)

        val aeroQuoter = AerodromeQuoter(
            name = "AERODROME",
            router = "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
            factory = "0x420dd381b31aef6683db6b902084cb0ffece40da",
            stable = false
        )

        val uniV2Quoter = UniV2Quoter(
            name = "UNIV2",
            router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24"
        )

        val quoters = listOf(aeroQuoter, uniV2Quoter)

        val web3Base = getWeb3ForChain(dexPairs.first().buyOn.chain)

        var lastEmailMs = 0L

        val EMAIL_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

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
                    val tokenIn = Tokens.byAddress(pair.buyOn.path.first())
                    val tokenOut = Tokens.byAddress(pair.buyOn.path.last())

                    val amountInRaw = AppConfig.tradeAmount

                    val snap = Detector.detectOnce(web3Base, tokenIn, tokenOut, amountInRaw, quoters)

                    if (snap == null) continue
                    println("Detector block: ${snap.blockNumber}")

                    val opp = toOpportunity(pair, snap)
                    if (opp != null) found += opp

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
                    val body = """
                        Pair: ${it.pair.label}
                        Spread: ${"%.5f".format(it.spread)}
                        Est Profit: $${"%.2f".format(it.adjustedProfit)}
                    """.trimIndent()

                    val now = System.currentTimeMillis()
                    // Stopping spam :)
                    if (now - lastEmailMs > EMAIL_COOLDOWN_MS) {
                        EmailNotifierService.send(
                            subject = "🚨 SpreadSniper Opportunity",
                            body = body
                        )
                        lastEmailMs = now
                    }


//                    sendToLoanShot(
//                        TriggerPayload(
//                            it.pair.label,
//                            0.99,
//                            1.01,
//                            0.8,
//                        )
//                    )
                } catch (e: Exception) {
                    println("<UNK> Error in ${e.message}")
                } finally {
                    println("🟢 Starting the LoanShot app")
                }
            } ?: run {
                TriggerService.logNoOpportunities(found)
            }

            delay(5000)
        }
    }
}
