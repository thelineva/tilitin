package kirjanpito.reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.ODFSpreadsheet;
import kirjanpito.util.Registry;

public class COAPrintModel implements PrintModel {
	private Registry registry;
	private ChartOfAccounts coa;
	private int accountLevel;
	private int mode;

	public static final int ALL_ACCOUNTS = 0;
	public static final int USED_ACCOUNTS = 1;
	public static final int FAVOURITE_ACCOUNTS = 2;

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void run() throws DataAccessException {
		if (mode == ALL_ACCOUNTS) {
			coa = registry.getChartOfAccounts();
		}
		else if (mode == USED_ACCOUNTS) {
			AccountBalances balances = fetchAccountBalances();
			coa = new ChartOfAccounts();
			coa.set(registry.getAccounts(), registry.getCOAHeadings());
			coa.filterNonUsedAccounts(balances);
		}
		else if (mode == FAVOURITE_ACCOUNTS) {
			coa = new ChartOfAccounts();
			coa.set(registry.getAccounts(), registry.getCOAHeadings());
			coa.filterNonFavouriteAccounts();
		}

		int maxLevel = 0;

		for (int i = 0; i < coa.getSize(); i++) {
			if (coa.getType(i) == ChartOfAccounts.TYPE_HEADING) {
				maxLevel = Math.max(maxLevel, coa.getHeading(i).getLevel());
			}
		}

		accountLevel = maxLevel + 1;
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		DecimalFormat formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
		Settings settings = registry.getSettings();
		writer.writeField("Tilikartta");
		writer.writeLine();
		writer.writeField("Nimi");
		writer.writeField(settings.getName());
		writer.writeLine();
		writer.writeField("Y-tunnus");
		writer.writeField(settings.getBusinessId());
		writer.writeLine();
		writer.writeLine();
		writer.writeField("");
		writer.writeField("Tilinumero");
		writer.writeField("Tilin nimi");
		writer.writeField("ALV");
		writer.writeLine();

		for (int i = 0; i < coa.getSize(); i++) {
			if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
				Account account = coa.getAccount(i);
				writer.writeField("");
				writer.writeField(account.getNumber());
				writer.writeField(account.getName());

				if (account.getVatRate().compareTo(BigDecimal.ZERO) > 0) {
					writer.writeField(formatter.format(account.getVatRate()));
				}
				else {
					writer.writeField("");
				}
			}
			else {
				COAHeading heading = coa.getHeading(i);
				writer.writeField(Integer.toString(heading.getLevel()));
				writer.writeField("");
				writer.writeField(heading.getText());
				writer.writeField("");
			}

			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		DecimalFormat formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
		s.setTitle("Tilikartta");
		s.defineColumn("co1", new BigDecimal("0.30").multiply(
				new BigDecimal(accountLevel + 1)).toPlainString() + "cm");
		s.defineColumn("co2", "1.5cm");
		s.defineColumn("co3", "7cm");
		s.defineColumn("co4", "3.5cm");
		s.addIndentLevels(accountLevel + 1, true, false);
		s.addTable("Tilikartta");
		s.addColumn("co1", "Default");
		s.addColumn("co2", "Default");
		s.addColumn("co3", "Default");
		s.addColumn("co4", "Default");

		for (int i = 0; i < coa.getSize(); i++) {
			s.addRow();

			if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
				Account account = coa.getAccount(i);
				s.writeEmptyCell();
				s.writeTextCell(account.getNumber());
				s.writeTextCell(account.getName());

				if (account.getVatRate().compareTo(BigDecimal.ZERO) > 0) {
					s.writeTextCell(String.format("ALV %s %%", formatter.format(account.getVatRate())));
				}
			}
			else {
				COAHeading heading = coa.getHeading(i);
				s.writeTextCell(heading.getText(), "indent" + heading.getLevel() + "Bold");
			}
		}
	}

	private AccountBalances fetchAccountBalances() throws DataAccessException {
		final AccountBalances balances = new AccountBalances(registry.getAccounts());
		DataSource dataSource = registry.getDataSource();
		int currentPeriodId = registry.getSettings().getCurrentPeriodId();
		Session sess = null;
		Period period = null;
		int periodIndex = -1;
		List<Period> periods;

		try {
			sess = dataSource.openSession();
			periods = dataSource.getPeriodDAO(sess).getAll();

			for (int i = 0; i < periods.size(); i++) {
				if (periods.get(i).getId() == currentPeriodId) {
					periodIndex = i;
					period = periods.get(i);
					break;
				}
			}

			DTOCallback<Entry> callback = new DTOCallback<Entry>() {
				public void process(Entry obj) {
					balances.addEntry(obj);
				}
			};

			dataSource.getEntryDAO(sess).getByPeriodId(period.getId(),
					EntryDAO.ORDER_BY_DOCUMENT_NUMBER, callback);

			if (periodIndex > 0) {
				period = periods.get(periodIndex - 1);
				dataSource.getEntryDAO(sess).getByPeriodId(period.getId(),
						EntryDAO.ORDER_BY_DOCUMENT_NUMBER, callback);
			}
		}
		finally {
			if (sess != null) sess.close();
		}

		return balances;
	}

	public Settings getSettings() {
		return registry.getSettings();
	}

	public ChartOfAccounts getChartOfAccounts() {
		return coa;
	}

	public int getAccountLevel() {
		return accountLevel;
	}
}
