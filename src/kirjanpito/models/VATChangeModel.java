package kirjanpito.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.db.Account;
import kirjanpito.ui.Kirjanpito;
import kirjanpito.util.AppSettings;
import kirjanpito.util.Registry;

public class VATChangeModel {
	private Registry registry;
	private ArrayList<VATChangeRule> rules;
	private Account newAccount;
	private DecimalFormat formatter;
	private Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);

	public VATChangeModel(Registry registry) {
		this.registry = registry;
		rules = new ArrayList<VATChangeRule>();
		formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
	}

	public int getRuleCount() {
		return rules.size();
	}

	public int getAccountId(int index) {
		return rules.get(index).accountId;
	}

	public void setAccountId(int index, int accountId) {
		Account account = registry.getAccountById(accountId);
		rules.get(index).accountId = accountId;
		rules.get(index).oldVatRate = (account == null) ? BigDecimal.ZERO : account.getVatRate();
	}

	public Account getAccount(int index) {
		return registry.getAccountById(rules.get(index).accountId);
	}

	public BigDecimal getOldVatRate(int index) {
		return rules.get(index).oldVatRate;
	}

	public void setOldVatRate(int index, BigDecimal rate) {
		rules.get(index).oldVatRate = rate;
	}

	public BigDecimal getNewVatRate(int index) {
		return rules.get(index).newVatRate;
	}

	public void setNewVatRate(int index, BigDecimal rate) {
		rules.get(index).newVatRate = rate;
	}

	public void addRule() {
		rules.add(new VATChangeRule(-1, BigDecimal.ZERO, BigDecimal.ZERO));
	}

	public void removeRule(int index) {
		rules.remove(index);
	}

	public void sortRules() {
		Collections.sort(rules);
	}

	public void load() {
		AppSettings settings = AppSettings.getInstance();
		File file = new File(settings.getDirectoryPath(), "alv-muutokset.txt");
		DecimalFormat formatter = new DecimalFormat();
		formatter.setParseBigDecimal(true);

		if (!file.exists()) {
			return;
		}

		logger.info("Luetaan ALV-muutossäännöt tiedostosta " + file);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\\s+");
				BigDecimal oldVatRate, newVatRate;

				if (cols.length != 3) {
					continue;
				}

				try {
					oldVatRate = (BigDecimal)formatter.parse(cols[1]);
					newVatRate = (BigDecimal)formatter.parse(cols[2]);
				}
				catch (ParseException e) {
					continue;
				}

				if (cols[0].equals("-")) {
					rules.add(new VATChangeRule(-1, oldVatRate, newVatRate));
				}
				else {
					addRule(cols[0], oldVatRate, newVatRate);
				}
			}

			reader.close();
		}
		catch (IOException e) {
			addDefaultRules();
		}
	}

	public void addDefaultRules() {
		rules.clear();
		rules.add(new VATChangeRule(-1, new BigDecimal("23"), new BigDecimal("24")));
		rules.add(new VATChangeRule(-1, new BigDecimal("13"), new BigDecimal("14")));
		rules.add(new VATChangeRule(-1, new BigDecimal("9"), new BigDecimal("10")));
	}

	private void addRule(String accountNumber, BigDecimal oldPercent, BigDecimal newPercent) {
		Account account = registry.getAccountByNumber(accountNumber);

		if (account != null) {
			rules.add(new VATChangeRule(account.getId(), oldPercent, newPercent));
		}
	}

	public void save() throws IOException {
		AppSettings settings = AppSettings.getInstance();
		File file = new File(settings.getDirectoryPath(), "alv-muutokset.txt");
		logger.info("Tallennetaan ALV-muutossäännöt tiedostoon " + file);
		FileWriter writer = new FileWriter(file);
		String newline = System.getProperty("line.separator");
		DecimalFormat formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);

		for (VATChangeRule rule : rules) {
			if (rule.accountId < 0) {
				writer.append('-');
			}
			else {
				writer.append(registry.getAccountById(rule.accountId).getNumber());
			}

			writer.append(' ').append(formatter.format(rule.oldVatRate));
			writer.append(' ').append(formatter.format(rule.newVatRate));
			writer.append(newline);
		}

		writer.close();
	}

	public boolean updateAccount(Account account) {
		int accountVatCode = account.getVatCode();

		if (accountVatCode != 4 && accountVatCode != 5 && accountVatCode != 9) {
			return false;
		}

		boolean changed = false;

		for (int i = rules.size() - 1; i >= 0; i--) {
			VATChangeRule rule = rules.get(i);

			if (rule.accountId < 0) {
				if (account.getVatRate().compareTo(rule.oldVatRate) == 0) {
					if (changeVatRate(account, rule.newVatRate)) {
						changed = true;
					}

					break;
				}
			}
			else if (rule.accountId == account.getId()) {
				if (changeVatRate(account, rule.newVatRate)) {
					changed = true;
				}

				break;
			}
		}

		return changed;
	}

	public Account getNewAccount() {
		return newAccount;
	}

	private boolean changeVatRate(Account account, BigDecimal vatRate) {
		if (account.getVatRate().compareTo(vatRate) == 0) {
			return false;
		}

		/* Luodaan uusi tili. */
		newAccount = new Account();
		newAccount.setNumber(account.getNumber());
		newAccount.setName(account.getName());
		newAccount.setType(account.getType());
		newAccount.setVatCode(account.getVatCode());
		newAccount.setVatRate(vatRate);
		newAccount.setVatAccount1Id(account.getVatAccount1Id());
		newAccount.setVatAccount2Id(account.getVatAccount2Id());

		/* Muodostetaan vanhalle tilille uusi numero. */
		int suffix = 1;
		String oldNumber = account.getNumber() + suffix;

		while (registry.getAccountByNumber(oldNumber) != null) {
			suffix += 1;
			oldNumber = account.getNumber() + suffix;
		}

		/* Muodostetaan vanhalle tilille uusi nimi. */
		StringBuilder sb = new StringBuilder(account.getName());
		sb.append(", ").append(formatter.format(account.getVatRate())).append(" %");
		String oldName = sb.toString();

		if (logger.isLoggable(Level.FINE)) {
			logger.fine(String.format("Tili %s %s -> %s %s", account.getNumber(),
					account.getName(), oldNumber, oldName));

			logger.fine(String.format("Uusi tili %s %s (%s %%)", newAccount.getNumber(),
					newAccount.getName(), formatter.format(account.getVatRate())));
		}

		account.setNumber(oldNumber);
		account.setName(oldName);
		return true;
	}

	private class VATChangeRule implements Comparable<VATChangeRule> {
		public int accountId;
		public BigDecimal oldVatRate;
		public BigDecimal newVatRate;

		public VATChangeRule(int accountId, BigDecimal oldRate, BigDecimal newRate) {
			this.accountId = accountId;
			this.oldVatRate = oldRate;
			this.newVatRate = newRate;
		}

		public int compareTo(VATChangeRule o) {
			if (accountId < 0 || o.accountId < 0) {
				return accountId - o.accountId;
			}
			else {
				if (accountId == 0 && o.accountId == 0) {
					return o.newVatRate.compareTo(newVatRate);
				}

				return registry.getAccountById(accountId).getNumber().compareTo(
						registry.getAccountById(o.accountId).getNumber());
			}
		}
	}
}
