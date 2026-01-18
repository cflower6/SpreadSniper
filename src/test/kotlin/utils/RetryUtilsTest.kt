package utils

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RetryUtilsTest {

    @Test
    fun `withRetrySync returns result on first success`() {
        var attempts = 0

        val result = withRetrySync(maxAttempts = 3) {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `withRetrySync retries on failure and succeeds`() {
        var attempts = 0

        val result = withRetrySync(
            maxAttempts = 3,
            initialDelayMs = 10 // Short delay for tests
        ) {
            attempts++
            if (attempts < 3) throw IOException("Simulated failure")
            "success on third try"
        }

        assertEquals("success on third try", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `withRetrySync returns null after all attempts fail`() {
        var attempts = 0

        val result = withRetrySync<String>(
            maxAttempts = 3,
            initialDelayMs = 10
        ) {
            attempts++
            throw IOException("Always fails")
        }

        assertNull(result)
        assertEquals(3, attempts)
    }

    @Test
    fun `withRetrySync respects retryOn predicate`() {
        var attempts = 0

        val result = withRetrySync<String>(
            maxAttempts = 3,
            initialDelayMs = 10,
            retryOn = { e -> e is IOException } // Only retry IOException
        ) {
            attempts++
            throw IllegalArgumentException("Non-retryable") // Different exception
        }

        assertNull(result)
        assertEquals(1, attempts) // Should not retry
    }

    @Test
    fun `withRetry async version works correctly`() = runTest {
        var attempts = 0

        val result = withRetry(
            maxAttempts = 3,
            initialDelayMs = 10
        ) {
            attempts++
            if (attempts < 2) throw IOException("Retry me")
            42
        }

        assertEquals(42, result)
        assertEquals(2, attempts)
    }

    @Test
    fun `withRetrySync handles null return from block`() {
        val result = withRetrySync<String?>(maxAttempts = 3) {
            null
        }

        assertNull(result)
    }

    @Test
    fun `exponential backoff increases delay`() {
        var lastTime = System.currentTimeMillis()
        val delays = mutableListOf<Long>()
        var attempts = 0

        withRetrySync<String>(
            maxAttempts = 4,
            initialDelayMs = 50,
            factor = 2.0,
            maxDelayMs = 500
        ) {
            val now = System.currentTimeMillis()
            if (attempts > 0) {
                delays.add(now - lastTime)
            }
            lastTime = now
            attempts++
            throw IOException("Keep retrying")
        }

        // Delays should roughly be: 50, 100, 200 (capped at pattern)
        // Allow some variance for timing
        assertEquals(3, delays.size)
        delays.forEachIndexed { index, delay ->
            // Each delay should be at least close to expected
            val expectedMin = (50 * Math.pow(2.0, index.toDouble())).toLong() - 20
            assert(delay >= expectedMin) { "Delay $index was $delay, expected >= $expectedMin" }
        }
    }
}
