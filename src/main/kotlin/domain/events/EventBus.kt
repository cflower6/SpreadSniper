package domain.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import domain.models.ArbitrageOpportunity

sealed interface OpportunityEvent {
    data class OpportunityFound(val data: ArbitrageOpportunity) : OpportunityEvent
    data class Notification(val data: ArbitrageOpportunity) : OpportunityEvent
    data class ExecuteOpportunity(val data: ArbitrageOpportunity) : OpportunityEvent
}

class EventBus {
    private val _events = MutableSharedFlow<OpportunityEvent>()
    val events = _events.asSharedFlow()

    // 1. Create a global background scope specifically for broadcasting events
    private val busScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 2. Non-suspending fire-and-forget emit function
    // Any class can call this from inside or outside a coroutine block safely!
    fun emit(event: OpportunityEvent) {
        busScope.launch {
            _events.emit(event)
        }
    }
}
