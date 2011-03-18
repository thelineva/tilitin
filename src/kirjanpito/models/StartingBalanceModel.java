package kirjanpito.models;

import java.math.BigDecimal;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.Registry;

/**
 * Malli alkusaldojen muokkausikkunalle.
 * 
 * @author Tommi Helineva
 */
public class StartingBalanceModel {
	private Registry registry;
	private Account[] accounts;
	private BigDecimal[] balances;
	private BigDecimal assetsTotal;
	private BigDecimal liabilitiesTotal;
	private Document document;
	private List<Entry> entries;
	private boolean changed;
	private boolean editable;
	
	public StartingBalanceModel(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Palauttaa <code>true</code>, jos alkusaldoihin on
	 * tehty muutoksia.
	 * 
	 * @return <code>true</code>, jos alkusaldoihin on
	 * tehty muutoksia
	 */
	public boolean isChanged() {
		return changed;
	}
	
	/**
	 * Ilmoittaa, voiko tietoja muokata.
	 * 
	 * @return <code>true</code>, jos tietoja voi muokata
	 */
	public boolean isEditable() {
		return editable;
	}

	public void initialize() throws DataAccessException
	{
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		int numRealAccounts = 0;
		
		try {
			sess = dataSource.openSession();
			document = dataSource.getDocumentDAO(
					sess).getByPeriodIdAndNumber(registry.getPeriod().getId(), 0);
			
			entries = dataSource.getEntryDAO(
					sess).getByDocumentId(document.getId());
		}
		finally {
			if (sess != null) sess.close();
		}
		
		editable = !registry.getPeriod().isLocked();
		
		/* Lasketaan taseeseen kuuluvien tilien lukumäärä. */
		for (Account account : registry.getAccounts()) {
			if (account.getType() == Account.TYPE_ASSET ||
					account.getType() == Account.TYPE_LIABILITY ||
					account.getType() == Account.TYPE_EQUITY ||
					account.getType() == Account.TYPE_PROFIT_PREV) {
				numRealAccounts++;
			}
		}
		
		AccountBalances balances = new AccountBalances(registry.getAccounts());
		
		/* Lasketaan vientien perusteella taseen tileille alkusaldot. */
		for (Entry entry : entries) {
			balances.addEntry(entry);
		}
		
		this.accounts = new Account[numRealAccounts];
		this.balances = new BigDecimal[numRealAccounts];
		updateAmounts(balances, null);
		calculateTotal();
	}
	
	/**
	 * Tallentaa alkusaldoihin tehdyt muutokset
	 * tietokantaan.
	 * 
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save() throws DataAccessException {
		Entry entry;
		BigDecimal balance;
		int entryIndex = 0;
		int index = 0;
		
		for (Account account : accounts) {
			if (balances[index].compareTo(BigDecimal.ZERO) != 0) {
				if (entryIndex >= entries.size()) {
					entry = new Entry();
					entries.add(entry);
				}
				else {
					entry = entries.get(entryIndex);
				}
				
				balance = balances[index];
				createStartingBalanceEntry(account, balance,
						document.getId(), entry);
				entryIndex++;
			}
			
			index++;
		}
		
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			
			for (int i = 0; i < entryIndex; i++) {
				dataSource.getEntryDAO(sess).save(entries.get(i));
			}
			
			/* Poistetaan ylimääräiset viennit,
			 * jos vientejä on ollut aikaisemmin enemmän. */
			for (int i = entryIndex; i < entries.size(); i++) {
				dataSource.getEntryDAO(sess).delete(entries.get(i).getId());
			}
			
			sess.commit();
		}
		finally {
			if (sess != null) sess.close();
		}
		
		changed = false;
	}
	
	public static void createStartingBalanceEntry(Account account,
			BigDecimal balance, int documentId, Entry entry) {
		
		/* Debet-vienti, jos
		 *  - vastaavaatili ja saldo > 0
		 *  - vastattavaa-, oma pääoma- tai edellisten
		 *    tilikausien voitto -tili ja saldo < 0
		 */
		entry.setDebit((account.getType() == Account.TYPE_ASSET &&
				balance.compareTo(BigDecimal.ZERO) > 0) ||
				((account.getType() == Account.TYPE_LIABILITY ||
						account.getType() == Account.TYPE_EQUITY ||
						account.getType() == Account.TYPE_PROFIT_PREV) &&
				balance.compareTo(BigDecimal.ZERO) < 0));
		
		if (balance.compareTo(BigDecimal.ZERO) < 0)
			balance = balance.negate();
		
		entry.setDocumentId(documentId);
		entry.setAccountId(account.getId());
		entry.setAmount(balance);
		entry.setDescription("Alkusaldo");
	}
	
	/**
	 * Palauttaa taseen tilien lukumäärän.
	 * 
	 * @return taseen tilien lukumäärä
	 */
	public int getAccountCount() {
		return accounts.length;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan rivin.
	 * 
	 * @param index rivinumero
	 * @return tili
	 */
	public Account getAccount(int index) {
		return accounts[index];
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin saldon.
	 * 
	 * @param index rivinumero
	 * @return tilin saldo
	 */
	public BigDecimal getBalance(int index) {
		return balances[index];
	}
	
	/**
	 * Asettaa rivillä <code>index</code> olevan tilin saldon.
	 * 
	 * @param index rivinumero
	 * @param value uusi saldo
	 */
	public void setBalance(int index, BigDecimal value) {
		balances[index] = value;
		changed = true;
		calculateTotal();
	}
	
	/**
	 * Palauttaa vastaavaa-summan.
	 * 
	 * @return vastaavaa yhteensä
	 */
	public BigDecimal getAssetsTotal() {
		return assetsTotal;
	}

	/**
	 * Palauttaa vastattavaa-summan.
	 * 
	 * @return vastattavaa yhteensä
	 */
	public BigDecimal getLiabilitiesTotal() {
		return liabilitiesTotal;
	}

	/**
	 * Kopioi alkusaldot edelliseltä tilikaudelta.
	 */
	public boolean copyFromPreviousPeriod() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		final AccountBalances balances = new AccountBalances(registry.getAccounts());
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			List<Period> periods = dataSource.getPeriodDAO(sess).getAll();
			Period currentPeriod = registry.getPeriod();
			Period prevPeriod = null;
			
			for (Period period : periods) {
				if (period.getId() == currentPeriod.getId()) {
					break;
				}
				
				prevPeriod = period;
			}
			
			if (prevPeriod == null) {
				return false;
			}
			
			dataSource.getEntryDAO(sess).getByPeriodId(prevPeriod.getId(), EntryDAO.ORDER_BY_DOCUMENT_NUMBER,
				new DTOCallback<Entry>() {
					public void process(Entry obj) {
						balances.addEntry(obj);
					}
				});
		}
		finally {
			if (sess != null) sess.close();
		}
		
		updateAmounts(balances, balances.getProfit());
		calculateTotal();
		changed = true;
		return true;
	}
	
	private void calculateTotal() {
		Account account;
		int index = 0;
		assetsTotal = BigDecimal.ZERO;
		liabilitiesTotal = BigDecimal.ZERO;
		
		for (BigDecimal balance : balances) {
			account = accounts[index];
			index++;
			
			if (account.getType() == Account.TYPE_ASSET) {
				assetsTotal = assetsTotal.add(balance);
			}
			else {
				liabilitiesTotal = liabilitiesTotal.add(balance);
			}
		}
	}
	
	private void updateAmounts(AccountBalances balances, BigDecimal profit) {
		BigDecimal balance;
		int index = 0;
		
		for (Account account : registry.getAccounts()) {
			if (account.getType() == Account.TYPE_ASSET ||
					account.getType() == Account.TYPE_LIABILITY ||
					account.getType() == Account.TYPE_EQUITY ||
					account.getType() == Account.TYPE_PROFIT_PREV)
			{
				balance = balances.getBalance(account.getId());
				
				if (balance == null)
					balance = BigDecimal.ZERO;
				
				if (account.getType() == Account.TYPE_PROFIT_PREV && profit != null)
					balance = balance.add(profit);
				
				this.accounts[index] = account;
				this.balances[index] = balance;
				index++;
			}
		}
	}
}
