package kirjanpito.reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.ODFSpreadsheet;
import kirjanpito.util.Registry;

/**
 * Malli tilien saldot -tulosteelle.
 *
 * @author Tommi Helineva
 */
public class AccountSummaryModel implements PrintModel {
	private Registry registry;
	private Settings settings;
	private Period period;
	private Period periodPrev;
	private Date startDate;
	private Date endDate;
	private AccountSummaryRow[] rows;
	private AccountBalances balances;
	private AccountBalances balancesPrev;
	private boolean previousPeriodVisible;
	private int printedAccounts;
	private int maxLevel;

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
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
	 * Palauttaa edellisen tilikauden.
	 *
	 * @return edellinen tilikausi
	 */
	public Period getPreviousPeriod() {
		return periodPrev;
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

	public int getPrintedAccounts() {
		return printedAccounts;
	}

	public void setPrintedAccounts(int printedAccounts) {
		this.printedAccounts = printedAccounts;
	}

	public void run() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		int accountCount = 0;
		settings = registry.getSettings();
		balances = new AccountBalances(registry.getAccounts());

		if (previousPeriodVisible) {
			balancesPrev = new AccountBalances(registry.getAccounts());
		}

		try {
			final HashSet<Integer> accountIds = new HashSet<Integer>();
			sess = dataSource.openSession();
			dataSource.getEntryDAO(sess).getByPeriodIdAndDate(period.getId(),
				startDate, endDate, new DTOCallback<Entry>() {
					public void process(Entry entry) {
						if (isAccountPrinted(entry.getAccountId())) {
							balances.addEntry(entry);
							accountIds.add(entry.getAccountId());
						}
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
					/* Lasketaan edellisen tilikauden tilien saldot. */
					dataSource.getEntryDAO(sess).getByPeriodIdAndDate(periodPrev.getId(),
						periodPrev.getStartDate(), periodPrev.getEndDate(), new DTOCallback<Entry>() {
							public void process(Entry entry) {
								if (isAccountPrinted(entry.getAccountId())) {
									balancesPrev.addEntry(entry);
									accountIds.add(entry.getAccountId());
								}
							}
						});
				}
			}

			accountCount = accountIds.size();
		}
		finally {
			if (sess != null) sess.close();
		}

		BigDecimal balance, balancePrev;
		Account account;
		COAHeading heading;
		ChartOfAccounts coa = registry.getChartOfAccounts();
		HashSet<Integer> headings = new HashSet<Integer>();
		Stack<COAHeading> headingStack = new Stack<COAHeading>();
		maxLevel = 0;

		/* Etsitään tarvittavat otsikot. */
		for (int i = 0; i < coa.getSize(); i++) {
			if (coa.getType(i) == ChartOfAccounts.TYPE_HEADING) {
				heading = coa.getHeading(i);

				while (!headingStack.isEmpty() && headingStack.peek().getLevel() >= heading.getLevel()) {
					headingStack.pop();
				}

				headingStack.add(heading);
			}
			else {
				if (previousPeriodVisible) {
					if (balances.getBalance(coa.getAccount(i).getId()) == null &&
							balancesPrev.getBalance(coa.getAccount(i).getId()) == null) {
						continue;
					}
				}
				else if (balances.getBalance(coa.getAccount(i).getId()) == null) {
					continue;
				}

				if (!headingStack.isEmpty()) {
					maxLevel = Math.max(maxLevel, headingStack.peek().getLevel() + 1);
				}

				for (COAHeading h : headingStack) {
					headings.add(h.getId());
				}
			}
		}

		rows = new AccountSummaryRow[headings.size() + accountCount];
		int index = 0;

		for (int i = 0; i < coa.getSize(); i++) {
			if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
				account = coa.getAccount(i);

				if (previousPeriodVisible) {
					/* Edellinen tilikausi */
					balance = balances.getBalance(account.getId());
					balancePrev = balancesPrev.getBalance(account.getId());

					if (balance != null || balancePrev != null) {
						rows[index++] = new AccountSummaryRow(account, balance, balancePrev, maxLevel);
					}
				}
				else {
					/* Ei edellistä tilikautta */
					balance = balances.getBalance(account.getId());

					if (balance != null) {
						rows[index++] = new AccountSummaryRow(account, balance, null, maxLevel);
					}
				}
			}
			else {
				heading = coa.getHeading(i);

				if (headings.contains(heading.getId())) {
					rows[index++] = new AccountSummaryRow(
						heading.getText(), heading.getLevel());
				}
			}
		}
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		DecimalFormat numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);

		writer.writeField("Tilien saldot");
		writer.writeLine();
		writer.writeField("Nimi");
		writer.writeField(settings.getName());
		writer.writeLine();
		writer.writeField("Y-tunnus");
		writer.writeField(settings.getBusinessId());
		writer.writeLine();
		writer.writeField("Päivämäärä");
		writer.writeField(dateFormat.format(endDate));
		writer.writeLine();
		writer.writeLine();
		writer.writeField("");
		writer.writeField("Tilinumero");
		writer.writeField("Tilin nimi");
		writer.writeField("Saldo");
		writer.writeLine();

		for (AccountSummaryRow row : rows) {
			if (row.text == null) {
				writer.writeField("");
				writer.writeField(row.account.getNumber());
				writer.writeField(row.account.getName());

				if (row.balance == null) {
					writer.writeField("");
				}
				else {
					writer.writeField(numberFormat.format(row.balance));
				}

				if (previousPeriodVisible) {
					if (row.balancePrev == null) {
						writer.writeField("");
					}
					else {
						writer.writeField(numberFormat.format(row.balancePrev));
					}
				}
			}
			else {
				writer.writeField(Integer.toString(row.level));
				writer.writeField("");
				writer.writeField(row.text);
				writer.writeField("");

				if (previousPeriodVisible) {
					writer.writeField("");
				}
			}

			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		s.setTitle("Tilien saldot");
		s.defineColumn("co1", new BigDecimal("0.30").multiply(
				new BigDecimal(maxLevel + 1)).toPlainString() + "cm");
		s.defineColumn("co2", "1.2cm");
		s.defineColumn("co3", "7cm");
		s.defineColumn("co4", "2.6cm");
		s.defineColumn("co5", "2.6cm");
		s.addIndentLevels(maxLevel, true, false);
		s.addTable("Tilien saldot");
		s.addColumn("co1", "Default");
		s.addColumn("co2", "Default");
		s.addColumn("co3", "Default");
		s.addColumn("co4", "Default");
		s.addColumn("co5", "Default");

		s.addRow();
		s.writeEmptyCell();
		s.writeEmptyCell();
		s.writeEmptyCell();
		s.writeTextCell(dateFormat.format(period.getStartDate()) + " -", "boldAlignRight");

		if (previousPeriodVisible) {
			s.writeTextCell(dateFormat.format(periodPrev.getStartDate()) + " -", "boldAlignRight");
		}

		s.addRow();
		s.writeEmptyCell();
		s.writeEmptyCell();
		s.writeEmptyCell();
		s.writeTextCell(dateFormat.format(period.getEndDate()), "boldAlignRight");

		if (previousPeriodVisible) {
			s.writeTextCell(dateFormat.format(periodPrev.getEndDate()), "boldAlignRight");
		}

		s.addRow();

		for (AccountSummaryRow row : rows) {
			s.addRow();

			if (row.text == null) {
				s.writeEmptyCell();
				s.writeTextCell(row.account.getNumber());
				s.writeTextCell(row.account.getName());

				if (row.balance == null) {
					s.writeEmptyCell();
				}
				else {
					s.writeFloatCell(row.balance, "num2");
				}

				if (previousPeriodVisible) {
					if (row.balancePrev == null) {
						s.writeEmptyCell();
					}
					else {
						s.writeFloatCell(row.balancePrev, "num2");
					}
				}
			}
			else {
				s.writeTextCell(row.text, "indent" + row.level + "Bold");
			}
		}
	}

	/**
	 * Palauttaa tulosteessa olevien rivien lukumäärän.
	 *
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return rows.length;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin.
	 *
	 * @param index rivinumero
	 * @return tili
	 */
	public Account getAccount(int index) {
		return rows[index].account;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin
	 * saldon.
	 *
	 * @param index rivinumero
	 * @return tilin saldo
	 */
	public BigDecimal getBalance(int index) {
		return rows[index].balance;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin
	 * edellisen tilikauden saldon.
	 *
	 * @param index rivinumero
	 * @return tilin saldo
	 */
	public BigDecimal getBalancePrev(int index) {
		return rows[index].balancePrev;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan otsikkotekstin.
	 *
	 * @param index rivinumero
	 * @return otsikkoteksti
	 */
	public String getText(int index) {
		return rows[index].text;
	}

	/**
	 * Palauttaa rivin <code>index</code> sisennystason.
	 *
	 * @param index rivinumero
	 * @return sisennystaso
	 */
	public int getLevel(int index) {
		return rows[index].level;
	}

	private boolean isAccountPrinted(int accountId) {
		if (printedAccounts == 0) {
			return true;
		}

		if (printedAccounts == 1) {
			Account account = registry.getAccountById(accountId);

			if (account != null) {
				int type = account.getType();

				return type == Account.TYPE_ASSET ||
					type == Account.TYPE_LIABILITY ||
					type == Account.TYPE_EQUITY ||
					type == Account.TYPE_PROFIT_PREV ||
					type == Account.TYPE_PROFIT;
			}
		}
		else if (printedAccounts == 2) {
			Account account = registry.getAccountById(accountId);

			if (account != null) {
				int type = account.getType();

				return type == Account.TYPE_REVENUE ||
					type == Account.TYPE_EXPENSE;
			}
		}

		return false;
	}

	private class AccountSummaryRow {
		public Account account;
		public BigDecimal balance;
		public BigDecimal balancePrev;
		public String text;
		public int level;

		public AccountSummaryRow(Account account, BigDecimal balance,
				BigDecimal balancePrev, int level) {
			this.account = account;
			this.balance = balance;
			this.balancePrev = balancePrev;
			this.level = level;
		}

		public AccountSummaryRow(String text, int level) {
			this.text = text;
			this.level = level;
		}
	}
}
