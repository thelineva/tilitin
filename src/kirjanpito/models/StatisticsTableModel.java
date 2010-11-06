package kirjanpito.models;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.Account;

public class StatisticsTableModel extends AbstractTableModel {
	private StatisticsModel model;
	
	private static final long serialVersionUID = 1L;
	
	public StatisticsTableModel(StatisticsModel model) {
		this.model = model;
	}
	
	/**
	 * Palauttaa sarakkeiden lukumäärän.
	 * 
	 * @return sarakkeiden lukumäärä
	 */
	public int getColumnCount() {
		return model.isEnabled() ? model.getPeriodCount() + 1 : 0;
	}
	
	/**
	 * Palauttaa tilikartassa olevien rivien lukumäärän.
	 * 
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return model.isEnabled() ? model.getAccountCount() : 0;
	}

	/**
	 * Palauttaa sarakkeen <code>index</code> otsikon.
	 * 
	 * @param index sarakkeen indeksi
	 * @return sarakeotsikko
	 */
	public String getColumnName(int index) {
		if (index == 0) {
			return "Tili";
		}
		else {
			return model.getPeriodName(index - 1);
		}
	}

	/**
	 * Palauttaa solun arvon.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public Object getValueAt(int row, int col) {
		if (col == 0) {
			Account account = model.getAccount(row);
			return account.getNumber() + " " + account.getName();
		}
		else {
			return model.getAmount(row, col - 1);
		}
	}
}
