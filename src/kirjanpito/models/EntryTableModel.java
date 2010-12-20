package kirjanpito.models;

import java.awt.event.KeyEvent;
import java.math.BigDecimal;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.Entry;
import kirjanpito.ui.CurrencyCellEditor;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * vientien tiedot.
 * 
 * @author Tommi Helineva
 */
public class EntryTableModel extends AbstractTableModel {
	private DocumentModel model;
	private CurrencyCellEditor currencyCellEditor;
	
	private static final long serialVersionUID = 1L;
	
	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Tili", "Debet", "Kredit", "ALV", "Selite"
	};
	
	public EntryTableModel(DocumentModel model) {
		setModel(model);
	}

	public DocumentModel getModel() {
		return model;
	}

	public void setModel(DocumentModel model) {
		this.model = model;
	}

	public CurrencyCellEditor getCurrencyCellEditor() {
		return currencyCellEditor;
	}

	public void setCurrencyCellEditor(CurrencyCellEditor currencyCellEditor) {
		this.currencyCellEditor = currencyCellEditor;
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
	 * Palauttaa vientien lukumäärän.
	 * 
	 * @return vientien lukumäärä
	 */
	public int getRowCount() {
		return (model == null) ? 0 : model.getEntryCount();
	}

	/**
	 * Palauttaa solun arvon.
	 * 
	 * @param row rivin indeksi
	 * @param col sarakkeen indeksi
	 */
	public Object getValueAt(int row, int col) {
		Object value = null;
		Entry entry = model.getEntry(row);
		
		if (col == 0) {
			value = entry.getAccountId();
		}
		else if (col == 1) {
			value = entry.isDebit() ? model.getVatIncludedAmount(row) : null;
		}
		else if (col == 2) {
			value = !entry.isDebit() ? model.getVatIncludedAmount(row) : null;
		}
		else if (col == 3) {
			value = model.getVatAmount(row);
		}
		else if (col == 4) {
			value = entry.getDescription();
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
		if (col == 3) return false;
		return model.isEditable();
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
				model.updateAccountId(row, -1);
			}
			else {
				model.updateAccountId(row, (Integer)value);
			}
		}
		else if (col == 1) {
			boolean vatEntries = (currencyCellEditor.getLastModifiers() & KeyEvent.SHIFT_DOWN_MASK) == 0;
			Entry entry = model.getEntry(row);
			entry.setDebit(true);
			model.updateAmount(row, (BigDecimal)value, vatEntries);
			currencyCellEditor.setLastModifiers(0);
		}
		else if (col == 2) {
			boolean vatEntries = (currencyCellEditor.getLastModifiers() & KeyEvent.SHIFT_DOWN_MASK) == 0;
			Entry entry = model.getEntry(row);
			entry.setDebit(false);
			model.updateAmount(row, (BigDecimal)value, vatEntries);
			currencyCellEditor.setLastModifiers(0);
		}
		else if (col == 4) {
			Entry entry = model.getEntry(row);
			entry.setDescription((String)value);
		}
		
		model.setDocumentChanged();
		fireTableRowsUpdated(row, row);
	}
}
