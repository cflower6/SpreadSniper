import client.RedisClient
import configurations.AppConfig
import configurations.DotenvLoader
import events.EventBus
import events.OpportunityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import orchestrator.StartupOrchestrator
import org.slf4j.LoggerFactory
import repositories.ArbitrageExecutorRepository
import repositories.RedisOpportunityRepository
import services.ArbitrageExecutorService
import services.DiscordNotifierService
import services.OpportunityCacheListener
import utils.messageBuilder
import kotlin.time.ExperimentalTime

private val logger = LoggerFactory.getLogger("SpreadSniper")

@OptIn(ExperimentalTime::class)
fun main() {
    logger.info("Loading in environment variables...")
    DotenvLoader.load()
    logger.info("Loaded environment")

    val eventBus = EventBus()

    // Create a standalone application scope to handle background listeners
    val appScope = CoroutineScope(Dispatchers.Default + Job())

    logger.info("Started listening on ${appScope.coroutineContext.job}.")
    // Launch thread to start listening for Notification Events
    appScope.launch {
        eventBus.events.collect { event ->
            if (event is OpportunityEvent.Notification) {
                println("[Notification listener] Acted on event: ${event.data}")
                try {
                    DiscordNotifierService.send(AppConfig.discordUrl, messageBuilder(event.data))
                    logger.info("[Notification listener] Successfully sent event: $event")
                } catch (e: Exception) {
                    logger.error("Failed to send discord: {}", e.message)
                }
            }
        }
    }
    // Launch thread to start listening for Redis Events
    appScope.launch {
        eventBus.events.collect { event ->
            if (event is OpportunityEvent.OpportunityFound) {
                logger.info("[Redis listener] Found event: $event")
                try {
                    OpportunityCacheListener(RedisOpportunityRepository(RedisClient())).handle(event)
                    logger.info("[Redis listener] Successfully sent event: $event")
                } catch (e: Exception) {
                    logger.error("Failed to update redis cache: {}", e.message)
                }
            }
        }
    }
    // Launch thread to start listening for Executor Events
    appScope.launch {
        eventBus.events.collect { event ->
            if (event is OpportunityEvent.ExecuteOpportunity) {
                logger.info("[Executor listener] Found event: $event")
                try {
                    ArbitrageExecutorService(ArbitrageExecutorRepository()).execute(event.data)
                    logger.info("[ArbitrageExecutorRepository] Successfully sent event: $event")
                } catch (e: Exception) {
                    logger.error("Failed to execute Arbitrage: {}", e.message)
                }
            }
        }
    }

    logger.info("Initializing start up orchestrator...")
    StartupOrchestrator(eventBus).startUp()

    // Block main slightly at the very end to give the background threads time to log output
    Thread.sleep(2000)
    logger.info("[Main] System execution complete.")
    appScope.cancel() // Cleanup resources
}

