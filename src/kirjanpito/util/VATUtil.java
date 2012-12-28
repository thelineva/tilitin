package kirjanpito.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VATUtil {
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	public static BigDecimal addVatAmount(BigDecimal percentage, BigDecimal amount) {
		return amount.multiply(percentage.divide(HUNDRED));
	}

	public static BigDecimal subtractVatAmount(BigDecimal percentage, BigDecimal amount) {
		BigDecimal f = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(
				percentage.add(HUNDRED).divide(HUNDRED), 14, RoundingMode.HALF_UP));
		return amount.multiply(f).setScale(2, RoundingMode.HALF_UP);
	}
}
