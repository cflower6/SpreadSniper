import TriggerService.toOpportunity
import configurations.AppConfig
import configurations.DotenvLoader
import dex.AerodromeQuoter
import dex.UniV2Quoter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import models.DexPair
import org.slf4j.LoggerFactory
import registries.Tokens
import services.Detector
import services.EmailNotifierService
import utils.GasEstimator
import utils.getWeb3ForChain

private val logger = LoggerFactory.getLogger("SpreadSniper")

fun main() {
    DotenvLoader.load()

    logger.info("SpreadSniper starting...")
    logger.info("Polling interval: {}ms | Profit threshold: \${} | Email cooldown: {}ms",
        AppConfig.pollingIntervalMs, AppConfig.profitThresholdUSD, AppConfig.emailCooldownMs)

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

        while (true) {
            // Fetch current gas cost (dynamic or static based on config)
            val gasCostUsd = if (AppConfig.dynamicGasEnabled) {
                GasEstimator.estimateGasCostUsd(web3Base, AppConfig.gasLimit)
            } else {
                AppConfig.gasCostEstimate
            }

            val found = detectPairsInParallel(dexPairs, web3Base, quoters, gasCostUsd)

            TriggerService.findBestOpportunity(found, threshold)?.let { opp ->
                logger.info("Found opportunity: {} | Profit: \${}", opp.pair.label, "%.4f".format(opp.adjustedProfit))

                val now = System.currentTimeMillis()
                if (now - lastEmailMs > AppConfig.emailCooldownMs) {
                    try {
                        val body = """
                            Pair: ${opp.pair.label}
                            Spread: ${"%.5f".format(opp.spread)}
                            Est Profit: $${"%.2f".format(opp.adjustedProfit)}
                        """.trimIndent()

                        EmailNotifierService.send(
                            subject = "SpreadSniper Opportunity",
                            body = body
                        )
                        lastEmailMs = now
                        logger.info("Email notification sent")
                    } catch (e: Exception) {
                        logger.error("Failed to send email: {}", e.message)
                    }
                } else {
                    logger.debug("Email skipped (cooldown active)")
                }
            } ?: run {
                TriggerService.logNoOpportunities(found)
            }

            delay(AppConfig.pollingIntervalMs)
        }
    }
}

private suspend fun detectPairsInParallel(
    dexPairs: List<DexPair>,
    web3Base: org.web3j.protocol.Web3j,
    quoters: List<interfaces.DexQuoter>,
    gasCostUsd: Double
): List<TriggerService.Opportunity> = coroutineScope {
    dexPairs.map { pair ->
        async {
            try {
                val tokenIn = Tokens.byAddress(pair.buyOn.path.first())
                val tokenOut = Tokens.byAddress(pair.buyOn.path.last())
                val amountInRaw = AppConfig.tradeAmount

                val snap = Detector.detectOnce(web3Base, tokenIn, tokenOut, amountInRaw, quoters)
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
