package kirjanpito.reports;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import kirjanpito.db.Entry;

/**
 * Tiliotetuloste.
 *
 * @author Tommi Helineva
 */
public class AccountStatementPrint extends Print {
	private AccountStatementModel model;
	private NumberFormat numberFormat;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;

	public AccountStatementPrint(AccountStatementModel model) {
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		numRowsPerPage = -1;
		this.model = model;
		setPrintId("accountStatement");
	}

	public String getTitle() {
		return "Tiliote";
	}

	public int getPageCount() {
		return pageCount;
	}

	public void initialize() {
		super.initialize();
		numRowsPerPage = (getContentHeight() - 10) / 17;

		if (numRowsPerPage > 0) {
			pageCount = (int)Math.ceil(model.getRowCount() / (double)numRowsPerPage);
			pageCount = Math.max(1, pageCount); /* Vähintään yksi sivu. */
		}
		else {
			pageCount = 1;
		}

		columns = null;
	}

	public String getVariableValue(String name) {
		if (name.equals("1")) {
			return model.getAccount().getNumber() + " " + model.getAccount().getName();
		}
		else if (name.equals("2")) {
			return dateFormat.format(model.getStartDate()) + " - " +
				dateFormat.format(model.getEndDate());
		}

		return super.getVariableValue(name);
	}

	protected void printHeader() {
		super.printHeader();

		if (columns == null) {
			int numberColumnWidth = Math.max(30, stringWidth(
					Integer.toString(model.getLastDocumentNumber())) + 20);

			columns = new int[6];
			columns[0] = getMargins().left;
			columns[1] = columns[0] + numberColumnWidth;
			columns[2] = columns[1] + 125;
			columns[3] = columns[1] + 185;
			columns[4] = columns[1] + 260;
			columns[5] = columns[1] + 275;
		}

		/* Tulostetaan sarakeotsikot. */
		setBoldStyle();
		y = margins.top + super.getHeaderHeight() + 12;
		setX(columns[0]);
		drawText("Nro");
		setX(columns[1]);
		drawText("Päivämäärä");
		setX(columns[2]);
		drawTextRight("Debet");
		setX(columns[3]);
		drawTextRight("Kredit");
		setX(columns[4]);
		drawTextRight("Saldo");
		setX(columns[5]);
		drawText("Selite");
		setY(getY() + 6);
		drawHorizontalLine(2.0f);
	}

	protected int getHeaderHeight() {
		return super.getHeaderHeight() + 20;
	}

	protected void printContent() {
		Entry entry;
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(model.getRowCount(), offset + numRowsPerPage);
		int descriptionWidth = getPageWidth() - margins.right - columns[5];
		y += 17;

		for (int i = offset; i < numRows; i++) {
			entry = model.getEntry(i);
			setNormalStyle();

			if (model.getDocumentNumber(i) >= 0) {
				if (model.getDocumentNumber(i) >= 1) {
					setX(columns[0]);
					drawText(Integer.toString(model.getDocumentNumber(i)));
					setX(columns[1]);
					drawText(dateFormatTable.format(model.getDate(i)));

					if (entry.isDebit()) {
						setX(columns[2]);
					}
					else {
						setX(columns[3]);
					}

					drawTextRight(numberFormat.format(entry.getAmount()));
				}

				setX(columns[4]);
				drawTextRight(numberFormat.format(model.getBalance(i)));
				setX(columns[5]);

				setSmallStyle();
				drawText(cutString(entry.getDescription(), descriptionWidth));
			}
			else if (model.getEntryCount() > 0) {
				String text = (model.getEntryCount() == 1) ? "1 vienti" :
					model.getEntryCount() + " vientiä";

				setBoldStyle();
				setX(columns[0]);
				drawText(text);

				if (model.getDebitTotal().compareTo(BigDecimal.ZERO) != 0) {
					setX(columns[2]);
					drawTextRight(numberFormat.format(model.getDebitTotal()));
				}

				if (model.getCreditTotal().compareTo(BigDecimal.ZERO) != 0) {
					setX(columns[3]);
					drawTextRight(numberFormat.format(model.getCreditTotal()));
				}

				setX(columns[4]);
				drawTextRight(numberFormat.format(model.getBalance(i)));
			}

			setY(getY() + 17);
		}
	}
}
