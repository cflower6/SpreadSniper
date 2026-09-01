package domain.interfaces

import domain.models.OpportunityKey

interface ExecutionIdempotencyStore {
    /**
     * Attempts to claim an opportunity for execution.
     *
     * @return true if this caller successfully claimed it.
     *         false if it has already been claimed.
     */
    suspend fun tryClaim(
        opportunityKey: OpportunityKey
    ): Boolean

//    suspend fun markSubmitted(
//        opportunityKey: OpportunityKey,
//        txHash: String
//    )
//
//    suspend fun markCompleted(
//        opportunityKey: OpportunityKey
//    )
//
//    suspend fun markFailed(
//        opportunityKey: OpportunityKey,
//        reason: String
//    )
}