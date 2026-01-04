package utils

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger

data class BlockCtx(val number: BigInteger, val param: DefaultBlockParameter)

/**
 * Helper function to stay on the same block (used to try and prevent fake drift)
 */
fun currentBlockCtx(web3: Web3j): BlockCtx {
    val bn = web3.ethBlockNumber().send().blockNumber
    return BlockCtx(bn, DefaultBlockParameter.valueOf(bn))
}
