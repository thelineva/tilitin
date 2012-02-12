package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
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
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import kirjanpito.db.Account;
import kirjanpito.util.ChartOfAccounts;
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
	private AccountTextField textField;
	private ActionListener listener;
	private TableModel tableModel;
	private Registry registry;

	private static final long serialVersionUID = 1L;
	
	public AccountCellEditor(Registry registry, TableModel tableModel, ActionListener listener)
	{
		this.textField = new AccountTextField();
		this.textField.getDocument().addDocumentListener(documentListener);
		this.textField.addKeyListener(keyListener);
		this.textField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.registry = registry;
		this.tableModel = tableModel;
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
		else if (evt instanceof KeyEvent) {
			return (((KeyEvent)evt).getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0;
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
		
		Integer accountId = (Integer)tableModel.getValueAt((Integer)value, -1);
		Account account = registry.getAccountById(accountId);
		
		if (account == null) {
			textField.setText("");
		}
		else {
			textField.setText(account.getNumber());
			textField.setSelectionStart(0);
			textField.setSelectionEnd(account.getNumber().length());
		}
		
		textField.clearAccountName();
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
	
	private class AccountTextField extends JTextField implements ActionListener {
		private Timer timer;
		private String accountName;
		private static final long serialVersionUID = 1L;

		public AccountTextField() {
			timer = new Timer(400, this);
			timer.setRepeats(false);
			clearAccountName();
		}
		
		public void clearAccountName() {
			accountName = "";
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			if (!accountName.isEmpty()) {
				Insets insets = getBorder().getBorderInsets(this);
				StringBuilder sb = new StringBuilder(getText());
				while (sb.length() < 4) sb.append('0');
				sb.append(' ');
				int x = g.getFontMetrics().stringWidth(sb.toString());
				int y = insets.top + g.getFontMetrics().getAscent() + 1;
				g.setColor(Color.gray);
				g.drawString(accountName, x, y);
			}
		}

		@Override
		protected void processKeyEvent(KeyEvent e) {
			super.processKeyEvent(e);
			
			if (e.getID() == KeyEvent.KEY_TYPED && Character.isLetterOrDigit(e.getKeyChar()) &&
					getText().length() >= 2) {
				clearAccountName();
				timer.restart();
			}
			else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
				clearAccountName();
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String text = getText();
			
			if (text.length() < 2) {
				clearAccountName();
			}
			else {
				ChartOfAccounts coa = registry.getChartOfAccounts();
				int index = coa.search(text);
				
				if (index >= 0 && coa.getType(index) == ChartOfAccounts.TYPE_ACCOUNT) {
					Account account = coa.getAccount(index);
					accountName = account.getName();
					setText(account.getNumber());
					setCaretPosition(text.length());
					moveCaretPosition(account.getNumber().length());
				}
				else {
					clearAccountName();
				}
			}
			
			repaint();
		}
	}
}