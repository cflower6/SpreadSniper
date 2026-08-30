package interfaces

import models.ArbitrageOpportunity

interface OpportunityRepository {
    suspend fun save(opportunity: ArbitrageOpportunity)
    suspend fun findLatest(): ArbitrageOpportunity?
    suspend fun findById(id: String): ArbitrageOpportunity?
}