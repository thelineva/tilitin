package kirjanpito.reports;

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
import kirjanpito.util.Registry;

/**
 * Malli päiväkirjatulosteelle.
 * 
 * @author Tommi Helineva
 */
public class GeneralJournalModel implements PrintModel {
	private Registry registry;
	private Settings settings;
	private Period period;
	private List<DocumentType> documentTypes;
	private List<GeneralJournalRow> rows;
	private int prevDocumentId;
	private int prevDocumentTypeId;

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
		
		settings = registry.getSettings();
		documentTypes = registry.getDocumentTypes();
		prevDocumentId = -1;
		rows = new ArrayList<GeneralJournalRow>();
		
		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(
					sess).getByPeriodId(period.getId(), 1);
			
			for (Document d : documents) {
				documentMap.put(d.getId(), d);
			}
			
			documents = null;
			dataSource.getEntryDAO(sess).getByPeriodId(
				period.getId(), EntryDAO.ORDER_BY_DOCUMENT_NUMBER,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());
						Document document = documentMap.get(entry.getDocumentId());
						
						if (account == null || document == null) {
							return;
						}
						
						DocumentType documentType = getDocumentTypeByNumber(document.getNumber());
						
						if (documentType != null && documentType.getId() != prevDocumentTypeId) {
							rows.add(new GeneralJournalRow(0, null, null, null, null));
							rows.add(new GeneralJournalRow(3, null, documentType, null, null));
							rows.add(new GeneralJournalRow(0, null, null, null, null));
							prevDocumentTypeId = documentType.getId();
						}
						
						if (prevDocumentId != document.getId()) {
							rows.add(new GeneralJournalRow(2, document, null, null, null));
						}
						
						rows.add(new GeneralJournalRow(1, document, null, account, entry));
						prevDocumentId = document.getId();
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
	 * @return tosite
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
	 * Palauttaa rivillä <code>index</code> olevan tositteen
	 * tositelajin
	 * 
	 * @param index rivinumero
	 * @return tositelaji
	 */
	public DocumentType getDocumentType(int index) {
		return rows.get(index).documentType;
	}
	
	private DocumentType getDocumentTypeByNumber(int number) {
		for (DocumentType type : documentTypes) {
			if (number >= type.getNumberStart() && number <= type.getNumberEnd()) {
				return type;
			}
		}
		
		return null;
	}
	
	private class GeneralJournalRow {
		public int type;
		public Document document;
		public DocumentType documentType;
		public Account account;
		public Entry entry;
		
		public GeneralJournalRow(int type, Document document, DocumentType documentType,
				Account account, Entry entry) {
			
			this.type = type;
			this.document = document;
			this.documentType = documentType;
			this.account = account;
			this.entry = entry;
		}
	}
}
