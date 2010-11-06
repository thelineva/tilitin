package kirjanpito.models;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.Account;
import kirjanpito.util.VATUtil;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * ALV-kannan muutossäännöt.
 * 
 * @author Tommi Helineva
 */
public class VATChangeTableModel extends AbstractTableModel {
	private VATChangeModel model;
	
	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Tili", "Vanha", "Uusi", "Selite"
	};
	
	private static final long serialVersionUID = 1L;

	public VATChangeTableModel(VATChangeModel model) {
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
	 * Palauttaa rivien lukumäärän.
	 * 
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return model.getRuleCount();
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
			return model.getAccountId(row);
		}
		else if (col == 1) {
			int index = model.getOldVatIndex(row);
			return (index < 0) ? -1 : VATUtil.VAT_RATE_M2V[index];
		}
		else if (col == 2) {
			int index = model.getNewVatIndex(row);
			return (index < 0) ? -1 : VATUtil.VAT_RATE_M2V[index];
		}
		else {
			int accountId = model.getAccountId(row);
			int vatBefore = model.getOldVatIndex(row);
			int vatAfter = model.getNewVatIndex(row);
			
			if (vatBefore < 0 || vatAfter < 0) {
				return "";
			}
			else if (accountId < 0) {
				return String.format("Kopioidaan tilit, joiden ALV-kanta on %s. Uuden tilin ALV-kannaksi asetetaan %s",
						VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[vatBefore]],
						VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[vatAfter]]);
			}
			else {
				Account account = model.getAccount(row);
				return String.format("Kopioidaan tili %s %s. Uuden tilin ALV-kannaksi asetetaan %s",
						account.getNumber(), account.getName(),
						VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[vatAfter]]);
			}
		}
	}

	/**
	 * Palauttaa <code>true</code>, jos taulukon solua voi muokata.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 * @return <code>true</code>, jos taulukon solua voi muokata
	 */
	public boolean isCellEditable(int row, int col) {
		return row >= 3 && col == 0;
	}

	/**
	 * Asettaa solun arvon.
	 * 
	 * @param value arvo
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public void setValueAt(Object value, int row, int col) {
		if (col == 0) {
			if (value == null) {
				model.setAccountId(row, -1);
			}
			else {
				model.setAccountId(row, (Integer)value);
			}
			
			fireTableDataChanged();
		}
		else if (col == 1) {
			model.setOldVatIndex(row, VATUtil.VAT_RATE_V2M[(Integer)value]);
			fireTableRowsUpdated(row, row);
		}
		else if (col == 2) {
			model.setNewVatIndex(row, VATUtil.VAT_RATE_V2M[(Integer)value]);
			fireTableRowsUpdated(row, row);
		}
	}
}