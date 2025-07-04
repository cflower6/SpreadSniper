import models.Dex
import org.web3j.protocol.Web3j
import java.math.BigInteger

object DexOracle {
    fun getUniswapPrice(web3: Web3j, amountIn: BigInteger): Double? {
        return DexUtils.getPriceFromRouter(
            web3 = web3,
            router = Dex.UNISWAP.router,
            path = Dex.UNISWAP.path,
            amountIn = amountIn,
            decimals = Dex.UNISWAP.outputDecimals
        )
    }

    fun getSushiPrice(web3: Web3j, amountIn: BigInteger): Double? {
        return DexUtils.getPriceFromRouter(
            web3 = web3,
            router = Dex.SUSHI.router,
            path = Dex.SUSHI.path,
            amountIn = amountIn,
            decimals = Dex.SUSHI.outputDecimals
        )
    }
}
