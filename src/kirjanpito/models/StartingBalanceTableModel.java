package kirjanpito.models;

import java.math.BigDecimal;

import javax.swing.table.AbstractTableModel;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * tilien alkusaldot.
 * 
 * @author Tommi Helineva
 */
public class StartingBalanceTableModel extends AbstractTableModel {
	private StartingBalanceModel model;
	
	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Nro", "Tili", "Alkusaldo"
	};
	
	private static final long serialVersionUID = 1L;

	public StartingBalanceModel getModel() {
		return model;
	}

	public void setModel(StartingBalanceModel model) {
		this.model = model;
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
		return model.getAccountCount();
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
		if (col == 0) {
			return model.getAccount(row).getNumber();
		}
		else if (col == 1) {
			return model.getAccount(row).getName();
		}
		else if (col == 2) {
			return model.getBalance(row);
		}
		
		return null;
	}

	/**
	 * Palauttaa <code>true</code>, jos taulukon solua voi muokata.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 * @return <code>true</code>, jos taulukon solua voi muokata
	 */
	public boolean isCellEditable(int row, int col) {
		return (col == 2 && model.isEditable());
	}

	/**
	 * Asettaa solun arvon.
	 * 
	 * @param value arvo
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public void setValueAt(Object value, int row, int col) {
		BigDecimal amount = (BigDecimal)value;
		
		if (amount == null) {
			amount = BigDecimal.ZERO;
		}
		
		model.setBalance(row, amount);
		fireTableCellUpdated(row, col);
	}
}
