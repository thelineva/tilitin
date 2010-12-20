package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import kirjanpito.util.ChartOfAccounts;

/**
 * <code>TableCellRenderer</code>in toteuttava luokka, joka näyttää
 * tilikartan rivin.
 * 
 * @author Tommi Helineva
 */
public class COATableCellRenderer extends DefaultTableCellRenderer {
	private ChartOfAccounts coa;
	private boolean indentEnabled;
	
	private static final long serialVersionUID = 1L;
	
	public COATableCellRenderer() {
		this.indentEnabled = true;
	}
	
	/**
	 * Palauttaa tilikartan, jonka rivit näytetään.
	 * 
	 * @return tilikartta
	 */
	public ChartOfAccounts getChartOfAccounts() {
		return coa;
	}

	/**
	 * Asettaa tilikartan, jonka rivit näytetään.
	 * 
	 * @param coa tilikartta
	 */
	public void setChartOfAccounts(ChartOfAccounts coa) {
		this.coa = coa;
	}

	public boolean isIndentEnabled() {
		return indentEnabled;
	}

	public void setIndentEnabled(boolean indentEnabled) {
		this.indentEnabled = indentEnabled;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column)
	{
		Component comp = super.getTableCellRendererComponent(table, value, 
				isSelected, hasFocus, row, column);

		row = table.convertRowIndexToModel(row);
		Font font = comp.getFont();
		int level;

		if (coa.getType(row) == ChartOfAccounts.TYPE_HEADING) {
			comp.setForeground((coa.getHeading(row).getLevel() == 0) ? Color.RED : Color.BLACK);
			comp.setFont(font.deriveFont(Font.BOLD));
			level = coa.getHeading(row).getLevel() * 2;
		}
		else {
			comp.setForeground(Color.BLACK);
			comp.setFont(font.deriveFont(Font.PLAIN));
			level = 12;
		}
		
		if (!indentEnabled) {
			level = 0;
		}
		
		/* Sisennetään tekstiä. */
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < level; i++) sb.append(' ');
		
		if (value != null)
			sb.append(value.toString());
		
		setText(sb.toString());
		return comp;
	}
}
