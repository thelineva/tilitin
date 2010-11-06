package kirjanpito.reports;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.VATUtil;

public class COAPrint extends Print {
	private COAPrintModel model;
	private ChartOfAccounts coa;
	private DateFormat dateFormat;
	private NumberFormat numberFormat;
	private Date now;
	private int numRowsPerPage;
	private int pageCount;
	
	public COAPrint(COAPrintModel model) {
		this.model = model;
		dateFormat = new SimpleDateFormat("d.M.yyyy");
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		now = new Date();
		numRowsPerPage = -1;
	}
	
	public String getTitle() {
		return "Tilikartta";
	}

	public int getPageCount() {
		return pageCount;
	}

	public void initialize() {
		int height = getContentHeight();
		numRowsPerPage = height / 14;
		coa = model.getChartOfAccounts();
		pageCount = (int)Math.ceil(coa.getSize() / (double)numRowsPerPage);
		pageCount = Math.max(1, pageCount); /* Vähintään yksi sivu. */
	}
	
	protected void printHeader() {
		/* Tulostetaan vasempaan reunaan nimi ja y-tunnus. */
		setNormalStyle();
		setY(getY() + 20);
		drawText(model.getSettings().getName());
		setY(getY() + 20);
		drawText(model.getSettings().getBusinessId());
		setY(getY() - 20);
		
		/* Tulostetaan oikeaan reunaan sivunumero ja päivämäärä. */
		setX(getPageWidth() - getMargins().right);
		drawTextRight("Sivu " + (getPageIndex() + 1));
		setY(getY() + 20);
		drawTextRight(dateFormat.format(now));
		
		/* Tulostetaan keskelle tulosteen otsikko. */
		setX(getPageWidth() / 2);
		setHeadingStyle();
		setY(getY() - 20);
		drawTextCenter(getTitle());
		
		setY(getY() + 30);
		drawHorizontalLine(2.0f);
	}
	
	protected int getHeaderHeight() {
		return 75;
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