package kirjanpito.models;

import java.math.BigDecimal;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.EntryTemplate;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * vientimallien tiedot.
 * 
 * @author Tommi Helineva
 */
public class EntryTemplateTableModel extends AbstractTableModel {
	private EntryTemplateModel model;
	
	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Nro", "Nimi", "Tili", "Debet", "Kredit", "Selite"
	};
	
	private static final long serialVersionUID = 1L;

	public EntryTemplateModel getModel() {
		return model;
	}

	public void setModel(EntryTemplateModel model) {
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
		return model.getEntryTemplateCount();
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
		EntryTemplate template = model.getEntryTemplate(row);
		
		if (col == 0) {
			return template.getNumber();
		}
		else if (col == 1) {
			return template.getName();
		}
		else if (col == 2) {
			return row;
		}
		else if (col == 3) {
			return template.isDebit() ? template.getAmount() : null;
		}
		else if (col == 4) {
			return !template.isDebit() ? template.getAmount() : null;
		}
		else if (col == 5) {
			return template.getDescription();
		}
		else if (col == -1) {
			return template.getAccountId();
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
		return true;
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
			try {
				model.updateNumber(row, Integer.parseInt(value.toString()));
			}
			catch (NumberFormatException e) { }
			
			fireTableDataChanged();
			return;
		}
		else if (col == 1) {
			model.updateName(row, value.toString());
			fireTableDataChanged();
			return;
		}
		else if (col == 2) {
			if (value == null) value = -1;
			model.updateAccountId(row, (Integer)value);
		}
		else if (col == 3) {
			model.updateAmount(row, true, (BigDecimal)value);
		}
		else if (col == 4) {
			model.updateAmount(row, false, (BigDecimal)value);
		}
		else if (col == 5) {
			model.updateDescription(row, value.toString());
		}
		
		fireTableRowsUpdated(row, row);
	}
}
