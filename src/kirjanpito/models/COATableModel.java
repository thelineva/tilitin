package kirjanpito.models;

import javax.swing.table.AbstractTableModel;

import kirjanpito.util.ChartOfAccounts;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * tilikartan tiedot.
 * 
 * @author Tommi Helineva
 */
public class COATableModel extends AbstractTableModel {
	private ChartOfAccounts coa;
	
	private static final long serialVersionUID = 1L;
	
	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Numero", "Nimi"
	};

	/**
	 * Palauttaa tilikartan, jonka tiedot näytetään.
	 * 
	 * @return tilikartta
	 */
	public ChartOfAccounts getChartOfAccounts() {
		return coa;
	}

	/**
	 * Asettaa tilikartan, jonka tiedot näytetään.
	 * 
	 * @param coa tilikartta
	 */
	public void setChartOfAccounts(ChartOfAccounts coa) {
		this.coa = coa;
		fireTableDataChanged();
	}

	/**
	 * Palauttaa sarakkeiden lukumäärän.
	 * 
	 * @return sarakkeiden lukumäärä
	 */
	public int getColumnCount() {
		return COLUMN_CAPTIONS.length;
	}
	
	/**
	 * Palauttaa tilikartassa olevien rivien lukumäärän.
	 * 
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return (coa == null) ? 0 : coa.getSize();
	}

	/**
	 * Palauttaa sarakkeen <code>index</code> otsikon.
	 * 
	 * @param index sarakkeen indeksi
	 * @return sarakeotsikko
	 */
	public String getColumnName(int index) {
		return COLUMN_CAPTIONS[index];
	}

	/**
	 * Palauttaa solun arvon.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public Object getValueAt(int row, int col) {
		int type = coa.getType(row);
		
		if (type == ChartOfAccounts.TYPE_ACCOUNT && col == 0) {
			return coa.getAccount(row).getNumber();
		}
		else if (type == ChartOfAccounts.TYPE_ACCOUNT && col == 1) {
			return coa.getAccount(row).getName();
		}
		else if (type == ChartOfAccounts.TYPE_HEADING && col == 1) {
			return coa.getHeading(row).getText();
		}
		else {
			return null;
		}
	}
}
