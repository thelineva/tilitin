package kirjanpito.reports;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class VATReportPrint extends Print {
	private VATReportModel model;
	private NumberFormat numberFormat;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;

	public VATReportPrint(VATReportModel model) {
		this.model = model;
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		numRowsPerPage = -1;
		setPrintId("vatReport");
	}

	public String getTitle() {
		return "ALV-laskelma tileittäin";
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

		columns = new int[5];
		columns[0] = getMargins().left;
		columns[1] = columns[0] + 40;
		columns[2] = columns[0] + 310;
		columns[3] = columns[0] + 380;
		columns[4] = columns[0] + 500;
	}

	public String getVariableValue(String name) {
		if (name.equals("1")) {
			return dateFormat.format(model.getStartDate()) +
				" - " + dateFormat.format(model.getEndDate());
		}

		return super.getVariableValue(name);
	}

	protected void printHeader() {
		super.printHeader();

		/* Tulostetaan sarakeotsikot. */
		setBoldStyle();
		y = margins.top + super.getHeaderHeight() + 12;
		setX(columns[0]);
		drawText("Nro");
		setX(columns[1]);
		drawText("Tili");
		setX(columns[2]);
		drawTextRight("Veron peruste");
		setX(columns[3]);
		drawTextRight("Vero");
		setX(columns[4]);
		drawTextRight("Verollinen summa");
		setY(getY() + 6);
		drawHorizontalLine(2.0f);
	}

	protected int getHeaderHeight() {
		return super.getHeaderHeight() + 20;
	}

	protected void printContent() {
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(model.getRowCount(), offset + numRowsPerPage);
		setNormalStyle();
		y += 17;

		for (int i = offset; i < numRows; i++) {
			if (model.getType(i) == 1) {
				String vatExcludedTotal = numberFormat.format(model.getVatExcludedTotal(i));

				setX(columns[0]);
				drawText(model.getAccount(i).getNumber());

				setX(columns[1]);
				drawText(cutString(model.getAccount(i).getName(),
						columns[2] - columns[1] - stringWidth(vatExcludedTotal) - 10));

				setX(columns[2]);
				drawTextRight(vatExcludedTotal);

				setX(columns[3]);
				drawTextRight(numberFormat.format(model.getVatAmountTotal(i)));

				setX(columns[4]);
				drawTextRight(numberFormat.format(model.getVatIncludedTotal(i)));
			}
			else if (model.getType(i) == 2) {
				setX(columns[1]);
				setBoldStyle();
				drawText(model.getText(i));
				setNormalStyle();
			}
			else if (model.getType(i) == 3) {
				setX(columns[1]);
				setBoldStyle();
				drawText(model.getText(i));
				setNormalStyle();

				setX(columns[2]);
				drawTextRight(numberFormat.format(model.getVatExcludedTotal(i)));

				setX(columns[3]);
				drawTextRight(numberFormat.format(model.getVatAmountTotal(i)));

				setX(columns[4]);
				drawTextRight(numberFormat.format(model.getVatIncludedTotal(i)));
			}
			else if (model.getType(i) == 4 || model.getType(i) == 5) {
				setX(columns[1]);

				if (model.getType(i) == 5) {
					setBoldStyle();
				}
				else {
					setNormalStyle();
				}

				drawText(model.getText(i));
				setNormalStyle();

				setX(columns[3]);
				drawTextRight(numberFormat.format(model.getVatAmountTotal(i)));
			}

			setY(getY() + 17);
		}
	}
}
