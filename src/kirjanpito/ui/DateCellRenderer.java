package kirjanpito.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * <code>TableCellRenderer</code>in toteuttava luokka, joka
 * näyttää päivämääriä.
 * 
 * @author Tommi Helineva
 */
public class DateCellRenderer extends DefaultTableCellRenderer {
	private DateFormat formatter;
	
	private static final long serialVersionUID = 1L;
	
	public DateCellRenderer() {
		formatter = new SimpleDateFormat("d.M.yyyy");
	}

	protected void setValue(Object obj) {
		if (obj == null) {
			setText(null);
		}
		else {
			setText(formatter.format((Date)obj));
		}
	}
}
