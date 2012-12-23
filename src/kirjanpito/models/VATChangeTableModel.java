package kirjanpito.models;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import javax.swing.table.AbstractTableModel;

import kirjanpito.db.Account;

/**
 * <code>TableModel</code>in toteuttava luokka, joka sisältää
 * ALV-kannan muutossäännöt.
 *
 * @author Tommi Helineva
 */
public class VATChangeTableModel extends AbstractTableModel {
	private VATChangeModel model;
	private DecimalFormat formatter;

	/* Sarakkeiden otsikot */
	private static final String[] COLUMN_CAPTIONS = new String[] {
		"Tili", "Vanha", "Uusi", "Selite"
	};

	private static final long serialVersionUID = 1L;

	public VATChangeTableModel(VATChangeModel model) {
		this.model = model;
		this.formatter = new DecimalFormat();
		this.formatter.setMinimumFractionDigits(0);
		this.formatter.setMaximumFractionDigits(2);
	}

	public int getColumnCount() {
		return COLUMN_CAPTIONS.length;
	}

	public int getRowCount() {
		return model.getRuleCount();
	}

	public String getColumnName(int index) {
		return COLUMN_CAPTIONS[index];
	}

	public Object getValueAt(int row, int col) {
		if (col == 0) {
			return row;
		}
		else if (col == 1) {
			return model.getOldVatRate(row);
		}
		else if (col == 2) {
			return model.getNewVatRate(row);
		}
		else if (col == -1) {
			return model.getAccountId(row);
		}
		else {
			int accountId = model.getAccountId(row);
			BigDecimal oldVatRate = model.getOldVatRate(row);
			BigDecimal newVatRate = model.getNewVatRate(row);

			if (accountId < 0) {
				return String.format("Kopioidaan tilit, joiden ALV-kanta on %s %%. Uuden tilin ALV-kannaksi asetetaan %s %%",
						formatter.format(oldVatRate), formatter.format(newVatRate));
			}
			else {
				Account account = model.getAccount(row);
				return String.format("Kopioidaan tili %s %s. Uuden tilin ALV-kannaksi asetetaan %s %%",
						account.getNumber(), account.getName(),
						formatter.format(newVatRate));
			}
		}
	}

	public boolean isCellEditable(int row, int col) {
		if (row >= model.getRuleCount()) {
			return false;
		}

		if (col == 1) {
			return model.getAccountId(row) < 0;
		}

		return col < 3;
	}

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
			model.setOldVatRate(row, (BigDecimal)value);
			fireTableRowsUpdated(row, row);
		}
		else if (col == 2) {
			model.setNewVatRate(row, (BigDecimal)value);
			fireTableRowsUpdated(row, row);
		}
	}
}