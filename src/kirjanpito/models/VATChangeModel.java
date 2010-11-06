package kirjanpito.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.db.Account;
import kirjanpito.ui.Kirjanpito;
import kirjanpito.util.AppSettings;
import kirjanpito.util.Registry;
import kirjanpito.util.VATUtil;

public class VATChangeModel {
	private Registry registry;
	private ArrayList<VATChangeRule> rules;
	private Account newAccount;
	private Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	
	public VATChangeModel(Registry registry) {
		this.registry = registry;
		rules = new ArrayList<VATChangeRule>();
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
		rules.get(index).oldVatIndex = (account == null) ? -1 : account.getVatRate();
		Collections.sort(rules);
	}
	
	public Account getAccount(int index) {
		return registry.getAccountById(rules.get(index).accountId);
	}
	
	public int getOldVatIndex(int index) {
		return rules.get(index).oldVatIndex;
	}
	
	public void setOldVatIndex(int index, int vat) {
		rules.get(index).oldVatIndex = vat;
	}
	
	public int getNewVatIndex(int index) {
		return rules.get(index).newVatIndex;
	}
	
	public void setNewVatIndex(int index, int vat) {
		rules.get(index).newVatIndex = vat;
	}
	
	public void addRule() {
		rules.add(new VATChangeRule(-1, -1, 6)); // 13 %
		Collections.sort(rules);
	}
	
	public void removeRule(int index) {
		rules.remove(index);
	}
	
	public void load() {
		AppSettings settings = AppSettings.getInstance();
		File file = new File(settings.getDirectoryPath(), "alv-muutokset.txt");
		
		if (!file.exists()) {
			return;
		}
		
		logger.info("Luetaan ALV-muutossäännöt tiedostosta " + file);
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\\s+");
				int oldVatIndex, newVatIndex;
				
				if (cols.length != 3) {
					continue;
				}
				
				try {
					oldVatIndex = Integer.parseInt(cols[1]);
					newVatIndex = Integer.parseInt(cols[2]);
				}
				catch (NumberFormatException e) {
					continue;
				}
				
				if (oldVatIndex < 0 || oldVatIndex >= VATUtil.VAT_RATES.length) {
					continue;
				}
				
				if (newVatIndex < 0 || newVatIndex >= VATUtil.VAT_RATES.length) {
					continue;
				}
				
				if (cols[0].equals("-")) {
					rules.add(new VATChangeRule(-1, oldVatIndex, newVatIndex));
				}
				else {
					addRule(cols[0], oldVatIndex, newVatIndex);
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
		rules.add(new VATChangeRule(-1, 1, 7)); // 22 % -> 23 %
		rules.add(new VATChangeRule(-1, 4, 6)); // 12 % -> 13 %
		rules.add(new VATChangeRule(-1, 3, 5)); //  8 % ->  9 %
	}
	
	private void addRule(String accountNumber, int oldVatIndex, int newVatIndex) {
		Account account = registry.getAccountByNumber(accountNumber);
		
		if (account != null) {
			rules.add(new VATChangeRule(account.getId(), oldVatIndex, newVatIndex));
		}
	}
	
	public void save() throws IOException {
		AppSettings settings = AppSettings.getInstance();
		File file = new File(settings.getDirectoryPath(), "alv-muutokset.txt");
		logger.info("Tallennetaan ALV-muutossäännöt tiedostoon " + file);
		FileWriter writer = new FileWriter(file);
		String newline = System.getProperty("line.separator");
		
		for (VATChangeRule rule : rules) {
			if (rule.accountId < 0) {
				writer.append('-');
			}
			else {
				writer.append(registry.getAccountById(rule.accountId).getNumber());
			}
			
			writer.append(' ').append(Integer.toString(rule.oldVatIndex));
			writer.append(' ').append(Integer.toString(rule.newVatIndex));
			writer.append(newline);
		}
		
		writer.close();
	}
	
	public boolean isChangesDone() {
		int count = 0;
		
		/* Muutokset on jo tehty, jos tilikartasta löytyy vähintään 150 tiliä,
		 * joiden ALV-prosentti on 23. */
		
		for (Account account : registry.getAccounts()) {
			if (account.getVatRate() == 7) { // 23 %
				count++;
			}
		}
		
		return count > 150;
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
				if (account.getVatRate() == rule.oldVatIndex) {
					if (changeVatRate(account, rule.newVatIndex)) {
						changed = true;
					}
					
					break;
				}
			}
			else if (rule.accountId == account.getId()) {
				if (changeVatRate(account, rule.newVatIndex)) {
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
	
	private boolean changeVatRate(Account account, int vatRate) {
		if (account.getVatRate() == vatRate) {
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
		sb.append(", ").append(VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[account.getVatRate()]]);
		String oldName = sb.toString();
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine(String.format("Tili %s %s -> %s %s", account.getNumber(),
					account.getName(), oldNumber, oldName));
			
			logger.fine(String.format("Uusi tili %s %s (%s)", newAccount.getNumber(),
					newAccount.getName(), VATUtil.VAT_RATE_TEXTS[VATUtil.VAT_RATE_M2V[newAccount.getVatRate()]]));
		}
		
		account.setNumber(oldNumber);
		account.setName(oldName);
		return true;
	}

	private class VATChangeRule implements Comparable<VATChangeRule> {
		public int accountId;
		public int oldVatIndex;
		public int newVatIndex;
		
		public VATChangeRule(int accountId, int oldVatIndex, int newVatIndex) {
			this.accountId = accountId;
			this.oldVatIndex = oldVatIndex;
			this.newVatIndex = newVatIndex;
		}

		public int compareTo(VATChangeRule o) {
			if (accountId < 0 || o.accountId < 0) {
				return accountId - o.accountId;
			}
			else {
				return registry.getAccountById(accountId).getNumber().compareTo(
						registry.getAccountById(o.accountId).getNumber());
			}
		}
	}
}
