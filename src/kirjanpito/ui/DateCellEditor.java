package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

/**
 * <code>TableCellEditor</code>in toteuttava luokka, jolla
 * muokataan päivämääriä.
 * 
 * @author Tommi Helineva
 */
public class DateCellEditor extends AbstractCellEditor
	implements TableCellEditor
{
	private DateFormat formatter;
	private DateTextField textField;

	private static final long serialVersionUID = 1L;
	
	public DateCellEditor() {
		formatter = new SimpleDateFormat("d.M.yyyy");
		textField = new DateTextField();
		textField.setBorder(new LineBorder(Color.BLACK));
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int rowIndex, int vColIndex) {
		
		if (value == null) {
			textField.setText("");
		}
		else {
			String text = formatter.format((Date)value);
			textField.setText(text);
			textField.setSelectionStart(0);
			textField.setSelectionEnd(text.length());
		}
		
		return textField;
	}

	/**
	 * Palauttaa käyttäjän kirjoittaman päivämäärän.
	 * 
	 * @return päivämäärä
	 */
	public Object getCellEditorValue() {
		try {
			return textField.getDate();
		}
		catch (ParseException e) {
			return null;
		}
	}
}