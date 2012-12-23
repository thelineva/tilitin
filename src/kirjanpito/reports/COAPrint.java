package kirjanpito.reports;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.util.ChartOfAccounts;

public class COAPrint extends Print {
	private COAPrintModel model;
	private ChartOfAccounts coa;
	private DecimalFormat numberFormat;
	private DecimalFormat percentageFormat;
	private int numRowsPerPage;
	private int pageCount;

	public COAPrint(COAPrintModel model) {
		this.model = model;
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		percentageFormat = new DecimalFormat();
		percentageFormat.setMinimumFractionDigits(0);
		percentageFormat.setMaximumFractionDigits(2);
		numRowsPerPage = -1;
		setPrintId("chartOfAccounts");
	}

	public String getTitle() {
		return "Tilikartta";
	}

	public int getPageCount() {
		return pageCount;
	}

	public void initialize() {
		super.initialize();
		int height = getContentHeight();
		numRowsPerPage = height / 14;
		coa = model.getChartOfAccounts();
		pageCount = (int)Math.ceil(coa.getSize() / (double)numRowsPerPage);
		pageCount = Math.max(1, pageCount); /* Vähintään yksi sivu. */
	}

	protected void printHeader() {
		super.printHeader();
		y = margins.top + super.getHeaderHeight();
		drawHorizontalLine(2.0f);
	}

	protected int getHeaderHeight() {
		return super.getHeaderHeight() + 22;
	}

	protected void printContent() {
		Account account;
		COAHeading heading;
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(coa.getSize(), offset + numRowsPerPage);
		int leftMargin = getMargins().left;
		int accountX = leftMargin + model.getAccountLevel() * 15;
		setNormalStyle();

		for (int i = offset; i < numRows; i++) {
			if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
				account = coa.getAccount(i);
				setX(accountX);
				drawText(account.getNumber());
				setX(accountX + 50);
				drawText(account.getName());

				if (account.getVatRate().compareTo(BigDecimal.ZERO) > 0) {
					setX(400);
					drawText(String.format("ALV %s %%",
							percentageFormat.format(account.getVatRate())));
				}
			}
			else {
				heading = coa.getHeading(i);
				setX(leftMargin + heading.getLevel() * 15);
				setBoldStyle();
				drawText(heading.getText());
				setNormalStyle();
			}

			setY(getY() + 14);
		}
	}
}
