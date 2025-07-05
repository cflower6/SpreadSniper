import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun BigInteger.toEth(decimals: Int): Double {
    return this.toBigDecimal()
        .divide(BigDecimal.TEN.pow(decimals), 6, RoundingMode.HALF_UP)
        .toDouble()
}