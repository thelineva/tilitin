package kirjanpito.reports;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import kirjanpito.db.Document;
import kirjanpito.db.Entry;

public class DocumentPrint extends Print {
	private DocumentPrintModel model;
	private NumberFormat numberFormat;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;
	
	public DocumentPrint(DocumentPrintModel model) {
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		this.model = model;
		setPrintId("documentPrint");
	}
	
	public String getTitle() {
		return "Tosite";
	}

	public int getPageCount() {
		return pageCount;
	}
	
	public void initialize() {
		super.initialize();
		numRowsPerPage = (getContentHeight() - 7) / 15;
		
		if (numRowsPerPage > 0) {
			pageCount = (int)Math.ceil(model.getEntryCount() / (double)numRowsPerPage);
			pageCount = Math.max(1, pageCount); /* Vähintään yksi sivu. */
		}
		else {
			pageCount = 1;
		}
		
		columns = new int[5];
		columns[0] = getMargins().left;
		columns[1] = columns[0] + 40;
		columns[2] = columns[0] + 225;
		columns[3] = columns[0] + 290;
		columns[4] = columns[0] + 300;
	}
	
	protected void printHeader() {
		super.printHeader();
		
		/* Tulostetaan tositteen tiedot. */
		setBoldStyle();
		y = margins.top + super.getHeaderHeight() + 12;
		setX(columns[0]);
		drawText("Tositenumero");
		y += 15;
		drawText("Päivämäärä");
		y -= 15;
		setX(140);
		
		Document document = model.getDocument();
		setNormalStyle();
		drawText(Integer.toString(document.getNumber()));
		y += 15;
		drawText(dateFormat.format(document.getDate()));
		
		/* Tulostetaan sarakeotsikot. */
		setY(getY() + 25);
		setBoldStyle();
		setX(columns[0]);
		drawText("Tili");
		setX(columns[2]);
		drawTextRight("Debet");
		setX(columns[3]);
		drawTextRight("Kredit");
		setX(columns[4]);
		drawText("Selite");
		setY(getY() + 6);
		drawHorizontalLine(2.0f);
	}
	
	protected int getHeaderHeight() {
		return super.getHeaderHeight() + 80;
	}

	protected void printContent() {
		Entry entry;
		String text;
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(model.getEntryCount(), offset + numRowsPerPage);
		setNormalStyle();
		
		for (int i = offset; i < numRows; i++) {
			entry = model.getEntry(i);
			setX(columns[0]);
			drawText(model.getAccount(i).getNumber());
			setX(columns[1]);
			text = model.getAccount(i).getName();

			/* Lasketaan tilin nimen enimmäispituus. */
			String amountString = numberFormat.format(entry.getAmount());
			int w = entry.isDebit() ? 145 - stringWidth(amountString) : 155;
			text = cutString(text, w);
			drawText(text);
			
			if (entry.isDebit()) {
				setX(columns[2]);
			}
			else {
				setX(columns[3]);
			}
			
			drawTextRight(amountString);
			setX(columns[4]);
			drawText(entry.getDescription());
			setY(getY() + 15);
		}
	}
}
