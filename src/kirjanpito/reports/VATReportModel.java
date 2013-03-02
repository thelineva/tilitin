package kirjanpito.reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ODFSpreadsheet;

/**
 * Malli ALV-laskelmalle.
 *
 * @author Tommi Helineva
 */
public class VATReportModel implements PrintModel {
	private DataSource dataSource;
	private Settings settings;
	private Period period;
	private Date startDate;
	private Date endDate;
	private List<Account> accounts;
	private ArrayList<VATReportRow> rows;
	private BigDecimal totalVatAmount2; // Suoritettava ALV yhteensä
	private BigDecimal totalVatAmount2M;
	private BigDecimal totalVatAmount3; // Vähennettävä ALV yhteensä
	private BigDecimal totalVatAmount3M;
	private int documentId;

	private static final String[] VAT_CODES = {
		null, null, null, null,
		"Verollinen myynti", "Verolliset ostot",
		"Veroton myynti", "Verottomat ostot",
		"Yhteisömyynti", "Yhteisöostot",
		"Rakentamispalvelun myynti", "Rakentamispalvelun ostot"
	};

	/**
	 * Palauttaa tietokannan, josta tiedot haetaan.
	 *
	 * @return tietokanta
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Asettaa tietokannan, josta tiedot haetaan.
	 *
	 * @param dataSource tietokanta
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Palauttaa tilikauden, jonka saldot haetaan.
	 *
	 * @return tilikausi
	 */
	public Period getPeriod() {
		return period;
	}

	/**
	 * Asettaa tilikauden, jonka saldot haetaan.
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
	 * Palauttaa tilit.
	 *
	 * @return tilit
	 */
	public List<Account> getAccounts() {
		return accounts;
	}

	/**
	 * Asettaa tilit.
	 *
	 * @param accounts tilit
	 */
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}

	/**
	 * Palauttaa asetukset.
	 *
	 * @return asetukset
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Asettaa asetukset.
	 *
	 * @param settings asetukset
	 */
	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public void run() throws DataAccessException {
		Session sess = null;
		final AccountBalances balances = new AccountBalances(accounts);

		final HashMap<Integer, BigDecimal> vatAmounts =
			new HashMap<Integer, BigDecimal>();

		final HashMap<Integer, Entry> entryMap =
			new HashMap<Integer, Entry>();

		final HashMap<Integer, Account> accountMap =
			new HashMap<Integer, Account>();

		for (Account a : accounts) {
			accountMap.put(a.getId(), a);
		}

		documentId = -1;
		totalVatAmount2 = BigDecimal.ZERO;
		totalVatAmount3 = BigDecimal.ZERO;
		totalVatAmount2M = BigDecimal.ZERO;
		totalVatAmount3M = BigDecimal.ZERO;

		try {
			sess = dataSource.openSession();
			dataSource.getEntryDAO(sess).getByPeriodIdAndDate(period.getId(),
				startDate, endDate, 1, new DTOCallback<Entry>() {
				public void process(Entry entry) {
					if (entry.getFlag(0)) {
						return;
					}

					if (entry.getDocumentId() != documentId) {
						documentId = entry.getDocumentId();
						entryMap.clear();
					}

					Account account = accountMap.get(entry.getAccountId());
					addVatAmount(account, entry);

					/* Verollinen myynti, verollinen osto, yhteisöosto tai rakentamispalvelun osto */
					if (account.getVatCode() == 4 || account.getVatCode() == 5 ||
							account.getVatCode() == 9 || account.getVatCode() == 11) {
						entryMap.put(entry.getRowNumber(), entry);
					}

					/* Veroton myynti, veroton osto, yhteisömyynti tai rakentamispalvelun myynti */
					if (account.getVatCode() == 6 || account.getVatCode() == 7 ||
							account.getVatCode() == 8 || account.getVatCode() == 10) {
						balances.addEntry(entry);
					}

					if (entry.getRowNumber() >= 100000 &&
							entry.getRowNumber() < 300000) {

						int rowNumber = entry.getRowNumber() % 100000;
						Entry entry2 = entryMap.remove(rowNumber);

						if (entry2 != null) {
							balances.addEntry(entry2);
							BigDecimal vatAmount = entry.getAmount();

							if (entry.isDebit())
								vatAmount = vatAmount.negate();

							BigDecimal vatAmountTotal = vatAmounts.get(entry2.getAccountId());

							if (vatAmountTotal == null)
								vatAmountTotal = BigDecimal.ZERO;

							vatAmountTotal = vatAmountTotal.add(vatAmount);
							vatAmounts.put(entry2.getAccountId(), vatAmountTotal);
						}
					}
				}
			});
		}
		finally {
			if (sess != null) sess.close();
		}

		rows = new ArrayList<VATReportRow>();
		BigDecimal vatExcluded, vatIncluded, vatAmount;

		/* Otetaan talteen veron perusteet ja veron määrät. */
		for (Account account : accounts) {
			vatExcluded = balances.getBalance(account.getId());
			vatAmount = vatAmounts.get(account.getId());

			if (vatExcluded != null && account.getVatCode() >= 4 &&
					vatExcluded.compareTo(BigDecimal.ZERO) != 0) {

				if (vatAmount == null)
					vatAmount = BigDecimal.ZERO;

				if (account.getVatCode() == 5 ||
						account.getVatCode() == 7 ||
						account.getVatCode() == 9 ||
						account.getVatCode() == 11) {
					vatExcluded = vatExcluded.negate();
				}

				vatIncluded = vatExcluded.add(vatAmount);

				rows.add(new VATReportRow(1, account, vatExcluded,
						vatIncluded, vatAmount, null));
			}
		}

		/* Lajitellaan rivit ALV-koodin ja ALV-prosentin perusteella. */
		Collections.sort(rows);

		int prevCode = -1;
		BigDecimal prevRate = new BigDecimal("-1");
		boolean codeChanged, rateChanged;
		BigDecimal vatExcludedR, vatIncludedR, vatAmountR;
		vatExcluded = vatExcludedR = BigDecimal.ZERO;
		vatIncluded = vatIncludedR = BigDecimal.ZERO;
		vatAmount = vatAmountR = BigDecimal.ZERO;
		DecimalFormat formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(0);
		formatter.setMinimumFractionDigits(2);

		/* Lisätään otsikot ja summarivit. */
		for (int i = 0; i < rows.size(); i++) {
			codeChanged = rows.get(i).account.getVatCode() != prevCode;
			rateChanged = rows.get(i).account.getVatRate().compareTo(prevRate) != 0;

			if ((codeChanged || rateChanged) && prevRate.compareTo(BigDecimal.ZERO) >= 0 &&
					(prevCode == 4 || prevCode == 5 || prevCode == 9 || prevCode == 11)) {

				String text = String.format("ALV %s yhteensä",
						formatter.format(prevRate));

				/* Lisätään ALV-prosentin summarivi. */
				rows.add(i, new VATReportRow(3, null,
						vatExcludedR, vatIncludedR, vatAmountR, text));
				i++;
				vatExcludedR = BigDecimal.ZERO;
				vatIncludedR = BigDecimal.ZERO;
				vatAmountR = BigDecimal.ZERO;
			}

			if (codeChanged) {
				if (prevCode != -1) {
					/* Lisätään ALV-koodin summarivi. */
					rows.add(i, new VATReportRow(3, null,
							vatExcluded, vatIncluded, vatAmount,
							VAT_CODES[prevCode] + " yhteensä"));
					i++;
					vatExcluded = vatExcludedR = BigDecimal.ZERO;
					vatIncluded = vatIncludedR = BigDecimal.ZERO;
					vatAmount = vatAmountR = BigDecimal.ZERO;

					/* Lisätään tyhjä rivi ennen otsikkoa. */
					rows.add(i, new VATReportRow(0, null, null, null, null, null));
					i++;
				}

				/* Lisätään ALV-koodin otsikko. */
				rows.add(i, new VATReportRow(2, null, null, null, null,
						VAT_CODES[rows.get(i).account.getVatCode()]));
				i++;
			}

			vatExcluded = vatExcluded.add(rows.get(i).vatExcludedTotal);
			vatIncluded = vatIncluded.add(rows.get(i).vatIncludedTotal);
			vatAmount = vatAmount.add(rows.get(i).vatAmountTotal);
			vatExcludedR = vatExcludedR.add(rows.get(i).vatExcludedTotal);
			vatIncludedR = vatIncludedR.add(rows.get(i).vatIncludedTotal);
			vatAmountR = vatAmountR.add(rows.get(i).vatAmountTotal);
			prevRate = rows.get(i).account.getVatRate();
			prevCode = rows.get(i).account.getVatCode();
		}

		if (prevRate.compareTo(BigDecimal.ZERO) >= 0) {
			if (prevCode == 4 || prevCode == 5 || prevCode == 9 || prevCode == 11) {
				String text = String.format("ALV %s yhteensä",
						formatter.format(prevRate));

				/* Lisätään ALV-prosentin summarivi. */
				rows.add(new VATReportRow(3, null,
						vatExcludedR, vatIncludedR, vatAmountR, text));
			}

			/* Lisätään ALV-koodin summarivi. */
			rows.add(new VATReportRow(3, null,
					vatExcluded, vatIncluded, vatAmount,
					VAT_CODES[prevCode] + " yhteensä"));

			vatExcluded = vatExcludedR = BigDecimal.ZERO;
			vatIncluded = vatIncludedR = BigDecimal.ZERO;
			vatAmount = vatAmountR = BigDecimal.ZERO;
		}

		rows.add(new VATReportRow(0, null, null, null, null, null));

		if (totalVatAmount2M.compareTo(BigDecimal.ZERO) != 0) {
			rows.add(new VATReportRow(4, null, null, null, totalVatAmount2M, "Erittelemätön vero myynnistä"));
		}

		if (totalVatAmount3M.compareTo(BigDecimal.ZERO) != 0) {
			rows.add(new VATReportRow(4, null, null, null, totalVatAmount3M, "Erittelemätön vero ostoista"));
		}

		BigDecimal sum = totalVatAmount2.add(totalVatAmount3);

		if (sum.compareTo(BigDecimal.ZERO) < 0) {
			rows.add(new VATReportRow(5, null, null, null, sum, "Palautukseen oikeuttava vero"));
		}
		else {
			rows.add(new VATReportRow(5, null, null, null, sum, "Maksettava vero"));
		}
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		DecimalFormat numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);

		writer.writeField("ALV-laskelma tileittäin");
		writer.writeLine();
		writer.writeField("Nimi");
		writer.writeField(settings.getName());
		writer.writeLine();
		writer.writeField("Y-tunnus");
		writer.writeField(settings.getBusinessId());
		writer.writeLine();
		writer.writeField("Alkaa");
		writer.writeField(dateFormat.format(startDate));
		writer.writeLine();
		writer.writeField("Päättyy");
		writer.writeField(dateFormat.format(endDate));
		writer.writeLine();
		writer.writeLine();
		writer.writeField("Tilinumero");
		writer.writeField("Tilin nimi");
		writer.writeField("Veron peruste");
		writer.writeField("Vero");
		writer.writeField("Verollinen summa");
		writer.writeLine();

		for (VATReportRow row : rows) {
			if (row.type == 1) {
				writer.writeField(row.account.getNumber());
				writer.writeField(row.account.getName());
				writer.writeField(numberFormat.format(row.vatExcludedTotal));
				writer.writeField(numberFormat.format(row.vatAmountTotal));
				writer.writeField(numberFormat.format(row.vatIncludedTotal));
			}
			else if (row.type == 2) {
				writer.writeField("");
				writer.writeField(row.text);
				writer.writeField("");
				writer.writeField("");
				writer.writeField("");
			}
			else if (row.type == 3) {
				writer.writeField("");
				writer.writeField(row.text);
				writer.writeField(numberFormat.format(row.vatExcludedTotal));
				writer.writeField(numberFormat.format(row.vatAmountTotal));
				writer.writeField(numberFormat.format(row.vatIncludedTotal));
			}
			else if (row.type == 4 || row.type == 5) {
				writer.writeField("");
				writer.writeField(row.text);
				writer.writeField("");
				writer.writeField(numberFormat.format(row.vatAmountTotal));
				writer.writeField("");
			}

			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		s.setTitle(String.format("ALV-laskelma tileittäin: %s - %s",
				dateFormat.format(startDate), dateFormat.format(endDate)));
		s.defineColumn("co1", "1.5cm");
		s.defineColumn("co2", "7cm");
		s.defineColumn("co3", "3.5cm");
		s.addTable("ALV-laskelma tileittäin");
		s.addColumn("co1", "Default");
		s.addColumn("co2", "Default");
		s.addColumn("co3", "num2");
		s.addColumn("co3", "num2");
		s.addColumn("co3", "num2");

		s.addRow();
		s.writeTextCell("", "boldBorderBottom");
		s.writeTextCell("Tili", "boldBorderBottom");
		s.writeTextCell("Veron peruste", "boldAlignRightBorderBottom");
		s.writeTextCell("Vero", "boldAlignRightBorderBottom");
		s.writeTextCell("Verollinen summa", "boldAlignRightBorderBottom");

		for (VATReportRow row : rows) {
			s.addRow();

			if (row.type == 1) {
				s.writeTextCell(row.account.getNumber());
				s.writeTextCell(row.account.getName());
				s.writeFloatCell(row.vatExcludedTotal, "num2");
				s.writeFloatCell(row.vatAmountTotal, "num2");
				s.writeFloatCell(row.vatIncludedTotal, "num2");
			}
			else if (row.type == 2) {
				s.writeEmptyCell();
				s.writeTextCell(row.text, "bold");
			}
			else if (row.type == 3) {
				s.writeEmptyCell();
				s.writeTextCell(row.text, "bold");
				s.writeFloatCell(row.vatExcludedTotal, "num2");
				s.writeFloatCell(row.vatAmountTotal, "num2");
				s.writeFloatCell(row.vatIncludedTotal, "num2");
			}
			else if (row.type == 4 || row.type == 5) {
				s.writeEmptyCell();
				s.writeTextCell(row.text, row.type == 4 ? "Default" : "bold");
				s.writeTextCell("", "num2");
				s.writeFloatCell(row.vatAmountTotal, "num2");
				s.writeTextCell("", "num2");
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
	 * @return rivin tyyppi
	 */
	public int getType(int index) {
		return rows.get(index).type;
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
	 * Palauttaa rivillä <code>index</code> olevan
	 * summan ilman ALV:tä.
	 *
	 * @param index rivinumero
	 * @return summa
	 */
	public BigDecimal getVatExcludedTotal(int index) {
		return rows.get(index).vatExcludedTotal;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan
	 * summan ALV:n kanssa.
	 *
	 * @param index rivinumero
	 * @return summa
	 */
	public BigDecimal getVatIncludedTotal(int index) {
		return rows.get(index).vatIncludedTotal;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan
	 * ALV:n määrän.
	 *
	 * @param index rivinumero
	 * @return ALV:n määrä
	 */
	public BigDecimal getVatAmountTotal(int index) {
		return rows.get(index).vatAmountTotal;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tekstin.
	 *
	 * @param index rivinumero
	 * @return teksti
	 */
	public String getText(int index) {
		return rows.get(index).text;
	}

	private void addVatAmount(Account account, Entry entry) {
		BigDecimal vatAmount = entry.getAmount();

		if (entry.isDebit())
			vatAmount = vatAmount.negate();

		if (account.getVatCode() == 2) {
			totalVatAmount2 = totalVatAmount2.add(vatAmount);

			if (entry.getRowNumber() < 100000) {
				totalVatAmount2M = totalVatAmount2M.add(vatAmount);
			}
		}

		if (account.getVatCode() == 3) {
			totalVatAmount3 = totalVatAmount3.add(vatAmount);

			if (entry.getRowNumber() < 100000) {
				totalVatAmount3M = totalVatAmount3M.add(vatAmount);
			}
		}
	}

	private class VATReportRow implements Comparable<VATReportRow> {
		public int type;
		public Account account;
		public BigDecimal vatExcludedTotal;
		public BigDecimal vatIncludedTotal;
		public BigDecimal vatAmountTotal;
		public String text;

		public VATReportRow(int type, Account account,
				BigDecimal vatExcludedTotal,
				BigDecimal vatIncludedTotal,
				BigDecimal vatAmountTotal,
				String text) {

			this.type = type;
			this.account = account;
			this.vatExcludedTotal = vatExcludedTotal;
			this.vatIncludedTotal = vatIncludedTotal;
			this.vatAmountTotal = vatAmountTotal;
			this.text = text;
		}

		public int compareTo(VATReportRow o) {
			if (account.getVatCode() == o.account.getVatCode()) {
				if (account.getVatRate() == o.account.getVatRate()) {
					return account.getNumber().compareTo(o.account.getNumber());
				}
				else {
					return account.getVatRate().compareTo(o.account.getVatRate());
				}
			}
			else {
				return account.getVatCode() - o.account.getVatCode();
			}
		}
	}
}
