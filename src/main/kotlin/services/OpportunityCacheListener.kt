package services

import events.OpportunityEvent
import interfaces.OpportunityRepository

class OpportunityCacheListener(
    private val opportunityRepository: OpportunityRepository
) {
    suspend fun handle(event: OpportunityEvent.OpportunityFound) {
        opportunityRepository.save(event.data)
    }
}