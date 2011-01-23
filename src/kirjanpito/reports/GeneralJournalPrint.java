package kirjanpito.reports;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import kirjanpito.db.Document;
import kirjanpito.db.Entry;

/**
 * Päiväkirjatuloste.
 * 
 * @author Tommi Helineva
 */
public class GeneralJournalPrint extends Print {
	private GeneralJournalModel model;
	private NumberFormat numberFormat;
	private int[] columns;
	private int numRowsPerPage;
	private int pageCount;
	
	public GeneralJournalPrint(GeneralJournalModel model) {
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		this.model = model;
		setPrintId("generalJournal");
	}
	
	public String getTitle() {
		return "Päiväkirja";
	}

	public int getPageCount() {
		return pageCount;
	}
	
	public void initialize() {
		super.initialize();
		numRowsPerPage = (getContentHeight() - 7) / 13;
		
		if (numRowsPerPage > 0) {
			pageCount = (int)Math.ceil(model.getRowCount() / (double)numRowsPerPage);
			pageCount = Math.max(1, pageCount); /* Vähintään yksi sivu. */
		}
		else {
			pageCount = 1;
		}
		
		columns = new int[7];
		columns[0] = getMargins().left;
		columns[1] = columns[0] + 45;
		columns[2] = columns[0] + 55;
		columns[3] = columns[0] + 95;
		columns[4] = columns[0] + 250;
		columns[5] = columns[0] + 310;
		columns[6] = columns[0] + 325;
	}
	
	public String getVariableValue(String name) {
		if (name.equals("1")) {
			return dateFormat.format(model.getStartDate()) +
				" – " + dateFormat.format(model.getEndDate());
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
		drawText("Päivämäärä");
		setY(getY() + 13);
		
		setX(columns[2]);
		drawText("Tili");
		setX(columns[4]);
		drawTextRight("Debet");
		setX(columns[5]);
		drawTextRight("Kredit");
		setX(columns[6]);
		drawText("Selite");
		setY(getY() + 6);
		drawHorizontalLine(2.0f);
	}
	
	protected int getHeaderHeight() {
		return super.getHeaderHeight() + 35;
	}

	protected void printContent() {
		Document document;
		Entry entry;
		String text;
		int offset = getPageIndex() * numRowsPerPage;
		int numRows = Math.min(model.getRowCount(), offset + numRowsPerPage);
		setNormalStyle();
		y += 13;
		
		for (int i = offset; i < numRows; i++) {
			document = model.getDocument(i);
			int rowType = model.getType(i);
			
			if (rowType == 3) {
				setX(columns[1]);
				setBoldStyle();
				drawText(model.getDocumentType(i).getName());
				setNormalStyle();
			}
			else if (rowType == 2) {
				setX(columns[0]);
				drawText(Integer.toString(document.getNumber()));
				setX(columns[1]);
				drawText(dateFormat.format(document.getDate()));
			}
			else if (rowType == 1) {
				entry = model.getEntry(i);
				setX(columns[2]);
				drawText(model.getAccount(i).getNumber());
				setX(columns[3]);
				text = model.getAccount(i).getName();

				/* Lasketaan tilin nimen enimmäispituus. */
				String amountString = numberFormat.format(entry.getAmount());
				int w = entry.isDebit() ? 145 - stringWidth(amountString) : 155;
				text = cutString(text, w);
				drawText(text);
				
				if (entry.isDebit()) {
					setX(columns[4]);
				}
				else {
					setX(columns[5]);
				}
				
				drawTextRight(amountString);
				setX(columns[6]);
				drawText(entry.getDescription());
			}
			
			setY(getY() + 13);
		}
	}
}