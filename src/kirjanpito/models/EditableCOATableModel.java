package kirjanpito.models;

import java.awt.Component;

import javax.swing.JOptionPane;

import kirjanpito.ui.Kirjanpito;
import kirjanpito.util.ChartOfAccounts;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * tilikartan tiedot. Tietoja voi myös muokata.
 * 
 * @author Tommi Helineva
 */
public class EditableCOATableModel extends COATableModel {
	private Component parent;
	private COAModel model;
	
	private static final long serialVersionUID = 1L;
	
	public EditableCOATableModel(Component parent, COAModel model) {
		this.parent = parent;
		this.model = model;
	}
	
	/**
	 * Palauttaa <code>true</code>, jos taulukon solua voi muokata.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 * @return <code>true</code>, jos taulukon solua voi muokata
	 */
	public boolean isCellEditable(int row, int col) {
		ChartOfAccounts coa = getChartOfAccounts();
		int type = coa.getType(row);
		
		/* Väliotsikon numerosaraketta ei voi muokata. */
		return type == ChartOfAccounts.TYPE_ACCOUNT ||
			(type == ChartOfAccounts.TYPE_HEADING && col == 1);
	}

	/**
	 * Asettaa solun arvon.
	 * 
	 * @param value arvo
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public void setValueAt(Object value, int row, int col) {
		ChartOfAccounts coa = getChartOfAccounts();
		int type = coa.getType(row);
		
		if (type == ChartOfAccounts.TYPE_ACCOUNT && col == 0) {
			String number = value.toString();
			
			/* Tilinumero ei saa olla tyhjä. */
			if (number.length() == 0) {
				return;
			}
			
			/* Ensimmäisen merkin on oltava numero. */
			if (!Character.isDigit(number.charAt(0))) {
				JOptionPane.showMessageDialog(parent,
						"Tilinumeron ensimmäisen merkin on oltava numero.", Kirjanpito.APP_NAME,
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			coa.getAccount(row).setNumber(value.toString());
		}
		else if (type == ChartOfAccounts.TYPE_ACCOUNT && col == 1) {
			coa.getAccount(row).setName(value.toString());
		}
		else if (type == ChartOfAccounts.TYPE_HEADING && col == 1) {
			coa.getHeading(row).setText(value.toString());
		}
		
		if (col == 0) {
			model.updateRow(row, true);
			fireTableDataChanged();
		}
		else {
			model.updateRow(row, false);
			fireTableCellUpdated(row, col);
		}
	}
}
