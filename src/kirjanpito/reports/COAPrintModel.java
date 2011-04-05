package kirjanpito.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
	private boolean allAccountsVisible;
	
	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public boolean isAllAccountsVisible() {
		return allAccountsVisible;
	}

	public void setAllAccountsVisible(boolean allAccountsVisible) {
		this.allAccountsVisible = allAccountsVisible;
	}

	public void run() throws DataAccessException {
		if (allAccountsVisible) {
			coa = registry.getChartOfAccounts();
			int maxLevel = 0;
			
			for (int i = 0; i < coa.getSize(); i++) {
				if (coa.getType(i) == ChartOfAccounts.TYPE_HEADING) {
					maxLevel = Math.max(maxLevel, coa.getHeading(i).getLevel());
				}
			}
			
			accountLevel = maxLevel + 1;
		}
		else {
			coa = createChartOfAccounts();
		}
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

	private ChartOfAccounts createChartOfAccounts() throws DataAccessException {
		AccountBalances balances = fetchAccountBalances();
		ChartOfAccounts coa = registry.getChartOfAccounts();
		ArrayList<Account> accounts = new ArrayList<Account>();
		ArrayList<COAHeading> headings = new ArrayList<COAHeading>();
		HashSet<Integer> headingIds = new HashSet<Integer>();
		Account account;
		COAHeading heading;
		int maxLevel = 0;
		int level;
		
		for (int i = 0; i < coa.getSize(); i++) {
			if (coa.getType(i) != ChartOfAccounts.TYPE_ACCOUNT)
				continue;
			
			account = coa.getAccount(i);
			
			if (balances.getBalance(account.getId()) == null)
				continue;
			
			accounts.add(account);
			level = Integer.MAX_VALUE;
			
			for (int j = i - 1; j >= 0; j--) {
				if (coa.getType(j) == ChartOfAccounts.TYPE_HEADING) {
					heading = coa.getHeading(j);

					if (heading.getLevel() < level) {
						if (headingIds.add(heading.getId())) {
							headings.add(heading);
						}
						
						level = heading.getLevel();
						maxLevel = Math.max(level, maxLevel);
						if (level == 0) break;
					}
				}
			}
		}
		
		Collections.sort(headings);
		coa = new ChartOfAccounts();
		coa.set(accounts, headings);
		accountLevel = maxLevel + 1;
		return coa;
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
