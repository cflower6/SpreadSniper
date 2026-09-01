package infrastructure.redis

import client.RedisClient
import domain.interfaces.ExecutionIdempotencyStore
import domain.models.OpportunityKey

class RedisExecutionIdempotencyStore(
    private val redis: RedisClient,
    private val ttlSeconds: Long = 120
) : ExecutionIdempotencyStore {

    override suspend fun tryClaim(
        opportunityKey: OpportunityKey
    ): Boolean {

        val key =
            "spreadsniper:execution:${opportunityKey.value}"

        return redis.setIfAbsent(
            key = key,
            value = "PROCESSING",
            ttlSeconds = ttlSeconds
        )
    }
}