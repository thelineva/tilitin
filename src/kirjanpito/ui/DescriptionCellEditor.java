package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

import kirjanpito.models.DocumentModel;

public class DescriptionCellEditor extends AbstractCellEditor
	implements TableCellEditor
{
	private JTextField textField;
	private DocumentModel model;
	private int accountId;

	private static final long serialVersionUID = 1L;
	
	public DescriptionCellEditor(DocumentModel documentModel) {
		textField = new JTextField();
		textField.setBorder(new LineBorder(Color.BLACK));
		textField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_F12) {
					removeSuffix();
					return;
				}
				
				if (!Character.isLetterOrDigit(e.getKeyChar())) return;
				String text = textField.getText();
				int len = textField.getCaretPosition();
				if (len < 2) return;
				String autoCompletion = model.autoCompleteEntryDescription(
						accountId, text);
				
				if (autoCompletion != null) {
					textField.setText(autoCompletion);
					textField.setCaretPosition(autoCompletion.length());
					textField.moveCaretPosition(len);
				}
			}
		});
		this.model = documentModel;
	}
	
	public JTextField getTextField() {
		return textField;
	}

	public boolean isCellEditable(EventObject evt) {
		if (evt instanceof MouseEvent) {
			/* Aloitetaan muokkaaminen tuplaklikkauksen jälkeen. */
			return ((MouseEvent)evt).getClickCount() >= 2;
		}

		return true;
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int rowIndex, int vColIndex) {

		if (value == null) {
			textField.setText("");
		}
		else {
			String text = value.toString();
			textField.setText(text);
			textField.setCaretPosition(0);
			textField.moveCaretPosition(text.length());
		}

		accountId = model.getEntry(rowIndex).getAccountId();
		return textField;
	}

	public Object getCellEditorValue() {
		return textField.getText();
	}
	
	public void removeSuffix() {
		String text = textField.getText();
		int pos = text.lastIndexOf(',');
		
		/* Jos pilkkua ei löydy, tyhjennetään tekstikenttä. */
		if (pos < 0) {
			text = "";
		}
		else {
			String trimmed = text.trim();
			boolean notWhitespace = pos + 1 < text.length() && text.charAt(pos + 1) != ' ';
			text = text.substring(0, pos);
			
			if (trimmed.charAt(trimmed.length() - 1) != ',') {
				text += notWhitespace ? "," : ", ";
			}
		}
		
		textField.setText(text);
	}
}