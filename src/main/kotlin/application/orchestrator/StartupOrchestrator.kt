package application.orchestrator

import configurations.AppConfig
import configurations.DexConfigurations
import configurations.QuoterConfiguration
import domain.events.EventBus
import domain.interfaces.DexQuoter
import domain.models.Chain
import domain.models.Dex
import domain.models.DexPair
import domain.models.registries.DexPairs
import domain.models.registries.PoolRegistry
import infrastructure.blockchain.BlockSubscriber
import infrastructure.blockchain.Web3Utils
import infrastructure.dex.AerodromePoolResolver
import infrastructure.dex.UniV2PoolResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import kotlin.time.Duration.Companion.milliseconds

/**
 * Startup Orchestrator coordinates:
 *
 * - application startup
 * - connection mode
 * - DEX pair registration
 * - quoter registration
 * - opportunity processing
 */
class StartupOrchestrator(
    eventBus: EventBus,
    private val poolRegistry: PoolRegistry
) {

    private val logger =
        LoggerFactory.getLogger(StartupOrchestrator::class.java)

    private val opportunityOrchestrator =
        OpportunityOrchestrator(eventBus)

    fun startUp() {
        logger.info("SpreadSniper starting...")

        val useWebSocket =
            AppConfig.useWebSocket &&
                    AppConfig.baseWsRpc != null

        runBlocking {

            logger.info("Discovering configured DEX pools...")

            val dexPairs =
                DexPairs.create(poolRegistry)

            logger.info(
                "Loaded {} arbitrage markets",
                dexPairs.size
            )

            val quoters =
                QuoterConfiguration.create()

            if (useWebSocket) {
                runWebSocketMode(
                    dexPairs,
                    quoters
                )
            } else {
                runPollingMode(
                    dexPairs,
                    quoters
                )
            }
        }
    }

    /**
     * WebSocket mode:
     * process each new block as it arrives.
     */
    private suspend fun runWebSocketMode(
        dexPairs: List<DexPair>,
        quoters: List<DexQuoter>
    ) {
        val wsUrl =
            AppConfig.baseWsRpc
                ?: error("BASE_WS_RPC not configured")

        var lastEmailMs = 0L

        logger.info(
            "Subscribing to blocks via WebSocket..."
        )

        BlockSubscriber
            .subscribeNewBlocks(wsUrl)
            .catch { e ->

                logger.error(
                    "WebSocket error, falling back to polling: {}",
                    e.message
                )

                val web3 =
                    Web3Utils()
                        .getWeb3ForChain(Chain.BASE)

                runPollingLoop(
                    dexPairs = dexPairs,
                    web3 = web3,
                    quoters = quoters,
                    initialLastEmailMs = lastEmailMs
                )
            }
            .collect { blockEvent ->

                logger.debug(
                    "Processing block {}",
                    blockEvent.number
                )

                val web3 =
                    BlockSubscriber.getWeb3()
                        ?: Web3Utils()
                            .getWeb3ForChain(
                                dexPairs.first().chain
                            )

                lastEmailMs =
                    opportunityOrchestrator
                        .processOpportunities(
                            dexPairs = dexPairs,
                            web3 = web3,
                            quoters = quoters,
                            lastEmailMs = lastEmailMs
                        )
            }
    }

    /**
     * Polling mode:
     * check prices at a fixed interval.
     */
    private suspend fun runPollingMode(
        dexPairs: List<DexPair>,
        quoters: List<DexQuoter>
    ) {
        val web3 =
            Web3Utils()
                .getWeb3ForChain(
                    dexPairs.first().chain
                )

        runPollingLoop(
            dexPairs = dexPairs,
            web3 = web3,
            quoters = quoters,
            initialLastEmailMs = 0L
        )
    }

    private suspend fun runPollingLoop(
        dexPairs: List<DexPair>,
        web3: Web3j,
        quoters: List<DexQuoter>,
        initialLastEmailMs: Long
    ) {
        logger.info("Starting polling")
        var lastEmailMs =
            initialLastEmailMs

        while (true) {
            lastEmailMs =
                opportunityOrchestrator
                    .processOpportunities(
                        dexPairs = dexPairs,
                        web3 = web3,
                        quoters = quoters,
                        lastEmailMs = lastEmailMs
                    )

            delay(
                AppConfig.pollingIntervalMs.milliseconds
            )
        }
    }
}