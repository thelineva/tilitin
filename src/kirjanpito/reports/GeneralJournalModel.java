package kirjanpito.reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ODFSpreadsheet;
import kirjanpito.util.Registry;

/**
 * Malli päiväkirjatulosteelle.
 *
 * @author Tommi Helineva
 */
public class GeneralJournalModel implements PrintModel {
	protected Registry registry;
	protected Settings settings;
	protected Period period;
	protected Date startDate;
	protected Date endDate;
	protected int orderBy;
	protected List<GeneralJournalRow> rows;
	protected int lastDocumentNumber;
	protected BigDecimal totalDebit;
	protected BigDecimal totalCredit;
	protected boolean totalAmountVisible;
	private int prevDocumentId;

	public static final int ORDER_BY_NUMBER = 1; // EntryDAO.ORDER_BY_DOCUMENT_NUMBER
	public static final int ORDER_BY_DATE = 2; // EntryDAO.ORDER_BY_DOCUMENT_DATE

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Palauttaa tilikauden.
	 *
	 * @return tilikausi
	 */
	public Period getPeriod() {
		return period;
	}

	/**
	 * Asettaa tilikauden.
	 *
	 * @param period tilikausi
	 */
	public void setPeriod(Period period) {
		this.period = period;
	}

	/**
	 * Palauttaa alkamispäivämäärän.
	 *
	 * @return alkamispäivämäärä
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Asettaa alkamispäivämäärän.
	 *
	 * @param startDate alkamispäivämäärä
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * Palauttaa päättymispäivämäärän.
	 *
	 * @return päättymispäivämäärä
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * Asettaa päättymispäivämäärän.
	 *
	 * @param endDate päättymispäivämäärä
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * Palauttaa vientien järjestyksen.
	 *
	 * @return ORDER_BY_NUMBER tai ORDER_BY_DATE
	 */
	public int getOrderBy() {
		return orderBy;
	}

	/**
	 * Asettaa vientien järjestyksen.
	 *
	 * @param orderBy ORDER_BY_NUMBER tai ORDER_BY_DATE
	 */
	public void setOrderBy(int orderBy) {
		this.orderBy = orderBy;
	}

	public boolean isTotalAmountVisible() {
		return totalAmountVisible;
	}

	public void setTotalAmountVisible(boolean totalAmountVisible) {
		this.totalAmountVisible = totalAmountVisible;
	}

	public void run() throws DataAccessException {
		List<Document> documents;
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		final HashMap<Integer, Document> documentMap =
			new HashMap<Integer, Document>();

		settings = registry.getSettings();
		prevDocumentId = -1;
		rows = new ArrayList<GeneralJournalRow>();
		lastDocumentNumber = 0;
		totalDebit = BigDecimal.ZERO;
		totalCredit = BigDecimal.ZERO;

		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(
					sess).getByPeriodIdAndDate(period.getId(), startDate, endDate);

			for (Document d : documents) {
				if (d.getNumber() >= 1) {
					documentMap.put(d.getId(), d);
				}
			}

			documents = null;
			dataSource.getEntryDAO(sess).getByPeriodId(
				period.getId(), orderBy,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());
						Document document = documentMap.get(entry.getDocumentId());

						if (account == null || document == null) {
							return;
						}

						if (prevDocumentId != document.getId()) {
							lastDocumentNumber = Math.max(lastDocumentNumber, document.getNumber());
							rows.add(new GeneralJournalRow(2, document, null, null, null));
						}

						if (entry.isDebit()) {
							totalDebit = totalDebit.add(entry.getAmount());
						}
						else {
							totalCredit = totalCredit.add(entry.getAmount());
						}

						rows.add(new GeneralJournalRow(1, document, null, account, entry));
						prevDocumentId = document.getId();
					}
				});
		}
		finally {
			if (sess != null) sess.close();
		}

		if (totalAmountVisible) {
			rows.add(new GeneralJournalRow(0, null, null, null, null));
			rows.add(new GeneralJournalRow(4, null, null, null, null));
		}
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		writeCSV(writer, false);
	}

	protected void writeCSV(CSVWriter writer, boolean documentTypes) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		DecimalFormat numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);

		if (documentTypes) {
			writer.writeField("Päiväkirja tositelajeittain");
		}
		else {
			writer.writeField("Päiväkirja");
		}

		writer.writeLine();
		writer.writeField("Nimi");
		writer.writeField(settings.getName());
		writer.writeLine();
		writer.writeField("Y-tunnus");
		writer.writeField(settings.getBusinessId());
		writer.writeLine();
		writer.writeField("Alkaa");
		writer.writeField(dateFormat.format(period.getStartDate()));
		writer.writeLine();
		writer.writeField("Päättyy");
		writer.writeField(dateFormat.format(period.getEndDate()));
		writer.writeLine();
		writer.writeLine();

		if (documentTypes) {
			writer.writeField("Tositelaji");
		}

		writer.writeField("Tositenumero");
		writer.writeField("Päivämäärä");
		writer.writeField("Tilinumero");
		writer.writeField("Tilin nimi");
		writer.writeField("Debet");
		writer.writeField("Kredit");
		writer.writeField("Selite");
		writer.writeLine();

		for (GeneralJournalRow row : rows) {
			if (row.type != 1) {
				continue;
			}

			if (documentTypes) {
				writer.writeField(row.documentType.getName());
			}

			writer.writeField(Integer.toString(row.document.getNumber()));
			writer.writeField(dateFormat.format(row.document.getDate()));
			writer.writeField(row.account.getNumber());
			writer.writeField(row.account.getName());

			if (row.entry.isDebit()) {
				writer.writeField(numberFormat.format(row.entry.getAmount()));
				writer.writeField("");
			}
			else {
				writer.writeField("");
				writer.writeField(numberFormat.format(row.entry.getAmount()));
			}

			writer.writeField(row.entry.getDescription());
			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		s.setTitle(String.format("Päiväkirja: %s - %s",
				dateFormat.format(startDate), dateFormat.format(endDate)));
		s.defineColumn("co1", "1.5cm");
		s.defineColumn("co2", "0.8cm");
		s.defineColumn("co3", "1.5cm");
		s.defineColumn("co4", "5cm");
		s.defineColumn("co5", "2.6cm");
		s.defineColumn("co6", "7cm");
		s.addTable("Päiväkirja");
		s.addColumn("co1", "num0AlignLeft");
		s.addColumn("co2", "dateAlignLeft");
		s.addColumn("co3", "Default");
		s.addColumn("co4", "Default");
		s.addColumn("co5", "num2");
		s.addColumn("co5", "num2");
		s.addColumn("co6", "Default");

		s.addRow();
		s.writeTextCell("Nro", "bold");
		s.writeTextCell("Päivämäärä", "bold");
		s.addRow();
		s.writeTextCell("", "boldBorderBottom");
		s.writeTextCell("", "boldBorderBottom");
		s.setColSpan(2);
		s.writeTextCell("Tili", "boldBorderBottom");
		s.setColSpan(1);
		s.writeTextCell("", "boldBorderBottom");
		s.writeTextCell("Debet", "boldAlignRightBorderBottom");
		s.writeTextCell("Kredit", "boldAlignRightBorderBottom");
		s.writeTextCell("Selite", "boldBorderBottom");

		for (GeneralJournalRow row : rows) {
			s.addRow();

			if (row.type == 3) {
				s.addRow();
				s.writeTextCell(row.documentType.getName(), "bold");
			}
			else if (row.type == 2) {
				s.writeTextCell(Integer.toString(row.document.getNumber()), "num0AlignLeft");
				s.setColSpan(3);
				s.writeDateCell(row.document.getDate(), "dateAlignLeft");
				s.setColSpan(1);
			}
			else if (row.type == 1) {
				s.writeEmptyCell();
				s.writeEmptyCell();
				s.writeTextCell(row.account.getNumber());
				s.writeTextCell(row.account.getName());

				if (row.entry.isDebit()) {
					s.writeFloatCell(row.entry.getAmount(), "num2");
					s.writeTextCell("", "num2");
				}
				else {
					s.writeTextCell("", "num2");
					s.writeFloatCell(row.entry.getAmount(), "num2");
				}

				s.writeTextCell(row.entry.getDescription());
			}
			else if (row.type == 4) {
				s.writeEmptyCell();
				s.writeEmptyCell();
				s.writeEmptyCell();
				s.writeEmptyCell();
				s.writeFloatCell(totalDebit, "num2Bold");
				s.writeFloatCell(totalCredit, "num2Bold");
			}
		}
	}

	/**
	 * Palauttaa tulosteessa olevien rivien lukumäärän.
	 *
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return rows.size();
	}

	/**
	 * Palauttaa rivin <code>index</code> tyypin.
	 *
	 * @param index rivinumero
	 * @return tyyppi
	 */
	public int getType(int index) {
		return rows.get(index).type;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tositteen.
	 *
	 * @param index rivinumero
	 * @return tosite
	 */
	public Document getDocument(int index) {
		return rows.get(index).document;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin.
	 *
	 * @param index rivinumero
	 * @return tosite
	 */
	public Account getAccount(int index) {
		return rows.get(index).account;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan viennin.
	 *
	 * @param index rivinumero
	 * @return vienti
	 */
	public Entry getEntry(int index) {
		return rows.get(index).entry;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tositteen
	 * tositelajin
	 *
	 * @param index rivinumero
	 * @return tositelaji
	 */
	public DocumentType getDocumentType(int index) {
		return rows.get(index).documentType;
	}

	/**
	 * Palauttaa viimeisen tositteen numeron.
	 *
	 * @return viimeisen tositteen numero
	 */
	public int getLastDocumentNumber() {
		return lastDocumentNumber;
	}

	/**
	 * Palauttaa debet-vientien summan.
	 *
	 * @return debet-vientien summa
	 */
	public BigDecimal getTotalDebit() {
		return totalDebit;
	}

	/**
	 * Palauttaa kredit-vientien summan.
	 *
	 * @return kredit-vientien summa
	 */
	public BigDecimal getTotalCredit() {
		return totalCredit;
	}

	protected class GeneralJournalRow {
		public int type;
		public Document document;
		public DocumentType documentType;
		public Account account;
		public Entry entry;

		public GeneralJournalRow(int type, Document document, DocumentType documentType,
				Account account, Entry entry) {

			this.type = type;
			this.document = document;
			this.documentType = documentType;
			this.account = account;
			this.entry = entry;
		}
	}
}
