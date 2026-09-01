package application.listeners

import domain.events.OpportunityEvent
import domain.interfaces.OpportunityRepository

class OpportunityCacheListener(
    private val opportunityRepository: OpportunityRepository
) {
    suspend fun handle(event: OpportunityEvent.OpportunityFound) {
        opportunityRepository.save(event.data)
    }
}