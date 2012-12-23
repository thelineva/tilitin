package kirjanpito.reports;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Tuloslaskelma ja tase.
 *
 * @author Tommi Helineva
 */
public class FinancialStatementPrint extends Print {
	private FinancialStatementModel model;
	private DecimalFormat numberFormat;
	private ArrayList<Integer> rowMapping;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;

	/**
	 * Luo tulosteen.
	 *
	 * @param model malli
	 * @param title tulosteen otsikko
	 * @param printStartDate määrittää, tulostetaanko tilikauden alkamispäivä
	 */
	public FinancialStatementPrint(FinancialStatementModel model) {
		this.model = model;

		switch (model.getType()) {
		case FinancialStatementModel.TYPE_INCOME_STATEMENT:
			setPrintId("incomeStatement");
			break;

		case FinancialStatementModel.TYPE_INCOME_STATEMENT_DETAILED:
			setPrintId("incomeStatementDetailed");
			break;

		case FinancialStatementModel.TYPE_BALANCE_SHEET:
			setPrintId("balanceSheet");
			break;

		case FinancialStatementModel.TYPE_BALANCE_SHEET_DETAILED:
			setPrintId("balanceSheetDetailed");
			break;
		}

		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		numRowsPerPage = -1;
	}

	public String getTitle() {
		return model.getTitle();
	}

	public int getPageCount() {
		return pageCount;
	}

	public void initialize() {
		super.initialize();
		numRowsPerPage = Math.max(1, (getContentHeight() - 10) / 17);
		rowMapping = new ArrayList<Integer>();
		int rowsRemaining = numRowsPerPage;
		pageCount = 1;

		for (int i = 0; i < model.getRowCount(); i++) {
			if (rowsRemaining == 0) {
				pageCount++;
				rowsRemaining = numRowsPerPage;
			}

			if (model.getLevel(i) < 0) {
				while (rowsRemaining > 0) {
					rowMapping.add(-1);
					rowsRemaining--;
				}

				continue;
			}

			rowMapping.add(i);
			rowsRemaining--;
		}

		columns = null;
	}

	public String getVariableValue(String name) {
		return super.getVariableValue(name);
	}

	protected void printHeader() {
		super.printHeader();
		y = margins.top + super.getHeaderHeight();
		drawHorizontalLine(2.0f);
		setBoldStyle();

		if (columns == null) {
			int maxWidth = 0;

			for (int i = 0; i < model.getRowCount(); i++) {
				maxWidth = Math.max(maxWidth, stringWidth(model.getText(i)));
			}

			columns = new int[2 + model.getColumnCount()];
			columns[0] = margins.left;
			columns[1] = model.containsDetails() ? columns[0] + 38 : columns[0];

			int width = stringWidth("00.00.0000") + 20;
			int cx = getPageWidth() - margins.right - model.getColumnCount() * width;
			cx = Math.min(cx, columns[1] + maxWidth + 20);
			cx = Math.max(cx, getPageWidth() / 2);

			for (int i = 2; i < columns.length; i++) {
				cx += width;
				columns[i] = cx;
			}
		}

		setY(getY() + 20);
		int type = model.getType();
		Date[] startDates = model.getStartDates();
		Date[] endDates = model.getEndDates();

		for (int i = 0; i < model.getColumnCount(); i++) {
			setX(columns[2 + i]);

			if (type != FinancialStatementModel.TYPE_BALANCE_SHEET &&
					type != FinancialStatementModel.TYPE_BALANCE_SHEET_DETAILED) {
				drawTextRight(dateFormat.format(startDates[i]) + "-");
			}

			setY(getY() + 14);
			drawTextRight(dateFormat.format(endDates[i]));
			setY(getY() - 14);
		}
	}

	protected int getHeaderHeight() {
		return super.getHeaderHeight() + 40;
	}

	protected void printContent() {
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(rowMapping.size(), offset + numRowsPerPage);
		int colCount = model.getColumnCount();

		setNormalStyle();
		y += 17;

		for (int r = offset; r < numRows; r++) {
			int i = rowMapping.get(r);

			if (i < 0) {
				setY(getY() + 17);
				continue;
			}

			if (model.getStyle(i) == FinancialStatementModel.STYLE_BOLD)
				setBoldStyle();
			else if (model.getStyle(i) == FinancialStatementModel.STYLE_ITALIC)
				setItalicStyle();

			String number = model.getNumber(i);

			if (number != null) {
				setX(columns[0]);
				drawText(number);
			}

			int maxX = getPageWidth() - margins.right;

			for (int j = 0; j < colCount; j++) {
				BigDecimal amount = model.getAmount(i, j);

				if (amount != null) {
					maxX = columns[2 + j] - stringWidth(numberFormat.format(amount)) - 10;
					break;
				}
			}

			setX(columns[1] + model.getLevel(i) * 12);
			drawText(cutString(model.getText(i), maxX - getX()));
			setNormalStyle();

			for (int j = 0; j < colCount; j++) {
				setX(columns[2 + j]);
				BigDecimal amount = model.getAmount(i, j);

				if (amount != null) {
					drawTextRight(numberFormat.format(amount));
				}
			}

			setY(getY() + 17);
		}
	}
}
