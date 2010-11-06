package kirjanpito.ui;

import javax.swing.table.DefaultTableCellRenderer;

public class ComboBoxCellRenderer extends DefaultTableCellRenderer {
	private String[] items;

	private static final long serialVersionUID = 1L;
	
	public ComboBoxCellRenderer(String[] items) {
		this.items = items;
	}

	protected void setValue(Object obj) {
		int index = (Integer)obj;
		setText((index < 0) ? "" : items[index]);
	}
}