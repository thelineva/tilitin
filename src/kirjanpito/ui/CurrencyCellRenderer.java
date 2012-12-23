package kirjanpito.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * <code>TableCellRenderer</code>in toteuttava luokka, joka
 * näyttää rahamääriä.
 *
 * @author Tommi Helineva
 */
public class CurrencyCellRenderer extends DefaultTableCellRenderer {
	private DecimalFormat formatter;

	private static final long serialVersionUID = 1L;

	public CurrencyCellRenderer() {
		this(new DecimalFormat());
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
	}

	public CurrencyCellRenderer(DecimalFormat formatter) {
		this.formatter = formatter;
		setHorizontalAlignment(SwingConstants.RIGHT);
	}

	protected void setValue(Object obj) {
		if (obj == null) {
			setText(null);
		}
		else {
			setText(formatter.format((BigDecimal)obj));
		}
	}
}