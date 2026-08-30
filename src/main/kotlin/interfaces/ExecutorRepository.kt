package interfaces

import models.ArbitrageOpportunity
import org.web3j.protocol.Web3j
import repositories.ArbitrageResult
import services.QuoteSnapshot
import java.math.BigInteger

/**
 * Executor Repository interface so that we can swap in different executor types on the fly.
 */
interface ExecutorRepository {
    suspend fun execute(web3: Web3j, opportunity: ArbitrageOpportunity, buyQuote: QuoteSnapshot,
                        sellQuote: QuoteSnapshot, buyRouterAddress: String, sellRouterAddress: String,
                        tokenPath: List<String>, amountIn: BigInteger): ArbitrageResult
}