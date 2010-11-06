package kirjanpito.reports;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.Registry;

/**
 * Malli pääkirjatulosteelle.
 * 
 * @author Tommi Helineva
 */
public class GeneralLedgerModel implements PrintModel {
	protected Registry registry;
	protected Period period;
	protected Settings settings;
	private List<GeneralLedgerRow> rows;
	private int prevAccountId;

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
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

	public void run() throws DataAccessException {
		List<Document> documents;
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		final HashMap<Integer, Document> documentMap =
			new HashMap<Integer, Document>();
		
		final AccountBalances balances = new AccountBalances(registry.getAccounts());
		
		settings = registry.getSettings();
		prevAccountId = -1;
		rows = new ArrayList<GeneralLedgerRow>();
		
		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(
					sess).getByPeriodId(period.getId(), 0);
			
			for (Document d : documents) {
				documentMap.put(d.getId(), d);
			}
			
			dataSource.getEntryDAO(sess).getByPeriodId(
				period.getId(), EntryDAO.ORDER_BY_ACCOUNT_NUMBER,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());
						Document document = documentMap.get(entry.getDocumentId());
						
						if (account == null || document == null) {
							return;
						}
						
						balances.addEntry(entry);
						
						if (prevAccountId != account.getId()) {
							if (prevAccountId != -1)
								rows.add(new GeneralLedgerRow(0, null, null, null, null));
							
							rows.add(new GeneralLedgerRow(2, null, account, null, null));
						}

						rows.add(new GeneralLedgerRow(1, document, account, entry,
								balances.getBalance(entry.getAccountId())));
						
						prevAccountId = account.getId();
					}
				});
		}
		finally {
			if (sess != null) sess.close();
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
	 * Palauttaa tulosteessa olevien rivien lukumäärän.
	 * 
	 * @return rivien lukumäärä
	 */
	public int getRowCount() {
		return rows.size();
	}
	
	/**
	 * Palauttaa rivin <code>index</code> tyypin.
	 * 
	 * @param index rivinumero
	 * @return tyyppi
	 */
	public int getType(int index) {
		return rows.get(index).type;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tositteen.
	 * 
	 * @param index rivinumero
	 * @return tosite
	 */
	public Document getDocument(int index) {
		return rows.get(index).document;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin.
	 * 
	 * @param index rivinumero
	 * @return tili
	 */
	public Account getAccount(int index) {
		return rows.get(index).account;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan viennin.
	 * 
	 * @param index rivinumero
	 * @return vienti
	 */
	public Entry getEntry(int index) {
		return rows.get(index).entry;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin
	 * saldon.
	 * 
	 * @param index rivinumero
	 * @return tilin saldo
	 */
	public BigDecimal getBalance(int index) {
		return rows.get(index).balance;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tositteen
	 * tositelajin
	 * 
	 * @param index rivinumero
	 * @return tositelaji
	 */
	public DocumentType getDocumentType(int index) {
		return null;
	}
	
	private class GeneralLedgerRow {
		public int type;
		public Document document;
		public Account account;
		public Entry entry;
		public BigDecimal balance;
		
		public GeneralLedgerRow(int type, Document document, Account account,
				Entry entry, BigDecimal balance) {
			
			this.type = type;
			this.document = document;
			this.account = account;
			this.entry = entry;
			this.balance = balance;
		}
	}
}