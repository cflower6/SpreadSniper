package services

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.websocket.WebSocketService
import java.math.BigInteger

private val logger = LoggerFactory.getLogger("BlockSubscriber")

data class BlockEvent(
    val number: BigInteger,
    val hash: String,
    val timestamp: BigInteger
)

/**
 * Subscribes to new blocks via WebSocket for real-time price detection.
 * Much faster than polling - triggers immediately when blocks are mined.
 */
object BlockSubscriber {
    private var wsService: WebSocketService? = null
    private var web3Ws: Web3j? = null
    private var subscription: Disposable? = null

    /**
     * Creates a Flow that emits BlockEvents as new blocks are mined.
     * Uses WebSocket subscription for real-time updates.
     *
     * @param wsUrl WebSocket RPC URL (wss://...)
     * @return Flow of BlockEvents
     */
    fun subscribeNewBlocks(wsUrl: String): Flow<BlockEvent> = callbackFlow {
        logger.info("Connecting to WebSocket...")

        val service = WebSocketService(wsUrl, false)
        service.connect()
        wsService = service

        val web3 = Web3j.build(service)
        web3Ws = web3

        logger.info("WebSocket connected, subscribing to new blocks")

        subscription = web3.blockFlowable(false).subscribe(
            { ethBlock: EthBlock ->
                val block = ethBlock.block
                if (block != null) {
                    val event = BlockEvent(
                        number = block.number,
                        hash = block.hash,
                        timestamp = block.timestamp
                    )
                    logger.debug("New block: {} ({})", event.number, event.hash.take(10))
                    trySend(event)
                }
            },
            { error ->
                logger.error("Block subscription error: {}", error.message)
                close(error)
            },
            {
                logger.info("Block subscription completed")
                close()
            }
        )

        awaitClose {
            logger.info("Closing WebSocket subscription")
            disconnect()
        }
    }

    /**
     * Returns the WebSocket-connected Web3j instance for RPC calls.
     * Lower latency than HTTP for quote fetching.
     */
    fun getWeb3(): Web3j? = web3Ws

    /**
     * Disconnects WebSocket and cleans up resources.
     */
    fun disconnect() {
        subscription?.dispose()
        subscription = null
        web3Ws?.shutdown()
        web3Ws = null
        wsService?.close()
        wsService = null
    }

    /**
     * Checks if WebSocket is currently connected.
     */
    fun isConnected(): Boolean = wsService != null && web3Ws != null
}
