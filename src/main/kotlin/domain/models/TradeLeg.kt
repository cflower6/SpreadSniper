package domain.models

import domain.models.registries.Token
import java.math.BigInteger

data class TradeLeg(
    val dex: Dex,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountInRaw: BigInteger,
    val amountOutRaw: BigInteger
)