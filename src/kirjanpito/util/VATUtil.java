package kirjanpito.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VATUtil {
	public static final String[] VAT_RATE_TEXTS = {
		"0 %", "8 %", "9 %", "12 %", "13 %", "17 %", "22 %", "23 %"
	};
	
	public static final int[] VAT_RATE_V2M = {
		0, 3, 5, 4, 6, 2, 1, 7
	};
	
	public static final int[] VAT_RATE_M2V = {
		0, 6, 5, 1, 3, 2, 4, 7
	};
	
	public static final BigDecimal[] VAT_RATES = {
		null,
		new BigDecimal("0.22"),
		new BigDecimal("0.17"),
		new BigDecimal("0.08"),
		new BigDecimal("0.12"),
		new BigDecimal("0.09"),
		new BigDecimal("0.13"),
		new BigDecimal("0.23")
	};
	
	public static BigDecimal addVatAmount(int vatRateIndex, BigDecimal amount) {
		return amount.multiply(VAT_RATES[vatRateIndex]);
	}
	
	public static BigDecimal subtractVatAmount(int vatRateIndex, BigDecimal amount) {
		BigDecimal f = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(
				BigDecimal.ONE.add(VAT_RATES[vatRateIndex]), 14, RoundingMode.HALF_UP));
		return amount.multiply(f).setScale(2, RoundingMode.HALF_UP);
	}
}
