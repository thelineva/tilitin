package kirjanpito.reports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.ReportStructure;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;

/**
 * Malli tuloslaskelmalle ja taseelle.
 * 
 * @author Tommi Helineva
 */
public class FinancialStatementModel implements PrintModel {
	private DataSource dataSource;
	private Settings settings;
	private Period period;
	private Period periodPrev;
	private Date startDate;
	private Date endDate;
	private String reportId;
	private boolean previousPeriodVisible;
	private ReportStructure structure;
	private List<Account> accounts;
	private List<FinancialStatementRow> rows;
	private AccountBalances balances;
	private AccountBalances balancesPrev;
	private DecimalFormat numberFormat;
	private boolean details;
	private boolean emptyRow;
	
	public static final int STYLE_PLAIN = 0;
	public static final int STYLE_BOLD = 1;
	public static final int STYLE_ITALIC = 2;
	
	public FinancialStatementModel() {
		numberFormat = new DecimalFormat();
		numberFormat.setParseBigDecimal(true);
	}
	
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
	 * Palauttaa tulosteen tunnisteen.
	 * 
	 * @return tulosteen tunniste
	 */
	public String getReportId() {
		return reportId;
	}

	/**
	 * Asettaa tulosteen tunnisteen.
	 * 
	 * @param reportId tulosteen tunniste
	 */
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	/**
	 * Ilmoittaa, onko edellisen tilikauden rahamäärät näkyvillä
	 * 
	 * @return <code>true</code> tai <code>false</code>
	 */
	public boolean isPreviousPeriodVisible() {
		return previousPeriodVisible;
	}

	/**
	 * Näyttää tai piilottaa edellisen tilikauden rahamäärät.
	 * 
	 * @param previousPeriodVisible ilmoittaa, onko edellisen tilikauden rahamäärät näkyvillä
	 */
	public void setPreviousPeriodVisible(boolean previousPeriodVisible) {
		this.previousPeriodVisible = previousPeriodVisible;
	}

	public void run() throws DataAccessException {
		Session sess = null;
		
		if (startDate == null)
			startDate = period.getStartDate();
	
		if (endDate == null)
			endDate = period.getEndDate();
		
		try {
			sess = dataSource.openSession();
			structure = dataSource.getReportStructureDAO(
					sess).getById(reportId);
			
			balances = new AccountBalances(accounts);
			
			/* Lasketaan nykyisen tilikauden tilien saldot. */
			dataSource.getEntryDAO(sess).getByPeriodIdAndDate(period.getId(),
				startDate, endDate, new DTOCallback<Entry>() {
					public void process(Entry entry) {
						balances.addEntry(entry);
					}
				});
			
			if (previousPeriodVisible) {
				/* Haetaan edellinen tilikausi. */
				periodPrev = null;
				List<Period> periods = dataSource.getPeriodDAO(sess).getAll();
				
				for (int i = 1; i < periods.size(); i++) {
					if (periods.get(i).getId() == period.getId()) {
						periodPrev = periods.get(i - 1);
						break;
					}
				}
				
				if (periodPrev == null) {
					previousPeriodVisible = false;
				}
				else {
					balancesPrev = new AccountBalances(accounts);
					
					/* Lasketaan edellisen tilikauden tilien saldot. */
					dataSource.getEntryDAO(sess).getByPeriodIdAndDate(periodPrev.getId(),
						periodPrev.getStartDate(), periodPrev.getEndDate(), new DTOCallback<Entry>() {
							public void process(Entry entry) {
								balancesPrev.addEntry(entry);
							}
						});
				}
			}
		}
		finally {
			if (sess != null) sess.close();
		}
		
		/* Tulosteen sisältö on tallennettu CSV-tiedostoon, jossa
		 * erottimena käytetään ;-merkkiä. Ensimmäinen kenttä sisältää
		 * kolme merkkiä:
		 * 
		 * 1. merkki 'D': Tilierittelyt.
		 * 1. merkki 'H': Tulostetaan otsikkorivi, ei rahamäärää.
		 *                Rivi näytetään aina.
		 * 1. merkki 'G': Tulostetaan otsikkorivi, ei rahamäärää. Rivi
		 *                näytetään vain, jos summa on erisuuri kuin 0,00.
		 * 1. merkki 'S': Tulostetaan teksti ja rahamäärä.
		 *                Rivi näytetään aina.
		 * 1. merkki 'T': Tulostetaan teksti ja rahamäärä. Rivi
		 *                näytetään vain, jos summa on erisuuri kuin 0,00.
		 * 2. merkki 'P': Tekstiä ei lihavoida eikä kursivoida.
		 * 2. merkki 'B': Teksti lihavoidaan.
		 * 2. merkki 'I': Teksti kursivoidaan.
		 * 3. merkki:     Ilmoittaa, kuinka paljon tekstiä sisennetään.
		 *                Kokonaisluku välillä 0 .. 9.
		 *                
		 * Seuraavat kentät ilmoittavat, miltä tilinumeroväleiltä
		 * summa lasketaan. Kenttiä on oltava parillinen määrä,
		 * jokaisen välin alku- ja loppunumero.
		 * 
		 * Viimeinen kenttä on tulostettava teksti.
		 */
		BufferedReader reader = new BufferedReader(
				new StringReader(structure.getData()));
		String line;
		rows = new ArrayList<FinancialStatementRow>();
		
		try {
			while ((line = reader.readLine()) != null) {
				if (line.length() > 0) {
					processLine(line);
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processLine(String line) {
		if (line.equals("-")) {
			if (!emptyRow) {
				rows.add(new FinancialStatementRow(null,
					"", STYLE_PLAIN, 0, null, null));
				emptyRow = true;
			}
			
			return;
		}
		
		String text;
		int style, level;
		char typeChar = line.charAt(0);
		char styleChar = line.charAt(1);
		int offset = 1;
		
		if (typeChar == 'D' && (styleChar == '+' || styleChar == '-' || styleChar == '0')) {
			offset = 2;
			styleChar = line.charAt(offset);
		}
		
		level = line.charAt(offset + 1) - '0';
		
		if (styleChar == 'B') {
			style = STYLE_BOLD;
		}
		else if (styleChar == 'I') {
			style = STYLE_ITALIC;
		}
		else {
			style = STYLE_PLAIN;
		}
		
		BigDecimal amount = BigDecimal.ZERO;
		BigDecimal amountPrev = BigDecimal.ZERO;
		
		if (typeChar == 'F') {
			String[] fields = line.substring(offset + 3).split(";");
			
			try {
				amount = (fields.length >= 2) ? (BigDecimal)numberFormat.parse(fields[1]) : null;
				amountPrev = (fields.length >= 3) ? (BigDecimal)numberFormat.parse(fields[2]) : null;
			}
			catch (ParseException e) {
				amount = null;
				amountPrev = null;
			}
			
			if (!previousPeriodVisible) {
				amountPrev = null;
			}
			
			rows.add(new FinancialStatementRow(null,
					fields[0], style, level, amount, amountPrev));
			emptyRow = false;
			return;
		}
		
		/* Luetaan tilinumerot ja lasketaan rivin rahamäärä. */
		int pos1, pos2, pos3;
		pos1 = offset + 3;
		pos2 = line.indexOf(';', pos1);
		pos3 = line.indexOf(';', pos2 + 1);
		
		while (pos2 >= 0) {
			String start = line.substring(pos1, pos2);
			String stop = line.substring(pos2 + 1, pos3);
			
			if (typeChar == 'D') {
				addDetailRows(start, stop, style, level, line.charAt(1));
			}
			else {
				amount = amount.add(calculateBalance(balances, start, stop));
				amountPrev = previousPeriodVisible ?
						amountPrev.add(calculateBalance(balancesPrev, start, stop)) : null;
			}
			
			pos1 = pos3 + 1;
			pos2 = line.indexOf(';', pos1);
			pos3 = line.indexOf(';', pos2 + 1);
		}
		
		text = line.substring(pos1);
		
		/* H- ja S-rivit näytetään aina, G- ja T-rivit vain, jos arvo on
		 * erisuuri kuin 0,00. */
		if (previousPeriodVisible) {
			if (amount.compareTo(BigDecimal.ZERO) == 0 &&
					amountPrev.compareTo(BigDecimal.ZERO) == 0 &&
					(typeChar == 'G' || typeChar == 'T')) {
				return;
			}
		}
		else if (amount.compareTo(BigDecimal.ZERO) == 0 &&
				(typeChar == 'G' || typeChar == 'T')) {
			return;
		}
		
		/* Otsikkoriveillä ei näytetä euromäärää. */
		if (typeChar == 'H' || typeChar == 'G') {
			amount = null;
			amountPrev = null;
		}
		
		if (typeChar != 'D') {
			rows.add(new FinancialStatementRow(null,
				text, style, level, amount, amountPrev));
			emptyRow = false;
		}
	}
	
	/**
	 * Laskee yhteen saldot niiltä tileiltä, joiden numero
	 * on välillä <code>[start, stop[</code>.
	 * 
	 * @param balances tilien saldot
	 * @param start välin alku
	 * @param stop välin loppu
	 * @return saldojen summa
	 */
	private BigDecimal calculateBalance(AccountBalances balances,
			String start, String stop) {
		String number;
		BigDecimal balance;
		BigDecimal sum = BigDecimal.ZERO;
		
		for (Account account : accounts) {
			number = account.getNumber();
			
			/* Jos tilinumero on välillä [start; stop[, lisätään tilin
			 * saldo summaan. */ 
			if (number.compareTo(start) >= 0 && number.compareTo(stop) < 0) {
				balance = balances.getBalance(account.getId());
				
				if (balance != null) {
					if (account.getType() == Account.TYPE_EXPENSE)
						balance = balance.negate();
					
					sum = sum.add(balance);
				}
			}
		}
		
		return sum;
	}
	
	private void addDetailRows(String start, String stop, int style, int level, char filterChar) {
		String number;
		BigDecimal balance;
		BigDecimal balancePrev;
		
		for (Account account : accounts) {
			number = account.getNumber();
			
			if (number.compareTo(start) >= 0 && number.compareTo(stop) < 0) {
				if (previousPeriodVisible) {
					balancePrev = balancesPrev.getBalance(account.getId());
				}
				else {
					balancePrev = null;
				}
				
				balance = balances.getBalance(account.getId());
				
				if (balance == null && filterChar == '0') {
					balance = BigDecimal.ZERO;
				}
				
				if (balance == null && balancePrev == null) {
					continue;
				}
				
				if (account.getType() == Account.TYPE_EXPENSE) {
					if (balance != null) balance = balance.negate();
					if (balancePrev != null) balancePrev = balancePrev.negate();
				}
				
				if (balance != null) {
					if (filterChar == '+' && balance.compareTo(BigDecimal.ZERO) < 0) {
						continue;
					}
					else if (filterChar == '-' && balance.compareTo(BigDecimal.ZERO) > 0) {
						continue;
					}
					else if (filterChar == '0' && balance.compareTo(BigDecimal.ZERO) != 0) {
						continue;
					}
				}
				
				details = true;
				emptyRow = false;
				rows.add(new FinancialStatementRow(account.getNumber(),
						account.getName(), style, level,
						balance, balancePrev));
			}
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
	 * Palauttaa edellisen tilikauden.
	 * 
	 * @return edellinen tilikausi
	 */
	public Period getPreviousPeriod() {
		return periodPrev;
	}
	
	/**
	 * Palauttaa rivien lukumäärän.
	 * 
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return rows.size();
	}
	
	/**
	 * Palauttaa tilinumeron rivillä <code>index</code>.
	 * 
	 * @param index rivinumero
	 * @return tilinumero
	 */
	public String getNumber(int index) {
		return rows.get(index).number;
	}
	
	/**
	 * Palauttaa tekstin rivillä <code>index</code>.
	 * 
	 * @param index rivinumero
	 * @return teksti
	 */
	public String getText(int index) {
		return rows.get(index).text;
	}
	
	/**
	 * Palauttaa tekstityylin rivillä <code>index</code>.
	 * 
	 * @param index rivinumero
	 * @return STYLE_PLAIN, STYLE_BOLD tai STYLE_ITALIC
	 */
	public int getStyle(int index) {
		return rows.get(index).style;
	}
	
	/**
	 * Palauttaa sisennystason rivillä <code>index</code>.
	 * 
	 * @param index rivinumero
	 * @return sisennystaso
	 */
	public int getLevel(int index) {
		return rows.get(index).level;
	}
	
	/**
	 * Palauttaa nykyisen tilikauden rahamäärän rivillä <code>index</code>.
	 * 
	 * @param index rivinumero
	 * @return rahamäärä
	 */
	public BigDecimal getAmount(int index) {
		return rows.get(index).amount;
	}
	
	/**
	 * Palauttaa edellisen tilikauden rahamäärän rivillä <code>index</code>.
	 * 
	 * @param index rivinumero
	 * @return rahamäärä
	 */
	public BigDecimal getAmountPrev(int index) {
		return rows.get(index).amountPrev;
	}
	
	public boolean containsDetails() {
		return details;
	}
	
	private class FinancialStatementRow {
		public String number;
		public String text;
		public int style;
		public int level;
		public BigDecimal amount;
		public BigDecimal amountPrev;
		
		public FinancialStatementRow(String number, String text,
				int style, int level, BigDecimal amount, BigDecimal amountPrev)
		{
			this.number = number;
			this.text = text;
			this.style = style;
			this.level = level;
			this.amount = amount;
			this.amountPrev = amountPrev;
		}
	}
}
