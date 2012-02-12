package kirjanpito.reports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ODFSpreadsheet;

/**
 * Malli tuloslaskelmalle ja taseelle.
 *
 * @author Tommi Helineva
 */
public class FinancialStatementModel implements PrintModel {
	private DataSource dataSource;
	private Settings settings;
	private int type;
	private String title;
	private Date[] startDates;
	private Date[] endDates;
	private boolean pageBreakEnabled;
	private ReportStructure structure;
	private List<Account> accounts;
	private List<FinancialStatementRow> rows;
	private AccountBalances[] balances;
	private DecimalFormat numberFormat;
	private boolean details;
	private boolean emptyRow;
	private int maxLevel;
	private boolean styleBold;
	private boolean styleItalic;

	public static final int STYLE_PLAIN = 0;
	public static final int STYLE_BOLD = 1;
	public static final int STYLE_ITALIC = 2;

	public static final int TYPE_INCOME_STATEMENT = 1;
	public static final int TYPE_INCOME_STATEMENT_DETAILED = 2;
	public static final int TYPE_BALANCE_SHEET = 3;
	public static final int TYPE_BALANCE_SHEET_DETAILED = 4;

	public FinancialStatementModel(int type) {
		this.type = type;
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
	 * Palauttaa tulosteen tyypin.
	 *
	 * @return tulosteen tyyppi
	 */
	public int getType() {
		return type;
	}

	/**
	 * Palauttaa alkamispäivämäärät.
	 *
	 * @return alkamispäivämäärät
	 */
	public Date[] getStartDates() {
		return startDates;
	}

	/**
	 * Asettaa alkamispäivämäärät.
	 *
	 * @param startDate alkamispäivämäärät
	 */
	public void setStartDates(Date[] startDates) {
		this.startDates = startDates;
	}

	/**
	 * Palauttaa päättymispäivämäärät.
	 *
	 * @return päättymispäivämäärät
	 */
	public Date[] getEndDates() {
		return endDates;
	}

	/**
	 * Asettaa päättymispäivämäärät.
	 *
	 * @param endDate päättymispäivämäärät
	 */
	public void setEndDates(Date[] endDates) {
		this.endDates = endDates;
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
	 * Palauttaa tulosteen nimen.
	 *
	 * @return tulosteen nimi
	 */
	public String getTitle() {
		return title;
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
	 * Ilmoittaa, ovatko sivunvaihdot käytössä
	 *
	 * @return <code>true</code>, jos sivunvaihdot ovat käytössä
	 */
	public boolean isPageBreakEnabled() {
		return pageBreakEnabled;
	}

	/**
	 * Asettaa sivunvaihdot käyttöön tai pois käytöstä.
	 *
	 * @param pageBreakEnabled <code>true</code> tai <code>false</code>
	 */
	public void setPageBreakEnabled(boolean pageBreakEnabled) {
		this.pageBreakEnabled = pageBreakEnabled;
	}

	public void run() throws DataAccessException {
		Session sess = null;
		String reportId;

		switch (type) {
		case TYPE_INCOME_STATEMENT:
			reportId = "income-statement";
			title = "Tuloslaskelma";
			break;

		case TYPE_INCOME_STATEMENT_DETAILED:
			reportId = "income-statement-detailed";
			title = "Tuloslaskelma";
			break;

		case TYPE_BALANCE_SHEET:
			reportId = "balance-sheet";
			title = "Tase";
			break;

		case TYPE_BALANCE_SHEET_DETAILED:
			reportId = "balance-sheet-detailed";
			title = "Tase";
			break;

		default:
			throw new IllegalArgumentException("Invalid report type: " + type);
		}

		try {
			sess = dataSource.openSession();
			structure = dataSource.getReportStructureDAO(
					sess).getById(reportId);

			if (type == TYPE_BALANCE_SHEET || type == TYPE_BALANCE_SHEET_DETAILED) {
				List<Period> periods = dataSource.getPeriodDAO(sess).getAll();

				for (int i = 0; i < startDates.length; i++) {
					for (Period period : periods) {
						if (!period.getStartDate().after(endDates[i]) &&
								!period.getEndDate().before(endDates[i])) {
							startDates[i] = period.getStartDate();
							break;
						}
					}

					if (startDates[i] == null) {
						startDates[i] = endDates[i];
					}
				}
			}

			balances = new AccountBalances[startDates.length];

			for (int i = 0; i < balances.length; i++) {
				balances[i] = new AccountBalances(accounts);
			}

			/* Lasketaan tilien saldot. */
			for (int i = 0; i < startDates.length; i++) {
				final int index = i;

				dataSource.getEntryDAO(sess).getByPeriodIdAndDate(-1,
						startDates[i], endDates[i],
					new DTOCallback<Entry>() {
						public void process(Entry entry) {
							balances[index].addEntry(entry);
						}
					});
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
		maxLevel = 0;
		styleBold = false;
		styleItalic = false;

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

		balances = null;
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		DecimalFormat numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);

		writer.writeField(title);
		writer.writeLine();
		writer.writeField("Nimi");
		writer.writeField(settings.getName());
		writer.writeLine();
		writer.writeField("Y-tunnus");
		writer.writeField(settings.getBusinessId());
		writer.writeLine();
		writer.writeField("Alkaa");

		for (Date date : startDates) {
			writer.writeField(dateFormat.format(date));
		}

		writer.writeLine();
		writer.writeField("Päättyy");

		for (Date date : endDates) {
			writer.writeField(dateFormat.format(date));
		}

		writer.writeLine();
		writer.writeLine();

		for (FinancialStatementRow row : rows) {
			writer.writeField(row.text.isEmpty() ? "" : Integer.toString(row.level));
			writer.writeField((row.number == null) ? "" : row.number);
			writer.writeField(row.text);

			if (row.amounts != null) {
				for (BigDecimal amount : row.amounts) {
					writer.writeField((amount == null) ? "" : numberFormat.format(amount));
				}
			}

			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		s.setTitle(title);
		s.defineColumn("co1", "1.5cm");
		s.defineColumn("co2", "10cm");
		s.defineColumn("co3", "2.6cm");
		s.addIndentLevels(maxLevel + 1, styleBold, styleItalic);
		s.addTable(title);
		s.addColumn("co1", "Default");
		s.addColumn("co2", "Default");

		for (int i = 0; i < startDates.length; i++) {
			s.addColumn("co3", "num2");
		}

		s.addRow();
		s.writeEmptyCell();
		s.writeEmptyCell();

		if (type != FinancialStatementModel.TYPE_BALANCE_SHEET &&
				type != FinancialStatementModel.TYPE_BALANCE_SHEET_DETAILED) {
			for (int i = 0; i < startDates.length; i++) {
				s.writeTextCell(dateFormat.format(startDates[i]) + " -", "boldAlignRight");
			}
		}

		s.addRow();
		s.writeEmptyCell();
		s.writeEmptyCell();

		for (int i = 0; i < endDates.length; i++) {
			s.writeTextCell(dateFormat.format(endDates[i]), "boldAlignRight");
		}

		s.addRow();

		for (FinancialStatementRow row : rows) {
			s.addRow();
			String style = "indent" + row.level;

			if (row.style == STYLE_BOLD) {
				style += "Bold";
			}
			else if (row.style == STYLE_ITALIC) {
				style += "Italic";
			}

			s.writeTextCell((row.number == null) ? "" : row.number);
			s.writeTextCell(row.text, style);

			if (row.amounts != null) {
				for (BigDecimal amount : row.amounts) {
					if (amount == null) {
						s.writeEmptyCell();
					}
					else {
						s.writeFloatCell(amount, "num2");
					}
				}
			}
		}
	}

	private void processLine(String line) {
		if (line.equals("--") && pageBreakEnabled) {
			rows.add(new FinancialStatementRow(null,
					"", STYLE_PLAIN, -1, null));
			emptyRow = true;
			return;
		}

		if (line.startsWith("-")) {
			if (!emptyRow) {
				rows.add(new FinancialStatementRow(null,
					"", STYLE_PLAIN, 0, null));
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
			styleBold = true;
		}
		else if (styleChar == 'I') {
			style = STYLE_ITALIC;
			styleItalic = true;
		}
		else {
			style = STYLE_PLAIN;
		}

		if (typeChar == 'F') {
			String[] fields = line.substring(offset + 3).split(";");
			FinancialStatementRow row = new FinancialStatementRow(null,
					fields[0], style, level, new BigDecimal[startDates.length]);

			try {
				for (int i = 0; i < startDates.length; i++) {
					if (fields.length >= 2 + i) {
						row.amounts[i] = (BigDecimal)numberFormat.parse(fields[1 + i]);
					}
				}
			}
			catch (ParseException e) {
				e.printStackTrace();
			}

			rows.add(row);
			emptyRow = false;
			return;
		}

		/* Luetaan tilinumerot ja lasketaan rivin rahamäärä. */
		int pos1, pos2, pos3;
		pos1 = offset + 3;
		pos2 = line.indexOf(';', pos1);
		pos3 = line.indexOf(';', pos2 + 1);
		BigDecimal[] amounts = new BigDecimal[startDates.length];

		while (pos2 >= 0) {
			String start = line.substring(pos1, pos2);
			String stop = line.substring(pos2 + 1, pos3);

			if (typeChar == 'D') {
				addDetailRows(start, stop, style, level, line.charAt(1));
			}
			else {
				for (int i = 0; i < amounts.length; i++) {
					amounts[i] = calculateBalance(balances[i], start, stop, amounts[i]);
				}
			}

			pos1 = pos3 + 1;
			pos2 = line.indexOf(';', pos1);
			pos3 = line.indexOf(';', pos2 + 1);
		}

		text = line.substring(pos1);

		/* H- ja S-rivit näytetään aina, G- ja T-rivit vain, jos
		 * jokin summattavista tileistä on ollut käytössä. */
		boolean nonZero = false;

		for (BigDecimal amount : amounts) {
			if (amount != null) {
				nonZero = true;
				break;
			}
		}

		if (!nonZero && (typeChar == 'G' || typeChar == 'T')) {
			return;
		}

		/* Otsikkoriveillä ei näytetä euromäärää. */
		if (typeChar == 'H' || typeChar == 'G') {
			amounts = null;
		}

		if (typeChar != 'D') {
			maxLevel = Math.max(level, maxLevel);
			rows.add(new FinancialStatementRow(null,
				text, style, level, amounts));
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
			String start, String stop, BigDecimal sum) {
		String number;
		BigDecimal balance;
		boolean singleAccount = start.equals(stop);

		for (Account account : accounts) {
			number = account.getNumber();

			/* Jos tilinumero on välillä [start; stop[, lisätään tilin
			 * saldo summaan. */
			if ((number.compareTo(start) >= 0 && number.compareTo(stop) < 0) ||
					(singleAccount && number.equals(start))) {
				balance = balances.getBalance(account.getId());

				if (balance != null) {
					if (account.getType() == Account.TYPE_EXPENSE)
						balance = balance.negate();

					if (sum == null) {
						sum = balance;
					}
					else {
						sum = sum.add(balance);
					}
				}
			}
		}

		return sum;
	}

	private void addDetailRows(String start, String stop, int style, int level, char filterChar) {
		String number;
		boolean singleAccount = start.equals(stop);

		for (Account account : accounts) {
			number = account.getNumber();

			if ((number.compareTo(start) >= 0 && number.compareTo(stop) < 0) ||
					(singleAccount && number.equals(start))) {

				BigDecimal[] amounts = new BigDecimal[startDates.length];
				boolean nonZero = false;

				for (int i = 0; i < startDates.length; i++) {
					amounts[i] = balances[i].getBalance(account.getId());

					if (amounts[i] != null) {
						nonZero = true;
					}
				}

				if (amounts[0] == null && filterChar == '0') {
					amounts[0] = BigDecimal.ZERO;
					nonZero = true;
				}

				if (!nonZero) {
					continue;
				}

				if (account.getType() == Account.TYPE_EXPENSE) {
					for (int i = 0; i < startDates.length; i++) {
						if (amounts[i] != null) amounts[i] = amounts[i].negate();
					}
				}

				if (amounts[0] != null) {
					if (filterChar == '+' && amounts[0].compareTo(BigDecimal.ZERO) < 0) {
						continue;
					}
					else if (filterChar == '-' && amounts[0].compareTo(BigDecimal.ZERO) > 0) {
						continue;
					}
					else if (filterChar == '0' && amounts[0].compareTo(BigDecimal.ZERO) != 0) {
						continue;
					}
				}

				details = true;
				emptyRow = false;
				maxLevel = Math.max(level, maxLevel);
				rows.add(new FinancialStatementRow(account.getNumber(),
						account.getName(), style, level, amounts));
			}
		}
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
	 * Palauttaa sarakkeiden lukumäärän.
	 *
	 * @return sarakkeiden lukumäärä
	 */
	public int getColumnCount() {
		return startDates.length;
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
	public BigDecimal getAmount(int index, int col) {
		FinancialStatementRow row = rows.get(index);
		return (row.amounts == null) ? null : row.amounts[col];
	}

	public boolean containsDetails() {
		return details;
	}

	private class FinancialStatementRow {
		public String number;
		public String text;
		public int style;
		public int level;
		public BigDecimal[] amounts;

		public FinancialStatementRow(String number, String text,
				int style, int level, BigDecimal[] amounts)
		{
			this.number = number;
			this.text = text;
			this.style = style;
			this.level = level;
			this.amounts = amounts;
		}
	}
}
