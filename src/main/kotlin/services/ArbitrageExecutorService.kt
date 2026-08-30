package services

import models.ArbitrageOpportunity
import repositories.ArbitrageExecutorRepository
import utils.Web3Utils

class ArbitrageExecutorService(private val arbitrageExecutorRepository: ArbitrageExecutorRepository) {
    suspend fun execute(opportunity: ArbitrageOpportunity) {
        arbitrageExecutorRepository.execute(
            web3 = Web3Utils().getWeb3ForChain(opportunity.chain),
            opportunity = opportunity,
            buyQuote = opportunity.snapshot.quotes.first(),
            sellQuote = opportunity.snapshot.quotes.last(),
            buyRouterAddress = opportunity.snapshot.tokenIn.address,
            sellRouterAddress = opportunity.snapshot.tokenOut.address,
            tokenPath = opportunity.pair.buyOn.path,
            amountIn = opportunity.snapshot.amountInRaw
        )
    }
}