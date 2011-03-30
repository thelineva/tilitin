package kirjanpito.models;

import java.util.Date;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.Period;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * tilikausien tiedot.
 * 
 * @author Tommi Helineva
 */
public class PeriodTableModel extends AbstractTableModel {
	private PropertiesModel model;
	
	private static final long serialVersionUID = 1L;

	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"", "Alkaa", "Päättyy"
	};
	
	public PeriodTableModel(PropertiesModel model) {
		setModel(model);
	}

	/**
	 * Palauttaa mallin, josta tiedot haetaan.
	 * 
	 * @return malli
	 */
	public PropertiesModel getModel() {
		return model;
	}

	/**
	 * Asettaa mallin, josta tiedot haetaan.
	 * 
	 * @param model malli
	 */
	public void setModel(PropertiesModel model) {
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
		return (model == null) ? 0 : model.getPeriodCount();
	}
	
	/**
	 * Palauttaa sarakkeen <code>col</code> tietotyypin.
	 */
	public Class<?> getColumnClass(int col) {
		if (col == 0) {
			return Boolean.class;
		}
		else {
			return Date.class;
		}
	}

	/**
	 * Palauttaa solun arvon.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public Object getValueAt(int row, int col) {
		Object value = null;
		Period period = model.getPeriod(row);
		
		if (col == 0) {
			value = Boolean.valueOf(row == model.getCurrentPeriodIndex());
		}
		else if (col == 1) {
			value = period.getStartDate();
		}
		else if (col == 2) {
			value = period.getEndDate();
		}
		
		return value;
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
	 * Palauttaa <code>true</code>, jos taulukon solua voi muokata.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 * @return <code>true</code>, jos taulukon solua voi muokata
	 */
	public boolean isCellEditable(int row, int col) {
		Period period = model.getPeriod(row);
		return col == 0 || !period.isLocked();
	}

	/**
	 * Asettaa solun arvon.
	 * 
	 * @param value arvo
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public void setValueAt(Object value, int row, int col) {
		Period period = model.getPeriod(row);
		
		if (col == 0) {
			model.setCurrentPeriodIndex(row);
		}
		if (col == 1) {
			period.setStartDate((Date)value);
		}
		else if (col == 2) {
			period.setEndDate((Date)value);
		}
		
		model.updatePeriod(row);
		fireTableDataChanged();
	}
}
