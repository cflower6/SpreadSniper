package domain.interfaces

import domain.models.ArbitrageOpportunity
import java.util.UUID

interface OpportunityRepository {
    suspend fun save(opportunity: ArbitrageOpportunity)

    suspend fun findLatest(): ArbitrageOpportunity?

    suspend fun findById(id: UUID): ArbitrageOpportunity?
}