import services.TriggerService.toOpportunity
import configurations.AppConfig
import configurations.DotenvLoader
import dex.AerodromeQuoter
import dex.UniV2Quoter
import interfaces.DexQuoter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import models.DexPair
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import registries.Tokens
import services.BlockSubscriber
import services.Detector
import services.NotifierService
import services.TriggerService
import utils.GasEstimator
import utils.getWeb3ForChain

private val logger = LoggerFactory.getLogger("SpreadSniper")

fun main() {
    DotenvLoader.load()

    logger.info("SpreadSniper starting...")

    val useWebSocket = AppConfig.useWebSocket && AppConfig.baseWsRpc != null

    if (useWebSocket) {
        logger.info("Mode: WebSocket (real-time blocks)")
        logger.info("Profit threshold: \${} | Email cooldown: {}ms",
            AppConfig.profitThresholdUSD, AppConfig.emailCooldownMs)
    } else {
        logger.info("Mode: Polling (interval: {}ms)", AppConfig.pollingIntervalMs)
        logger.info("Profit threshold: \${} | Email cooldown: {}ms",
            AppConfig.profitThresholdUSD, AppConfig.emailCooldownMs)
    }

    runBlocking {
        val dexPairs = listOf(
            DexPair.BASE_AERO_UNI_WETH,
            DexPair.BASE_AERO_UNI_USDBC,
            DexPair.BASE_AERO_UNI_cbETH,
            DexPair.BASE_AERO_UNI_AERO,
        )
        val quoters = createQuoters()

        if (useWebSocket) {
            runWebSocketMode(dexPairs, quoters)
        } else {
            runPollingMode(dexPairs, quoters)
        }
    }
}

private fun createQuoters(): List<DexQuoter> {
    // more up-to-date router
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

    return listOf(aeroQuoter, uniV2Quoter)
}

/**
 * WebSocket mode: Subscribe to new blocks and detect on each block.
 * Lower latency than polling - reacts immediately to new blocks.
 */
private suspend fun runWebSocketMode(
    dexPairs: List<DexPair>,
    quoters: List<DexQuoter>
) {
    val wsUrl = AppConfig.baseWsRpc ?: error("BASE_WS_RPC not configured")
    var lastEmailMs = 0L

    logger.info("Subscribing to blocks via WebSocket...")

    BlockSubscriber.subscribeNewBlocks(wsUrl)
        .catch { e ->
            logger.error("WebSocket error, falling back to polling: {}", e.message)
            // Fallback to polling mode
            val web3 = getWeb3ForChain(dexPairs.first().buyOn.chain)
            runPollingLoop(dexPairs, web3, quoters, lastEmailMs)
        }
        .collect { blockEvent ->
            logger.debug("Processing block {}", blockEvent.number)

            // Use the WebSocket-connected Web3j for RPC calls
            val web3 = BlockSubscriber.getWeb3() ?: getWeb3ForChain(dexPairs.first().buyOn.chain)

            lastEmailMs = processOpportunities(dexPairs, web3, quoters, lastEmailMs)
        }
}

/**
 * Polling mode: Check prices at fixed intervals.
 * More reliable but higher latency than WebSocket.
 */
private suspend fun runPollingMode(
    dexPairs: List<DexPair>,
    quoters: List<DexQuoter>
) {
    val web3Base = getWeb3ForChain(dexPairs.first().buyOn.chain)
    val lastEmailMs = 0L

    runPollingLoop(dexPairs, web3Base, quoters, lastEmailMs)
}

private suspend fun runPollingLoop(
    dexPairs: List<DexPair>,
    web3: Web3j,
    quoters: List<DexQuoter>,
    initialLastEmailMs: Long
) {
    var lastEmailMs = initialLastEmailMs

    while (true) {
        lastEmailMs = processOpportunities(dexPairs, web3, quoters, lastEmailMs)
        delay(AppConfig.pollingIntervalMs)
    }
}

/**
 * Core opportunity detection and notification logic.
 * Shared between WebSocket and polling modes.
 */
private suspend fun processOpportunities(
    dexPairs: List<DexPair>,
    web3: Web3j,
    quoters: List<DexQuoter>,
    lastEmailMs: Long
): Long {
    var newLastEmailMs = lastEmailMs

    // Fetch current gas cost
    val gasCostUsd = if (AppConfig.dynamicGasEnabled) {
        GasEstimator.estimateGasCostUsd(web3, AppConfig.gasLimit)
    } else {
        AppConfig.gasCostEstimate
    }

    val found = detectPairsInParallel(dexPairs, web3, quoters, gasCostUsd)

    TriggerService.findBestOpportunity(found, AppConfig.profitThresholdUSD)?.let { opp ->
        logger.info("Found opportunity: {} | Profit: \${}", opp.pair.label, "%.4f".format(opp.adjustedProfit))

        val now = System.currentTimeMillis()
        if (now - lastEmailMs > AppConfig.emailCooldownMs) {
            try {
                val body = """
                    Pair: ${opp.pair.label}
                    Spread: ${"%.5f".format(opp.spread)}
                    Est Profit: $${"%.2f".format(opp.adjustedProfit)}
                """.trimIndent()

                NotifierService.send(
                    subject = "sniper_find",
                    body = body,
                )
                newLastEmailMs = now
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

    return newLastEmailMs
}

private suspend fun detectPairsInParallel(
    dexPairs: List<DexPair>,
    web3: Web3j,
    quoters: List<DexQuoter>,
    gasCostUsd: Double
): List<TriggerService.Opportunity> = coroutineScope {
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
