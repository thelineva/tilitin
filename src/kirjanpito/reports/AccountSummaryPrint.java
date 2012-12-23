package kirjanpito.reports;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import kirjanpito.db.Account;
import kirjanpito.db.Period;

/**
 * Tilien saldot -tuloste.
 *
 * @author Tommi Helineva
 */
public class AccountSummaryPrint extends Print {
	private AccountSummaryModel model;
	private NumberFormat numberFormat;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;
	private boolean printStartDate;

	public AccountSummaryPrint(AccountSummaryModel model, boolean printStartDate) {
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		numRowsPerPage = -1;
		this.model = model;
		this.printStartDate = printStartDate;
		setPrintId("accountSummary");
	}

	public String getTitle() {
		return "Tilien saldot";
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

		columns = new int[4];
		columns[0] = getMargins().left;
		columns[1] = columns[0] + 50;
		columns[2] = columns[0] + 360;
		columns[3] = columns[0] + 460;
	}

	protected void printHeader() {
		super.printHeader();

		y = margins.top + super.getHeaderHeight();
		drawHorizontalLine(2.0f);

		if (model.isPreviousPeriodVisible()) {
			setBoldStyle();
			setY(getY() + 30);
			setX(columns[2]);

			if (printStartDate) {
				drawTextRight(dateFormat.format(model.getStartDate()) + "-");
			}

			y += 14;
			drawTextRight(dateFormat.format(model.getEndDate()));
			y -= 14;
			setX(columns[3]);
			Period periodPrev = model.getPreviousPeriod();

			if (printStartDate) {
				drawTextRight(dateFormat.format(periodPrev.getStartDate()) + "-");
			}

			setY(getY() + 14);
			drawTextRight(dateFormat.format(periodPrev.getEndDate()));
		}
	}

	public String getVariableValue(String name) {
		if (name.equals("1")) {
			return dateFormat.format(model.getEndDate());
		}

		return super.getVariableValue(name);
	}

	protected int getHeaderHeight() {
		return super.getHeaderHeight() + (model.isPreviousPeriodVisible() ? 40 : 5);
	}

	protected void printContent() {
		String text;
		Account account;
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(model.getRowCount(), offset + numRowsPerPage);
		int xo;

		setNormalStyle();
		y += 17;

		for (int i = offset; i < numRows; i++) {
			text = model.getText(i);
			xo = 15 * model.getLevel(i);

			if (text == null) {
				account = model.getAccount(i);
				setX(columns[0] + xo);
				drawText(account.getNumber());

				/* Lasketaan tilin nimen ensimmäispituus. */
				BigDecimal balance = model.getBalance(i);
				String balanceString = (balance == null) ? "" : numberFormat.format(balance);
				int w = 275 - xo - stringWidth(balanceString);

				setX(columns[1] + xo);
				drawText(cutString(account.getName(), w));
				setX(columns[2]);
				drawTextRight(balanceString);

				setX(columns[3]);
				balance = model.getBalancePrev(i);

				if (balance != null) {
					drawTextRight(numberFormat.format(balance));
				}
			}
			else {
				setX(columns[0] + xo);
				setBoldStyle();
				drawText(text);
				setNormalStyle();
			}

			setY(getY() + 17);
		}
	}
}
