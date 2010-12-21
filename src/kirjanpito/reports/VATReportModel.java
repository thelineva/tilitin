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
import kirjanpito.util.VATUtil;

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
	private int documentId;
	
	private static final String[] VAT_CODES = {
		null, null, null, null,
		"Verollinen myynti", "Verolliset ostot",
		"Veroton myynti", "Verottomat ostot",
		"Yhteisömyynti", "Yhteisöostot"
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
		
		try {
			sess = dataSource.openSession();
			dataSource.getEntryDAO(sess).getByPeriodIdAndDate(period.getId(),
				startDate, endDate, new DTOCallback<Entry>() {
				public void process(Entry entry) {
					if (entry.getDocumentId() != documentId) {
						documentId = entry.getDocumentId();
						entryMap.clear();
					}
					
					Account account = accountMap.get(entry.getAccountId());
					
					if (account.getVatCode() >= 4) {
						entryMap.put(entry.getRowNumber(), entry);
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
		
		for (Account account : accounts) {
			vatExcluded = balances.getBalance(account.getId());
			vatAmount = vatAmounts.get(account.getId());
			
			if (vatExcluded != null && account.getVatCode() >= 4 &&
					vatExcluded.compareTo(BigDecimal.ZERO) != 0) {
				
				if (vatAmount == null)
					vatAmount = BigDecimal.ZERO;
				
				if (account.getType() == Account.TYPE_EXPENSE) {
					vatExcluded = vatExcluded.negate();
				}
				
				vatIncluded = vatExcluded.add(vatAmount);
				
				rows.add(new VATReportRow(1, account, vatExcluded,
						vatIncluded, vatAmount, null));
			}
		}
		
		Collections.sort(rows);
		
		int prevCode = -1;
		int prevRate = -1;
		boolean codeChanged, rateChanged;
		BigDecimal vatExcludedR, vatIncludedR, vatAmountR;
		vatExcluded = vatExcludedR = BigDecimal.ZERO;
		vatIncluded = vatIncludedR = BigDecimal.ZERO;
		vatAmount = vatAmountR = BigDecimal.ZERO;
		
		for (int i = 0; i < rows.size(); i++) {
			codeChanged = rows.get(i).account.getVatCode() != prevCode;
			rateChanged = rows.get(i).account.getVatRate() != prevRate;
			
			if ((codeChanged || rateChanged) && prevRate >= 0 &&
					(prevCode == 4 || prevCode == 5 || prevCode == 9)) {
				
				String text = String.format("ALV %s yhteensä",
						VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[prevRate]]);
				
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
		
		if (prevRate != -1) {
			if (prevCode == 4 || prevCode == 5 || prevCode == 9) {
				String text = String.format("ALV %s yhteensä",
						VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[prevRate]]);
				
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
			
			writer.writeLine();
		}
	}
	
	/**
	 * Palauttaa käyttäjän nimen.
	 * 
	 * @return käyttäjän nimi
	 */
	public String getName() {
		return settings.getName();
	}
	
	/**
	 * Palauttaa Y-tunnuksen.
	 * 
	 * @return y-tunnus
	 */
	public String getBusinessId() {
		return settings.getBusinessId();
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
					return VATUtil.VAT_RATE_M2V[account.getVatRate()] -
						VATUtil.VAT_RATE_M2V[o.account.getVatRate()];
				}
			}
			else {
				return account.getVatCode() - o.account.getVatCode();
			}
		}
	}
}
