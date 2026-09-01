package application.health

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HealthState {

    val scannerRunning =
        AtomicBoolean(false)

    val redisConnected =
        AtomicBoolean(false)

    val marketCount =
        AtomicInteger(0)

    val lastProcessedBlock =
        AtomicLong(0)

    val lastScanDurationMs =
        AtomicLong(0)
}