package kirjanpito.ui;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class ComboBoxCellEditor extends AbstractCellEditor
	implements TableCellEditor {
	
	private static final long serialVersionUID = 1L;
	private JComboBox comboBox;
	
	public ComboBoxCellEditor(Object[] items) {
		comboBox = new JComboBox(items);
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

		comboBox.setSelectedIndex((Integer)value);
		return comboBox;
	}
	
	public Object getCellEditorValue() {
		return comboBox.getSelectedIndex();
	}
}
