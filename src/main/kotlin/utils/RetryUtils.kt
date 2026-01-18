package utils

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RetryUtils")

/**
 * Executes a block with exponential backoff retry logic.
 *
 * @param maxAttempts Maximum number of attempts (default 3)
 * @param initialDelayMs Initial delay between retries in ms (default 100)
 * @param maxDelayMs Maximum delay cap in ms (default 2000)
 * @param factor Multiplier for exponential backoff (default 2.0)
 * @param retryOn Predicate to determine if exception should trigger retry (default: all exceptions)
 * @param block The operation to execute
 * @return Result of the block, or null if all retries failed
 */
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 2000,
    factor: Double = 2.0,
    retryOn: (Exception) -> Boolean = { true },
    block: suspend () -> T
): T? {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (!retryOn(e)) {
                logger.debug("Non-retryable exception: {}", e.message)
                return null
            }

            if (attempt < maxAttempts - 1) {
                logger.debug("Attempt {} failed, retrying in {}ms: {}", attempt + 1, currentDelay, e.message)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }

    logger.warn("All {} attempts failed. Last error: {}", maxAttempts, lastException?.message)
    return null
}

/**
 * Synchronous version of withRetry for non-coroutine contexts.
 */
fun <T> withRetrySync(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 2000,
    factor: Double = 2.0,
    retryOn: (Exception) -> Boolean = { true },
    block: () -> T
): T? {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (!retryOn(e)) {
                logger.debug("Non-retryable exception: {}", e.message)
                return null
            }

            if (attempt < maxAttempts - 1) {
                logger.debug("Attempt {} failed, retrying in {}ms: {}", attempt + 1, currentDelay, e.message)
                Thread.sleep(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }

    logger.warn("All {} attempts failed. Last error: {}", maxAttempts, lastException?.message)
    return null
}
