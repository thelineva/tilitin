package kirjanpito.util;

import java.util.List;

import kirjanpito.db.Document;
import kirjanpito.db.Entry;

public class DummyAutoCompleteSupport implements AutoCompleteSupport {
	public void addDocuments(List<Document> documents) {
	}

	public void addEntry(Entry entry) {
	}
	
	public void clear() {
	}

	public String autoCompleteEntryDescription(int accountId, String description) {
		return null;
	}
}
