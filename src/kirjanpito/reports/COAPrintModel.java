package kirjanpito.reports;

import java.io.IOException;
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
import kirjanpito.util.Registry;
import kirjanpito.util.VATUtil;

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
				int vatRate = account.getVatRate();
				
				if (vatRate > 0 && vatRate < VATUtil.VAT_RATE_M2V.length) {
					writer.writeField(VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[vatRate]]);
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
