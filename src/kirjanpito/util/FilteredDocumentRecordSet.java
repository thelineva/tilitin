package kirjanpito.util;

import java.util.List;

import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.Period;
import kirjanpito.db.Session;

public class FilteredDocumentRecordSet extends DocumentRecordSet {
	private int startNumber;
	private int endNumber;
	
	public FilteredDocumentRecordSet(DataSource dataSource,
			Period period, DocumentType type, AutoCompleteSupport autoCompleteSupport) {
		
		super(dataSource, period, autoCompleteSupport);
		
		if (type == null) {
			startNumber = 1;
			endNumber = Integer.MAX_VALUE;
		}
		else {
			startNumber = type.getNumberStart();
			endNumber = type.getNumberEnd();
		}
	}

	protected void fetchCount(Session sess) throws DataAccessException {
		setCount(getDataSource().getDocumentDAO(
				sess).getCountByPeriodIdAndNumber(getPeriod().getId(),
						startNumber, endNumber));
	}
	
	protected void fetchDocuments(Session sess, int offset, int limit)
		throws DataAccessException {
		
		DataSource dataSource = getDataSource();
		List<Document> documents = dataSource.getDocumentDAO(
				sess).getByPeriodIdAndNumber(getPeriod().getId(), startNumber,
						endNumber, offset, limit);
		
		updateDocuments(documents);
		clearEntries();
		
		if (documents.size() > 0) {
			int firstNumber = documents.get(0).getNumber();
			int lastNumber = documents.get(documents.size() - 1).getNumber();
			dataSource.getEntryDAO(sess).getByPeriodIdAndNumber(
					getPeriod().getId(), firstNumber, lastNumber, new DTOCallback<Entry>() {
						public void process(Entry obj) {
							addEntry(obj);
						}
					});
		}
	}
}
