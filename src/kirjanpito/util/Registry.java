package kirjanpito.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.DocumentType;
import kirjanpito.db.EntryTemplate;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;

public class Registry {
	private ArrayList<RegistryListener> listeners;
	private DataSource dataSource;
	private Settings settings;
	private Period period;
	private List<Account> accounts;
	private List<COAHeading> coaHeadings;
	private List<EntryTemplate> entryTemplates;
	private List<DocumentType> documentTypes;
	private ChartOfAccounts coa;
	private Map<Integer, Account> accountMap;
	
	public Registry() {
		listeners = new ArrayList<RegistryListener>();
		coa = new ChartOfAccounts();
	}
	
	/**
	 * Lisää kuuntelijan.
	 * 
	 * @param listener kuuntelija
	 */
	public void addListener(RegistryListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Poistaa kuuntelijan.
	 * 
	 * @param listener
	 */
	public void removeListener(RegistryListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Palauttaa tietokannan, josta tositetiedot haetaan.
	 * 
	 * @return tietokanta
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Asettaa tietokannan, josta tositetiedot haetaan.
	 * 
	 * @param dataSource tietokanta
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
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
	 * Palauttaa nykyisen tilikauden.
	 * 
	 * @return tilikausi
	 */
	public Period getPeriod() {
		return period;
	}
	
	/**
	 * Asettaa nykyisen tilikauden.
	 * 
	 * @param period tilikausi
	 */
	public void setPeriod(Period period) {
		this.period = period;
	}
	
	/**
	 * Palauttaa tilikartan tilit.
	 * 
	 * @return tilikartan tilit
	 */
	public List<Account> getAccounts() {
		return accounts;
	}
	
	/**
	 * Palauttaa tilikartan otsikot.
	 * 
	 * @return tilikartan otsikot
	 */
	public List<COAHeading> getCOAHeadings() {
		return coaHeadings;
	}
	
	/**
	 * Palauttaa tilikartan.
	 * 
	 * @return tilikartta
	 */
	public ChartOfAccounts getChartOfAccounts() {
		return coa;
	}
	
	/**
	 * Palauttaa vientimallit.
	 * 
	 * @return vientimallit
	 */
	public List<EntryTemplate> getEntryTemplates() {
		return entryTemplates;
	}
	
	/**
	 * Palauttaa tositelajit.
	 * 
	 * @return tositelajit
	 */
	public List<DocumentType> getDocumentTypes() {
		return documentTypes;
	}
	
	/**
	 * Palauttaa tilin, jonka tunniste on <code>id</code>.
	 * 
	 * @param id haettavan tilin tunniste
	 * @return löytynyt tili tai <code>null</code>, jos tiliä
	 * ei löydy
	 */
	public Account getAccountById(int id) {
		return accountMap.get(id);
	}
	
	/**
	 * Palauttaa tilin, jonka numero on <code>number</code>.
	 * 
	 * @param number haettavan tilin numero
	 * @return löytynyt tili tai <code>null</code>, jos tiliä
	 * ei löydy
	 */
	public Account getAccountByNumber(String number) {
		for (Account a : accounts) {
			if (a.getNumber().equals(number)) {
				return a;
			}
		}
		
		return null;
	}
	
	public void clear() {
		settings = null;
		period = null;
		accounts = null;
		coaHeadings = null;
		entryTemplates = null;
		documentTypes = null;
		accountMap = null;
	}
	
	public boolean fetchPeriod(Session sess) throws DataAccessException {
		period = dataSource.getPeriodDAO(sess).getCurrent();
		return period != null;
	}
	
	public void fetchSettings(Session sess) throws DataAccessException {
		settings = dataSource.getSettingsDAO(sess).get();
	}
	
	public void fetchChartOfAccounts() throws DataAccessException {
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			fetchChartOfAccounts(sess);
		}
		finally {
			if (sess != null) sess.close();
		}
	}
	
	public void fetchChartOfAccounts(Session sess) throws DataAccessException {
		accounts = dataSource.getAccountDAO(sess).getAll();
		coaHeadings = dataSource.getCOAHeadingDAO(sess).getAll();
		updateChartOfAccounts();
	}
	
	public void updateChartOfAccounts() {
		coa.set(accounts, coaHeadings);
		
		/* Luodaan tileille hajautustaulu, jotta tilit
		 * löytyvät nopeasti tunnisteen perusteella. */
		accountMap = new HashMap<Integer, Account>();
		
		for (Account account : accounts) {
			accountMap.put(account.getId(), account);
		}
	}
	
	public void fetchEntryTemplates() throws DataAccessException {
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			fetchEntryTemplates(sess);
		}
		finally {
			if (sess != null) sess.close();
		}
	}
	
	public void fetchEntryTemplates(Session sess) throws DataAccessException {
		entryTemplates = dataSource.getEntryTemplateDAO(sess).getAll();
	}
	
	public void fetchDocumentTypes() throws DataAccessException {
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			fetchDocumentTypes(sess);
		}
		finally {
			if (sess != null) sess.close();
		}
	}
	
	public void fetchDocumentTypes(Session sess) throws DataAccessException {
		documentTypes = dataSource.getDocumentTypeDAO(sess).getAll();
	}
	
	public void fireSettingsChanged() {
		for (RegistryListener listener : listeners) {
			listener.settingsChanged();
		}
	}
	
	public void firePeriodChanged() {
		for (RegistryListener listener : listeners) {
			listener.periodChanged();
		}
	}
	
	public void fireChartOfAccountsChanged() {
		for (RegistryListener listener : listeners) {
			listener.chartOfAccountsChanged();
		}
	}
	
	public void fireEntryTemplatesChanged() {
		for (RegistryListener listener : listeners) {
			listener.entryTemplatesChanged();
		}
	}
	
	public void fireDocumentTypesChanged() {
		for (RegistryListener listener : listeners) {
			listener.documentTypesChanged();
		}
	}
}
