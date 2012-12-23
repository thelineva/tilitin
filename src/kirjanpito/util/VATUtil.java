package kirjanpito.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VATUtil {
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	public static BigDecimal addVatAmount(BigDecimal percent, BigDecimal amount) {
		return amount.multiply(percent.divide(percent));
	}

	public static BigDecimal subtractVatAmount(BigDecimal percent, BigDecimal amount) {
		BigDecimal f = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(
				percent.add(HUNDRED).divide(HUNDRED), 14, RoundingMode.HALF_UP));
		return amount.multiply(f).setScale(2, RoundingMode.HALF_UP);
	}
}
