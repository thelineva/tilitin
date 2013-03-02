package kirjanpito.reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import kirjanpito.util.AccountBalances;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ODFSpreadsheet;
import kirjanpito.util.Registry;

/**
 * Malli pääkirjatulosteelle.
 *
 * @author Tommi Helineva
 */
public class GeneralLedgerModel implements PrintModel {
	protected Registry registry;
	protected Period period;
	protected Date startDate;
	protected Date endDate;
	protected int orderBy;
	protected Settings settings;
	protected List<GeneralLedgerRow> rows;
	protected int lastDocumentNumber;
	protected BigDecimal totalDebit;
	protected BigDecimal totalCredit;
	protected boolean totalAmountVisible;
	private int prevAccountId;

	public static final int ORDER_BY_NUMBER = 3; // EntryDAO.ORDER_BY_ACCOUNT_NUMBER_AND_DOCUMENT_NUMBER
	public static final int ORDER_BY_DATE = 4; // EntryDAO.ORDER_BY_ACCOUNT_NUMBER_AND_DOCUMENT_DATE

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

		final AccountBalances balances = new AccountBalances(registry.getAccounts());

		settings = registry.getSettings();
		prevAccountId = -1;
		rows = new ArrayList<GeneralLedgerRow>();
		lastDocumentNumber = 0;
		totalDebit = BigDecimal.ZERO;
		totalCredit = BigDecimal.ZERO;

		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(sess).getByPeriodId(period.getId(), 0);

			for (Document d : documents) {
				documentMap.put(d.getId(), d);
			}

			dataSource.getEntryDAO(sess).getByPeriodId(
				period.getId(), orderBy,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());
						Document document = documentMap.get(entry.getDocumentId());

						if (account == null || document == null) {
							return;
						}

						balances.addEntry(entry);

						if (document.getDate().before(startDate) || document.getDate().after(endDate)) {
							return;
						}

						if (document.getNumber() >= 1) {
							if (entry.isDebit()) {
								totalDebit = totalDebit.add(entry.getAmount());
							}
							else {
								totalCredit = totalCredit.add(entry.getAmount());
							}
						}

						if (account.getType() == Account.TYPE_PROFIT_PREV) {
							if (prevAccountId != -1)
								rows.add(new GeneralLedgerRow(0, null, null, null, null, null));

							rows.add(new GeneralLedgerRow(4, document, null, account, null,
									balances.getBalance(entry.getAccountId())));
							return;
						}

						if (prevAccountId != account.getId()) {
							if (prevAccountId != -1)
								rows.add(new GeneralLedgerRow(0, null, null, null, null, null));

							rows.add(new GeneralLedgerRow(2, null, null, account, null, null));
						}

						lastDocumentNumber = Math.max(lastDocumentNumber, document.getNumber());
						rows.add(new GeneralLedgerRow(1, document, null, account, entry,
								balances.getBalance(entry.getAccountId())));

						prevAccountId = account.getId();
					}
				});
		}
		finally {
			if (sess != null) sess.close();
		}

		addProfitRow(balances.getProfit());

		if (totalAmountVisible) {
			rows.add(new GeneralLedgerRow(0, null, null, null, null, null));
			rows.add(new GeneralLedgerRow(5, null, null, null, null, null));
		}
	}

	/**
	 * Lisää tilikauden voitto/tappio -rivin.
	 *
	 * @param profit tilikauden voitto/tappio
	 */
	protected void addProfitRow(BigDecimal profit) {
		ArrayList<Account> profitAccounts = new ArrayList<Account>();

		for (Account account : registry.getAccounts()) {
			if (account.getType() == Account.TYPE_PROFIT) {
				profitAccounts.add(account);
			}
		}

		for (int i = 0; i < rows.size(); i++) {
			GeneralLedgerRow row = rows.get(i);

			if (row.type != 2) {
				continue;
			}

			Iterator<Account> iter = profitAccounts.iterator();

			while (iter.hasNext()) {
				Account profitAccount = iter.next();

				if (profitAccount.getNumber().compareTo(row.account.getNumber()) < 0) {
					rows.add(i, new GeneralLedgerRow(0, null, null, null, null, null));
					rows.add(i, new GeneralLedgerRow(4, null, null, profitAccount, null, profit));
					iter.remove();
				}
			}

			if (profitAccounts.isEmpty()) {
				break;
			}
		}

		for (Account profitAccount : profitAccounts) {
			rows.add(new GeneralLedgerRow(0, null, null, null, null, null));
			rows.add(new GeneralLedgerRow(4, null, null, profitAccount, null, profit));
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
			writer.writeField("Pääkirja tositelajeittain");
		}
		else {
			writer.writeField("Pääkirja");
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

		writer.writeField("Tilinumero");
		writer.writeField("Tilin nimi");
		writer.writeField("Tositenumero");
		writer.writeField("Päivämäärä");
		writer.writeField("Debet");
		writer.writeField("Kredit");
		writer.writeField("Saldo");
		writer.writeField("Selite");
		writer.writeLine();

		for (GeneralLedgerRow row : rows) {
			if (row.type != 1 && row.type != 4) {
				continue;
			}

			if (documentTypes) {
				writer.writeField(row.documentType.getName());
			}

			writer.writeField(row.account.getNumber());
			writer.writeField(row.account.getName());

			if (row.document == null || row.document.getNumber() == 0) {
				writer.writeField("");
				writer.writeField("");
				writer.writeField("");
				writer.writeField("");
			}
			else {
				writer.writeField(Integer.toString(row.document.getNumber()));
				writer.writeField(dateFormat.format(row.document.getDate()));

				if (row.entry.isDebit()) {
					writer.writeField(numberFormat.format(row.entry.getAmount()));
					writer.writeField("");
				}
				else {
					writer.writeField("");
					writer.writeField(numberFormat.format(row.entry.getAmount()));
				}
			}

			writer.writeField(numberFormat.format(row.balance));

			if (row.type == 1) {
				writer.writeField(row.entry.getDescription());
			}

			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		s.setTitle(String.format("Pääkirja: %s - %s",
				dateFormat.format(startDate), dateFormat.format(endDate)));
		s.defineColumn("co1", "1.5cm");
		s.defineColumn("co2", "1.5cm");
		s.defineColumn("co3", "2.6cm");
		s.defineColumn("co4", "5cm");
		s.defineColumn("co5", "2.6cm");
		s.defineColumn("co6", "7cm");
		s.addTable("Pääkirja");
		s.addColumn("co1", "Default");
		s.addColumn("co2", "num0AlignLeft");
		s.addColumn("co3", "dateAlignLeft");
		s.addColumn("co5", "num2");
		s.addColumn("co5", "num2");
		s.addColumn("co5", "num2");
		s.addColumn("co6", "Default");

		s.addRow();
		s.setColSpan(4);
		s.writeTextCell("Tili", "bold");
		s.setColSpan(1);
		s.addRow();
		s.writeTextCell("", "boldBorderBottom");
		s.writeTextCell("Nro", "boldAlignRightBorderBottom");
		s.writeTextCell("Päivämäärä", "boldAlignRightBorderBottom");
		s.writeTextCell("Debet", "boldAlignRightBorderBottom");
		s.writeTextCell("Kredit", "boldAlignRightBorderBottom");
		s.writeTextCell("Saldo", "boldAlignRightBorderBottom");
		s.writeTextCell("Selite", "boldBorderBottom");

		for (GeneralLedgerRow row : rows) {
			s.addRow();

			if (row.type == 3) {
				s.addRow();
				s.writeTextCell(row.documentType.getName(), "bold");
			}
			else if (row.type == 2) {
				s.writeTextCell(row.account.getNumber());
				s.setColSpan(3);
				s.writeTextCell(row.account.getName());
				s.setColSpan(1);
			}
			else if (row.type == 1) {
				s.writeEmptyCell();

				if (row.document.getNumber() != 0) {
					s.writeFloatCell(row.document.getNumber(), "num0");
					s.writeDateCell(row.document.getDate(), "date");

					if (row.entry.isDebit()) {
						s.writeFloatCell(row.entry.getAmount(), "num2");
						s.writeTextCell("", "num2");
					}
					else {
						s.writeTextCell("", "num2");
						s.writeFloatCell(row.entry.getAmount(), "num2");
					}
				}
				else {
					s.writeTextCell("", "num0");
					s.writeTextCell("", "date");
					s.writeTextCell("", "num2");
					s.writeTextCell("", "num2");
				}

				s.writeFloatCell(row.balance, "num2");
				s.writeTextCell(row.entry.getDescription());
			}
			else if (row.type == 4) {
				s.writeTextCell(row.account.getNumber());
				s.setColSpan(3);
				s.writeTextCell(row.account.getName());
				s.setColSpan(1);
				s.writeEmptyCell();
				s.writeEmptyCell();
				s.writeEmptyCell();
				s.writeFloatCell(row.balance, "num2");
			}
			else if (row.type == 5) {
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
	 * @return tili
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
	 * Palauttaa rivillä <code>index</code> olevan tilin
	 * saldon.
	 *
	 * @param index rivinumero
	 * @return tilin saldo
	 */
	public BigDecimal getBalance(int index) {
		return rows.get(index).balance;
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

	protected class GeneralLedgerRow {
		public int type;
		public Document document;
		public DocumentType documentType;
		public Account account;
		public Entry entry;
		public BigDecimal balance;

		public GeneralLedgerRow(int type, Document document, DocumentType documentType,
				Account account, Entry entry, BigDecimal balance) {

			this.type = type;
			this.document = document;
			this.documentType = documentType;
			this.account = account;
			this.entry = entry;
			this.balance = balance;
		}
	}
}