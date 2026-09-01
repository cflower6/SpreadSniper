//package application.services
//
//import infrastructure.redis.dto.RedisOpportunityDto
//
//class OpportunityQueryService(
//    private val repository: OpportunityCacheRepository
//) {
//
//    suspend fun findLatest(): RedisOpportunityDto? =
//        repository.findLatest()
//}