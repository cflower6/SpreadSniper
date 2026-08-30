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
import services.DiscordNotifierService
import kotlin.time.Clock
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
    appScope.launch {
        eventBus.events.collect { event ->
            if (event is OpportunityEvent.Notification) {
                println("[Notification listener] Acted on event: ${event.data}")
                val opportunity = event.data
                val msg = """
                        🚨 ARBITRAGE DETECTED 🚨

                        Buy: ${opportunity.buyDex}
                        Sell: ${opportunity.sellDex}

                        TokenIn: ${opportunity.tokenIn}
                        TokenOut: ${opportunity.tokenOut}

                        Net Profit: ${opportunity.estimatedNetProfitUsd}

                        Spread: ${opportunity.grossSpreadBps}
                        Time: ${Clock.System.now()}
                        """.trimIndent()
                try {
                    DiscordNotifierService.send(AppConfig.discordUrl,msg)
                    logger.info("[Notification listener] Successfully sent event: $event")
                } catch (e: Exception) {
                    logger.error("Failed to send discord: {}", e.message)
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

