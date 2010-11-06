package kirjanpito.models;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.DocumentType;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * tositelajien tiedot.
 * 
 * @author Tommi Helineva
 */
public class DocumentTypeTableModel extends AbstractTableModel {
	private DocumentTypeModel model;
	
	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Nro", "Nimi", "Alkaa", "Päättyy"
	};
	
	private static final long serialVersionUID = 1L;

	public DocumentTypeModel getModel() {
		return model;
	}

	public void setModel(DocumentTypeModel model) {
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
		return model.getDocumentTypeCount();
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
		DocumentType type = model.getDocumentType(row);
		
		if (col == 0) {
			return type.getNumber();
		}
		else if (col == 1) {
			return type.getName();
		}
		else if (col == 2) {
			return type.getNumberStart();
		}
		else if (col == 3) {
			return type.getNumberEnd();
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
		}
		else if (col == 2) {
			try {
				model.updateNumberStart(row, Integer.parseInt(value.toString()));
			}
			catch (NumberFormatException e) { }
		}
		else if (col == 3) {
			try {
				model.updateNumberEnd(row, Integer.parseInt(value.toString()));
			}
			catch (NumberFormatException e) { }
		}
		
		fireTableCellUpdated(row, col);
	}
}