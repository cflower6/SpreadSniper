package repositories

import client.RedisClient
import interfaces.OpportunityRepository
import models.ArbitrageOpportunity

class RedisOpportunityRepository(
    private val redisClient: RedisClient
) : OpportunityRepository {
    override suspend fun save(opportunity: ArbitrageOpportunity) {
        // serialize and store
    }

    override suspend fun findLatest(): ArbitrageOpportunity? {
        // read latest
        return null
    }

    override suspend fun findById(id: String): ArbitrageOpportunity? {
        return null
    }
}