import application.executor.ArbitrageExecutor
import application.executor.ExecutionEvaluator
import application.listeners.OpportunityCacheListener
import application.orchestrator.StartupOrchestrator
import client.RedisClient
import configurations.AppConfig
import configurations.DexConfigurations
import configurations.DotenvLoader
import domain.events.EventBus
import domain.events.OpportunityEvent
import domain.models.Chain
import domain.models.Dex
import domain.models.registries.PoolRegistry
import infrastructure.blockchain.Web3Utils
import infrastructure.dex.AerodromePoolResolver
import infrastructure.dex.UniV2PoolResolver
import infrastructure.execution.RouterRegistry
import infrastructure.execution.TradeExecutor
import infrastructure.execution.WalletFundedArbitrageExecutionStrategy
import infrastructure.notification.DiscordNotifierService
import infrastructure.redis.RedisExecutionIdempotencyStore
import infrastructure.redis.RedisOpportunityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import utils.messageBuilder
import kotlin.time.ExperimentalTime

private val logger =
    LoggerFactory.getLogger("SpreadSniper")

@OptIn(ExperimentalTime::class)
fun main() = runBlocking {

    logger.info("Loading environment variables...")
    DotenvLoader.load()
    logger.info("Environment loaded.")

    /*
     * ======================================================
     * SHARED INFRASTRUCTURE
     * ======================================================
     */

    val web3Utils =
        Web3Utils()

    val baseWeb3 =
        web3Utils.getWeb3ForChain(
            Chain.BASE
        )

    /*
     * ======================================================
     * DEX CONFIGURATION
     * ======================================================
     */

    val aerodromeConfig =
        DexConfigurations.get(
            chain = Chain.BASE,
            dex = Dex.AERODROME
        )

    val uniswapConfig =
        DexConfigurations.get(
            chain = Chain.BASE,
            dex = Dex.UNISWAP
        )

    /*
     * ======================================================
     * POOL DISCOVERY
     * ======================================================
     */

    val poolRegistry =
        PoolRegistry(
            resolvers = listOf(
                AerodromePoolResolver(
                    web3 = baseWeb3,
                    factoryAddress =
                        aerodromeConfig.factoryAddress
                            ?: error(
                                "Aerodrome factory missing"
                            )
                ),

                UniV2PoolResolver(
                    web3 = baseWeb3,
                    factoryAddress =
                        uniswapConfig.factoryAddress
                            ?: error(
                                "Uniswap factory missing"
                            )
                )
            )
        )

    /*
     * ======================================================
     * REDIS
     * ======================================================
     */

    val redisClient =
        RedisClient(
            redisUrl = AppConfig.redisUrl
        )

    val opportunityRepository =
        RedisOpportunityRepository(
            redisClient = redisClient
        )

    val executionIdempotencyStore =
        RedisExecutionIdempotencyStore(
            redis = redisClient,
            ttlSeconds =
                AppConfig.executionClaimTtlSeconds
        )

    /*
     * ======================================================
     * EXECUTION INFRASTRUCTURE
     * ======================================================
     */

    val routerRegistry =
        RouterRegistry()

    val executionEvaluator =
        ExecutionEvaluator()

    // Initialize wallet credentials once at application startup.
    if (AppConfig.executionEnabled) {

        logger.info("Initializing trade executor...")

        val initialized =
            TradeExecutor.initialize()

        if (!initialized) {
            logger.error(
                "Trade execution is enabled, but TradeExecutor failed to initialize"
            )

            error(
                "Cannot start with EXECUTION_ENABLED=true without a valid wallet"
            )
        }

        logger.info(
            "Trade executor initialized successfully"
        )

    } else {
        logger.info(
            "Trade execution disabled - running in dry-run mode"
        )
    }

    val executionStrategy =
        WalletFundedArbitrageExecutionStrategy(
            web3Utils = web3Utils,
            routerRegistry = routerRegistry,
            tradeExecutor = TradeExecutor
        )

    val arbitrageExecutor =
        ArbitrageExecutor(
            executionEvaluator = executionEvaluator,
            executionStrategy = executionStrategy,
            idempotencyStore = executionIdempotencyStore
        )

    /*
     * ======================================================
     * APPLICATION SERVICES
     * ======================================================
     */

    val eventBus =
        EventBus()

    val opportunityCacheListener =
        OpportunityCacheListener(
            opportunityRepository
        )

    val startupOrchestrator =
        StartupOrchestrator(
            eventBus = eventBus,
            poolRegistry = poolRegistry
        )

    /*
     * ======================================================
     * EVENT CONSUMERS
     * ======================================================
     */

    val appScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default
        )

    logger.info(
        "Starting SpreadSniper event consumers..."
    )

    /*
     * ------------------------------------------------------
     * NOTIFICATION CONSUMER
     * ------------------------------------------------------
     */

    appScope.launch {

        eventBus.events.collect { event ->

            if (
                event !is OpportunityEvent.Notification
            ) {
                return@collect
            }

            val opportunity =
                event.data

            logger.info(
                "[Notification] Received {}",
                opportunity.opportunityKey.value
            )

            try {

                DiscordNotifierService.send(
                    AppConfig.discordUrl,
                    messageBuilder(opportunity)
                )

                logger.info(
                    "[Notification] Sent {}",
                    opportunity.opportunityKey.value
                )

            } catch (e: Exception) {

                logger.error(
                    "[Notification] Failed {}: {}",
                    opportunity.opportunityKey.value,
                    e.message,
                    e
                )
            }
        }
    }

    /*
     * ------------------------------------------------------
     * REDIS CACHE CONSUMER
     * ------------------------------------------------------
     */

    appScope.launch {

        eventBus.events.collect { event ->

            if (
                event !is OpportunityEvent.OpportunityFound
            ) {
                return@collect
            }

            val opportunity =
                event.data

            logger.info(
                "[Cache] Received {}",
                opportunity.opportunityKey.value
            )

            try {

                opportunityCacheListener
                    .handle(event)

                logger.info(
                    "[Cache] Stored {}",
                    opportunity.opportunityKey.value
                )

            } catch (e: Exception) {

                logger.error(
                    "[Cache] Failed {}: {}",
                    opportunity.opportunityKey.value,
                    e.message,
                    e
                )
            }
        }
    }

    /*
     * ------------------------------------------------------
     * EXECUTION CONSUMER
     * ------------------------------------------------------
     */

    appScope.launch {

        eventBus.events.collect { event ->

            if (
                event !is OpportunityEvent.ExecuteOpportunity
            ) {
                return@collect
            }

            val opportunity =
                event.data

            logger.info(
                "[Executor] Received {}",
                opportunity.opportunityKey.value
            )

            try {

                val result =
                    arbitrageExecutor.execute(
                        opportunity
                    )

                logger.info(
                    "[Executor] Result {} -> {}",
                    opportunity.opportunityKey.value,
                    result
                )

            } catch (e: Exception) {

                logger.error(
                    "[Executor] Failed {}: {}",
                    opportunity.opportunityKey.value,
                    e.message,
                    e
                )
            }
        }
    }

    /*
     * ======================================================
     * APPLICATION STARTUP
     * ======================================================
     */

    try {

        logger.info(
            "Initializing startup orchestrator..."
        )

        startupOrchestrator.startUp()

    } finally {

        /*
         * This executes if StartupOrchestrator exits
         * normally or throws.
         */

        logger.info(
            "Shutting down SpreadSniper..."
        )

        appScope.cancel()

        redisClient.close()

        logger.info(
            "SpreadSniper shutdown complete."
        )
    }
}