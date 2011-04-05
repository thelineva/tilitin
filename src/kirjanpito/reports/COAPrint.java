package kirjanpito.reports;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.VATUtil;

public class COAPrint extends Print {
	private COAPrintModel model;
	private ChartOfAccounts coa;
	private NumberFormat numberFormat;
	private int numRowsPerPage;
	private int pageCount;
	
	public COAPrint(COAPrintModel model) {
		this.model = model;
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
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
				int vatRate = account.getVatRate();
				
				if (vatRate > 0 && vatRate < VATUtil.VAT_RATE_M2V.length) {
					setX(400);
					drawText("ALV " + VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[vatRate]]);
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