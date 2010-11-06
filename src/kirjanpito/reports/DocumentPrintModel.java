package kirjanpito.reports;

import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.Registry;

public class DocumentPrintModel implements PrintModel {
	private Registry registry;
	private Settings settings;
	private Document document;
	private List<Entry> entries;

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public void run() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		settings = registry.getSettings();
		
		try {
			sess = dataSource.openSession();
			entries = dataSource.getEntryDAO(sess
					).getByDocumentId(document.getId());
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
	 * Palauttaa vientien lukumäärän.
	 * 
	 * @return vientien lukumäärä
	 */
	public int getEntryCount() {
		return entries.size();
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan viennin.
	 * 
	 * @param index rivinumero
	 * @return vienti
	 */
	public Entry getEntry(int index) {
		return entries.get(index);
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan viennin tilin.
	 * 
	 * @param index rivinumero
	 * @return tili
	 */
	public Account getAccount(int index) {
		return registry.getAccountById(entries.get(index).getAccountId());
	}
}
