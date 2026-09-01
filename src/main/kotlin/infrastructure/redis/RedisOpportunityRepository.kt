package infrastructure.redis

import client.RedisClient
import domain.interfaces.OpportunityRepository
import domain.models.ArbitrageOpportunity
import infrastructure.redis.dto.toRedisDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class RedisOpportunityRepository(
    private val redisClient: RedisClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
) : OpportunityRepository {

    override suspend fun save(
        opportunity: ArbitrageOpportunity
    ) {
        val dto =
            opportunity.toRedisDto()

        val serialized =
            json.encodeToString(dto)

        redisClient.set(
            key = opportunityKey(opportunity.id),
            value = serialized,
            ttlSeconds = 300
        )

        redisClient.set(
            key = LATEST_KEY,
            value = serialized,
            ttlSeconds = 30
        )
    }

    override suspend fun findLatest(): ArbitrageOpportunity? {
        TODO(
            "For now Redis returns DTOs. Add DTO -> domain mapping when this method is needed."
        )
    }

    override suspend fun findById(
        id: UUID
    ): ArbitrageOpportunity? {
        TODO(
            "For now Redis returns DTOs. Add DTO -> domain mapping when this method is needed."
        )
    }

    private fun opportunityKey(
        id: UUID
    ): String =
        "spreadsniper:opportunity:$id"

    companion object {
        private const val LATEST_KEY =
            "spreadsniper:opportunity:latest"
    }
}