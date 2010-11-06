package kirjanpito.util;

import java.util.List;

import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.Session;

public class SearchRecordSet extends DocumentRecordSet {
	private String q;
	
	public SearchRecordSet(DataSource dataSource,
			Period period, String q, AutoCompleteSupport autoCompleteSupport) {
		
		super(dataSource, period, autoCompleteSupport);
		this.q = q;
	}
	
	protected void fetchCount(Session sess) throws DataAccessException {
		setCount(getDataSource().getDocumentDAO(
				sess).getCountByPeriodIdAndPhrase(getPeriod().getId(), q));
	}
	
	protected void fetchDocuments(Session sess, int offset, int limit)
		throws DataAccessException {
		
		DataSource dataSource = getDataSource();
		List<Document> documents = dataSource.getDocumentDAO(
				sess).getByPeriodIdAndPhrase(getPeriod().getId(), q, offset, limit);
		
		updateDocuments(documents);
		clearEntries();
		
		if (documents.size() > 0) {
			dataSource.getEntryDAO(sess).getByDocuments(documents, new DTOCallback<Entry>() {
				public void process(Entry obj) {
					addEntry(obj);
				}
			});
		}
	}
}
