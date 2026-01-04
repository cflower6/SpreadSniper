package utils

import registries.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun toHuman(amountRaw: BigInteger, token: Token, scale: Int = 8): BigDecimal {
    return amountRaw.toBigDecimal()
        .divide(BigDecimal.TEN.pow(token.decimals), scale, RoundingMode.HALF_UP)
}