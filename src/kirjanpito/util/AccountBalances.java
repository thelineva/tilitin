package kirjanpito.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.Entry;

/**
 * Laskee tilien saldot vientien perusteella. Aluksi oliolle on annettava
 * tilit, joiden saldot lasketaan. Tilit voidaan antaa luotaessa oliota
 * tai <code>addAccount()</code>-metodilla.
 * 
 * @author Tommi Helineva
 */
public class AccountBalances {
	private HashMap<Integer, AccountBalance> balances;
	private BigDecimal profit;
	private int count;
	
	/**
	 * Luo <code>AccountBalances</code>-olion, jolle ei ole vielä
	 * asetettu tilejä, joiden saldot lasketaan.
	 */
	public AccountBalances() {
		balances = new HashMap<Integer, AccountBalance>();
		profit = BigDecimal.ZERO;
	}
	
	/**
	 * Luo <code>AccountBalances</code>-olion, joka laskee saldot
	 * tileille <code>accounts</code>.
	 * 
	 * @param accounts tilit
	 */
	public AccountBalances(List<Account> accounts) {
		this();
		
		for (Account account : accounts) {
			balances.put(account.getId(), new AccountBalance(account));
		}
	}
	
	/**
	 * Lisää tilin.
	 * 
	 * @param account tili
	 */
	public void addAccount(Account account) {
		balances.put(account.getId(), new AccountBalance(account));
	}
	
	/**
	 * Laskee tilin uuden saldon parametrina annetun viennin perusteella.
	 * 
	 * @param entry vienti
	 */
	public void addEntry(Entry entry) {
		AccountBalance ab = balances.get(entry.getAccountId());
		
		if (ab == null)
			return;
		
		BigDecimal amount = entry.getAmount();
		int type = ab.account.getType();
		boolean debit = entry.isDebit();
		
		/* Tilin saldo lasketaan seuraavan taulukon mukaan:
		 * 
		 * +-------------+--------+--------+
		 * | Account     | Debit  | Credit |
		 * +-------------+--------+--------+
		 * | Assets      |  INC   |  DEC   |
		 * | Expenses    |  INC   |  DEC   |
		 * | Liabilities |  DEC   |  INC   |
		 * | Equity      |  DEC   |  INC   |
		 * | Revenue     |  DEC   |  INC   |
		 * +-------------+--------+--------+
		 * 
		 * (INC = saldo kasvaa, DEC = saldo vähenee)
		 */
		
		if ((type == Account.TYPE_ASSET && !debit) ||
				(type == Account.TYPE_EXPENSE && !debit) ||
				(type == Account.TYPE_LIABILITY && debit) ||
				(type == Account.TYPE_EQUITY && debit) ||
				(type == Account.TYPE_REVENUE && debit) ||
				(type == Account.TYPE_PROFIT_PREV && debit) ||
				(type == Account.TYPE_PROFIT && debit))
		{
			amount = amount.negate();
		}
		
		if (type == Account.TYPE_EXPENSE) {
			profit = profit.subtract(amount);
		}
		else if (type == Account.TYPE_REVENUE) {
			profit = profit.add(amount);
		}
		
		if (ab.balance == null) {
			ab.balance = amount;
			count++;
		}
		else {
			ab.balance = ab.balance.add(amount);
		}
	}
	
	/**
	 * Nollaa saldot.
	 */
	public void reset() {
		for (AccountBalance ab : balances.values()) {
			ab.balance = null;
		}
		
		count = 0;
		profit = BigDecimal.ZERO;
	}
	
	/**
	 * Palauttaa tilin saldon.
	 * 
	 * @param id tilin tunniste
	 * @return tilin saldo tai <code>null</code>, jos tilille ei ole kirjattu
	 * yhtään vientiä
	 */
	public BigDecimal getBalance(int id) {
		AccountBalance ab = balances.get(id);
		return (ab == null) ? null : ab.balance;
	}
	
	/**
	 * Palauttaa niiden tilien lukumäärän, joiden saldo on asetettu.
	 * 
	 * @return tilien lukumäärä
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * Palauttaa tilikauden voiton.
	 * 
	 * @return tilikauden voitto
	 */
	public BigDecimal getProfit() {
		return profit;
	}

	private class AccountBalance {
		public Account account;
		public BigDecimal balance;
		
		public AccountBalance(Account account) {
			this.account = account;
		}
	}
}
