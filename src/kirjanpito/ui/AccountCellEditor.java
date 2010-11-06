package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;

import kirjanpito.db.Account;
import kirjanpito.util.Registry;

/**
 * <code>TableCellEditor</code>in toteuttava luokka, jolla muokataan
 * viennin tiliä.
 * 
 * @author Tommi Helineva
 */
public class AccountCellEditor extends AbstractCellEditor
	implements TableCellEditor
{
	private JTextField textField;
	private ActionListener listener;
	private Registry registry;

	private static final long serialVersionUID = 1L;
	
	public AccountCellEditor(Registry registry, ActionListener listener)
	{
		this.textField = new JTextField();
		this.textField.getDocument().addDocumentListener(documentListener);
		this.textField.addKeyListener(keyListener);
		this.textField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.registry = registry;
		this.listener = listener;
	}

	public JTextField getTextField() {
		return textField;
	}
	
	public ActionListener getActionListener() {
		return listener;
	}

	public void setActionListener(ActionListener listener) {
		this.listener = listener;
	}
	
	public boolean isCellEditable(EventObject evt) {
		if (evt instanceof MouseEvent) {
			if (((MouseEvent)evt).getClickCount() >= 2) {
				textField.setText("");
				fireActionPerformed();
			}
			
			return false;
		}
		
		return true;
	}

	/**
	 * Palauttaa tekstilaatikon, johon tilinumero kirjoitetaan.
	 * 
	 * @return tekstilaatikko
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int rowIndex, int vColIndex) {
		
		Account account = registry.getAccountById((Integer)value);
		
		if (account == null) {
			textField.setText("");
		}
		else {
			textField.setText(account.getNumber());
			textField.setSelectionStart(0);
			textField.setSelectionEnd(account.getNumber().length());
		}
		
		return textField;
	}

	/**
	 * Palauttaa käyttäjän valitseman tilin tunnisteen.
	 * 
	 * @return tilin tunniste
	 */
	public Object getCellEditorValue() {
		String number = textField.getText();
		Account account = registry.getAccountByNumber(number);
		return (account == null) ? null : account.getId();
	}
	
	protected void fireActionPerformed() {
		if (listener != null)
			listener.actionPerformed(new ActionEvent(this, 0, null));
	}
	
	private KeyAdapter keyListener = new KeyAdapter() {
		public void keyTyped(KeyEvent e) {
			/* Avataan tilinvalintaikkuna, jos tekstikentässä
			 * painetaan '-näppäintä. */
			if (e.getKeyChar() == '\'') {
				fireActionPerformed();
				e.consume();
			}
		}
	};
	
	private DocumentListener documentListener = new DocumentListener() {
		public void changedUpdate(DocumentEvent e) { }
		public void removeUpdate(DocumentEvent e) { }

		public void insertUpdate(DocumentEvent e) {
			String text = textField.getText();
			
			/* Avataan tilinvalintaikkuna, jos tekstikentän ensimmäinen
			 * merkki on jokin muu kuin numero. */
			if (text.length() == 1 && !Character.isDigit(text.charAt(0))) {
				fireActionPerformed();
			}
		}
	};
}