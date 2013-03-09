package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

/**
 * <code>TableCellEditor</code>in toteuttava luokka, jolla
 * muokataan rahamääriä.
 *
 * @author Tommi Helineva
 */
public class CurrencyCellEditor extends AbstractCellEditor
	implements TableCellEditor
{
	private DecimalFormat formatter;
	private JTextField textField;
	private char decimalSeparator;
	private int lastModifiers;
	private ActionListener actionListener;

	private static final long serialVersionUID = 1L;

	public CurrencyCellEditor() {
		formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
		formatter.setGroupingUsed(false);
		formatter.setParseBigDecimal(true);
		decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
		textField = new JTextField();
		textField.setBorder(new LineBorder(Color.BLACK));
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					lastModifiers = e.getModifiers();
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();

				if (c == '.' || c == ',') {
					e.setKeyChar(decimalSeparator);
				}
				else if (c == '§') {
					if (actionListener != null) {
						actionListener.actionPerformed(new ActionEvent(this, 0, null));
					}

					e.consume();
				}
			}
		});
	}

	public int getLastModifiers() {
		return lastModifiers;
	}

	public void setLastModifiers(int lastModifiers) {
		this.lastModifiers = lastModifiers;
	}

	public ActionListener getActionListener() {
		return actionListener;
	}

	public void setActionListener(ActionListener actionListener) {
		this.actionListener = actionListener;
	}

	public boolean isCellEditable(EventObject evt) {
		if (evt instanceof MouseEvent) {
			/* Aloitetaan muokkaaminen tuplaklikkauksen jälkeen. */
			return ((MouseEvent)evt).getClickCount() >= 2;
		}

		if (evt instanceof KeyEvent) {
			return ((KeyEvent)evt).getKeyChar() != '§';
		}

		return true;
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int rowIndex, int vColIndex) {

		if (value == null) {
			textField.setText("");
		}
		else {
			String text = formatter.format((BigDecimal)value);
			textField.setText(text);
			textField.setSelectionStart(0);
			textField.setSelectionEnd(text.length());
		}

		return textField;
	}

	/**
	 * Palauttaa käyttäjän kirjoittaman rahamäärän.
	 *
	 * @return rahamäärä
	 */
	public Object getCellEditorValue() {
		try {
			return ((BigDecimal)formatter.parse(
					textField.getText())).setScale(2, RoundingMode.HALF_UP);
		}
		catch (ParseException e) {
			return null;
		}
	}
}
