package kirjanpito.reports;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import kirjanpito.db.Period;

/**
 * Tuloslaskelma ja tase.
 * 
 * @author Tommi Helineva
 */
public class FinancialStatementPrint extends Print {
	private FinancialStatementModel model;
	private String title;
	private DecimalFormat numberFormat;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;
	private boolean printStartDate;
	
	/**
	 * Luo tulosteen.
	 * 
	 * @param model malli
	 * @param title tulosteen otsikko
	 * @param printStartDate määrittää, tulostetaanko tilikauden alkamispäivä
	 */
	public FinancialStatementPrint(FinancialStatementModel model,
			String printId, String title, boolean printStartDate) {
		this.model = model;
		this.title = title;
		this.printStartDate = printStartDate;
		setPrintId(printId);
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		numRowsPerPage = -1;
	}
	
	public String getTitle() {
		return title;
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
			return printStartDate ? dateFormat.format(model.getStartDate()) +
					" – " + dateFormat.format(model.getEndDate()) : dateFormat.format(model.getEndDate());
		}
		
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
			
			columns = new int[4];
			columns[0] = margins.left;
			columns[1] = model.containsDetails() ? columns[0] + 38 : columns[0];
			columns[2] = columns[0] + Math.min(420, maxWidth + 130);
			columns[3] = columns[0] + Math.min(500, maxWidth + 210);
		}
		
		if (model.isPreviousPeriodVisible()) {
			setY(getY() + 20);
			setX(columns[2]);
			
			if (printStartDate) {
				drawTextRight(dateFormat.format(model.getStartDate()) + "–");
			}
			
			setY(getY() + 14);
			drawTextRight(dateFormat.format(model.getEndDate()));
			setY(getY() - 14);
			setX(columns[3]);
			Period periodPrev = model.getPreviousPeriod();
			
			if (printStartDate) {
				drawTextRight(dateFormat.format(periodPrev.getStartDate()) + "–");
			}
			
			setY(getY() + 14);
			drawTextRight(dateFormat.format(periodPrev.getEndDate()));
		}
	}
	
	protected int getHeaderHeight() {
		return super.getHeaderHeight() + (model.isPreviousPeriodVisible() ? 40 : 5);
	}

	protected void printContent() {
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(model.getRowCount(), offset + numRowsPerPage);
		BigDecimal amount, amountPrev;
		
		setNormalStyle();
		y += 17;
		
		for (int i = offset; i < numRows; i++) {
			if (model.getStyle(i) == FinancialStatementModel.STYLE_BOLD)
				setBoldStyle();
			else if (model.getStyle(i) == FinancialStatementModel.STYLE_ITALIC)
				setItalicStyle();
			
			String number = model.getNumber(i);
			
			if (number != null) {
				setX(columns[0]);
				drawText(number);
			}
			
			setX(columns[1] + model.getLevel(i) * 12);
			drawText(model.getText(i));
			
			setNormalStyle();
			amount = model.getAmount(i);
			amountPrev = model.getAmountPrev(i);
			
			if (amount != null) {
				setX(columns[2]);
				drawTextRight(numberFormat.format(amount));
			}
			
			if (amountPrev != null) {
				setX(columns[3]);
				drawTextRight(numberFormat.format(amountPrev));
			}
			
			setY(getY() + 17);
		}
	}
}
