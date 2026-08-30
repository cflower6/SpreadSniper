package orchestrator

import configurations.AppConfig
import events.EventBus
import interfaces.DexQuoter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import models.DexPair
import models.createQuoters
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import services.BlockSubscriber
import utils.Web3Utils
import kotlin.time.Duration.Companion.milliseconds

/**
 * Startup Orchestrator will orchestrate the start up ->
 * init type of connection ->
 * init the process ->
 * init the Opportunity Orchestrator
 *
 */
class StartupOrchestrator(private val eventBus: EventBus) {
    private val logger = LoggerFactory.getLogger("StartupOrchestrator")
    fun startUp() {
        logger.info("SpreadSniper starting...")

        val useWebSocket = AppConfig.useWebSocket && AppConfig.baseWsRpc != null

        if (useWebSocket) logger.info("Mode: WebSocket (real-time blocks)") else logger.info("Mode: Polling (interval: {}ms)", AppConfig.pollingIntervalMs)

        logger.info("Profit threshold: \${} | Email cooldown: {}ms",
            AppConfig.profitThresholdUSD, AppConfig.emailCooldownMs)

        runBlocking {
            val dexPairs = listOf(
                DexPair.BASE_AERO_UNI_WETH,
                DexPair.BASE_AERO_UNI_USDBC,
                DexPair.BASE_AERO_UNI_cbETH,
                DexPair.BASE_AERO_UNI_AERO,
            )

            if (useWebSocket) runWebSocketMode(dexPairs, createQuoters()) else runPollingMode(dexPairs, createQuoters())
        }
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
                val web3 = Web3Utils().getWeb3ForChain(dexPairs.first().buyOn.chain)
                runPollingLoop(dexPairs, web3, quoters, lastEmailMs)
            }
            .collect { blockEvent ->
                logger.debug("Processing block {}", blockEvent.number)

                // Use the WebSocket-connected Web3j for RPC calls
                val web3 = BlockSubscriber.getWeb3() ?: Web3Utils().getWeb3ForChain(dexPairs.first().buyOn.chain)

                lastEmailMs = OpportunityOrchestrator(eventBus).processOpportunities(dexPairs, web3, quoters, lastEmailMs)
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
        val web3Base = Web3Utils().getWeb3ForChain(dexPairs.first().buyOn.chain)
        val lastEmailMs = 0L

        runPollingLoop(dexPairs, web3Base, quoters, lastEmailMs)
    }

    /**
     * Incase we want to add multiple polling types
     */
    private suspend fun runPollingLoop(
        dexPairs: List<DexPair>,
        web3: Web3j,
        quoters: List<DexQuoter>,
        initialLastEmailMs: Long
    ) {
        var lastEmailMs = initialLastEmailMs

        while (true) {
            lastEmailMs = OpportunityOrchestrator(eventBus).processOpportunities(dexPairs, web3, quoters, lastEmailMs)
            delay(AppConfig.pollingIntervalMs.milliseconds)
        }
    }
}