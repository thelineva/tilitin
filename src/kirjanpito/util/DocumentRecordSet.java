package kirjanpito.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.Session;

public abstract class DocumentRecordSet {
	private DataSource dataSource;
	private Period period;
	private List<Document> documents;
	private HashMap<Integer, List<Entry>> entryMap;
	private int pageSize;
	private int offset;
	private int index;
	private int count;
	private AutoCompleteSupport autoCompleteSupport;
	
	public DocumentRecordSet(DataSource dataSource, Period period,
			AutoCompleteSupport autoCompleteSupport) {
		this.dataSource = dataSource;
		this.period = period;
		this.pageSize = 10;
		this.offset = -1;
		this.index = -1;
		this.count = 0;
		this.autoCompleteSupport = autoCompleteSupport;
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
	 * Palauttaa tilikauden, jolta tositetiedot haetaan.
	 * 
	 * @return tilikausi
	 */
	public Period getPeriod() {
		return period;
	}

	/**
	 * Asettaa tilikauden, jolta tositetiedot haetaan.
	 * 
	 * @return tilikausi
	 */
	public void setPeriod(Period period) {
		this.period = period;
	}

	public AutoCompleteSupport getAutoCompleteSupport() {
		return autoCompleteSupport;
	}

	public void setAutoCompleteSupport(AutoCompleteSupport autoCompleteSupport) {
		this.autoCompleteSupport = autoCompleteSupport;
	}

	/**
	 * Palauttaa tositteen järjestysnumeron.
	 * 
	 * @return tositteen järjestysnumero
	 */
	public int getPosition() {
		return offset + index;
	}
	
	/**
	 * Palauttaa tositteiden lukumäärän.
	 * 
	 * @return tositteiden lukumäärä
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * Palauttaa tositteen.
	 * 
	 * @return tosite
	 */
	public Document getDocument() {
		return documents.get(index);
	}
	
	/**
	 * Palauttaa tositteen viennit.
	 * 
	 * @return tositteen viennit
	 */
	public List<Entry> getEntries() {
		return entryMap.get(getDocument().getId());
	}
	
	/**
	 * Tallettaa muuttuneen tositteen tiedot välimuistiin.
	 * 
	 * @param document tosite
	 */
	public void setDocument(Document document) {
		documents.set(index, document);
	}
	
	/**
	 * Tallettaa muuttuneet viennit välimuistiin.
	 * 
	 * @param entries viennit
	 */
	public void setEntries(List<Entry> entries) {
		entryMap.put(getDocument().getId(), entries);
	}
	
	/**
	 * Lisää uuden tositteen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void add() throws DataAccessException {
		documents.add(null);
		index++;
		count++;
	}
	
	/**
	 * Poistaa tositteen välimuistista.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void remove() throws DataAccessException {
		documents.remove(index);
		count--;
		
		if (count == 0)
			index = -1;
	}
	
	/**
	 * Hakee tositetiedot tietokannasta.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void open() throws DataAccessException {
		Session sess = null;

		try {
			sess = dataSource.openSession();
			open(sess);
		}
		finally {
			if (sess != null) sess.close();
		}
	}
	
	/**
	 * Hakee tositetiedot tietokannasta.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void open(Session sess) throws DataAccessException {
		fetchCount(sess);
		
		if (count > 0) {
			last(sess);
		}
		else {
			updateDocuments(new ArrayList<Document>());
			clearEntries();
		}
	}
	
	/**
	 * Siirtyy tositteeseen, jonka järjestysnumero on <code>i</code>
	 * @param i tositteen järjestysnumero
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void move(int i) throws DataAccessException {
		if (documents != null && i >= offset && i < offset + documents.size()) {
			index = i - offset;
			return;
		}
		
		Session sess = null;
		int newOffset = i - (i % pageSize);

		try {
			sess = dataSource.openSession();
			move(sess, i);
		}
		finally {
			if (sess != null) sess.close();
		}
		
		offset = newOffset;
		index = i - newOffset;
	}
	
	/**
	 * Siirtyy tositteeseen, jonka järjestysnumero on <code>i</code>
	 * @param i tositteen järjestysnumero
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void move(Session sess, int i) throws DataAccessException {
		if (documents != null && i >= offset && i < offset + documents.size()) {
			index = i - offset;
			return;
		}
		
		int newOffset = i - (i % pageSize);
		fetchDocuments(sess, newOffset, pageSize);
		offset = newOffset;
		index = i - newOffset;
	}
	
	/**
	 * Siirtyy seuraavaan tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void next() throws DataAccessException {
		move(offset + index + 1);
	}
	
	/**
	 * Siirtyy seuraavaan tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void next(Session sess) throws DataAccessException {
		move(sess, offset + index + 1);
	}
	
	/**
	 * Siirtyy edelliseen tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void previous() throws DataAccessException {
		move(offset + index - 1);
	}
	
	/**
	 * Siirtyy edelliseen tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void previous(Session sess) throws DataAccessException {
		move(sess, offset + index - 1);
	}
	
	/**
	 * Siirtyy ensimmäiseen tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void first() throws DataAccessException {
		move(0);
	}
	
	/**
	 * Siirtyy ensimmäiseen tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void first(Session sess) throws DataAccessException {
		move(sess, 0);
	}
	
	/**
	 * Siirtyy viimeiseen tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void last() throws DataAccessException {
		move(count - 1);
	}
	
	/**
	 * Siirtyy viimeiseen tositteeseen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void last(Session sess) throws DataAccessException {
		move(sess, count - 1);
	}
	
	protected void updateDocuments(List<Document> documents) {
		this.documents = documents;
		autoCompleteSupport.addDocuments(documents);
	}
	
	protected void clearEntries() {
		entryMap = new HashMap<Integer, List<Entry>>();
	}
	
	protected void addEntry(Entry entry) {
		List<Entry> entryList = entryMap.get(entry.getDocumentId());
		
		if (entryList == null) {
			entryList = new ArrayList<Entry>();
			entryMap.put(entry.getDocumentId(), entryList);
		}
		
		entryList.add(entry);
		autoCompleteSupport.addEntry(entry);
	}
	
	protected void setCount(int count) {
		this.count = count;
	}
	
	protected abstract void fetchCount(Session sess)
			throws DataAccessException;
	
	protected abstract void fetchDocuments(Session sess,
			int offset, int limit) throws DataAccessException;
}
