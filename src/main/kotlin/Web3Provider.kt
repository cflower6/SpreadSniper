import models.Chain
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

fun getWeb3ForChain(chain: Chain): Web3j {
    val rpc = when (chain) {
        Chain.ETHEREUM -> AppConfig.ethereumRpc
        Chain.BASE -> AppConfig.baseRpc
    }
    return Web3j.build(HttpService(rpc))
}