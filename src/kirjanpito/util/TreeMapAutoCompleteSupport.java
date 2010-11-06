package kirjanpito.util;

import java.util.List;
import java.util.TreeMap;

import kirjanpito.db.Document;
import kirjanpito.db.Entry;

public class TreeMapAutoCompleteSupport implements AutoCompleteSupport {
	private TreeMap<String, String> map;
	private static final int MAX_NUMBER_OF_ENTRIES = 500;
	
	public TreeMapAutoCompleteSupport() {
		map = new TreeMap<String, String>();
	}
	
	public void addDocuments(List<Document> documents) {
		if (map.size() > MAX_NUMBER_OF_ENTRIES) {
			map.clear();
		}
	}

	public void addEntry(Entry entry) {
		if (!entry.getDescription().isEmpty()) {
			map.put(entry.getAccountId() + entry.getDescription().toLowerCase(),
					entry.getDescription());
		}
	}
	
	public void clear() {
		map.clear();
	}

	public String autoCompleteEntryDescription(int accountId, String description) {
		java.util.Map.Entry<String, String> e = map.ceilingEntry(
				accountId + description.toLowerCase());
		
		if (e != null && e.getValue().regionMatches(true, 0, description, 0, description.length())) {
			return e.getValue();
		}
		else {
			return null;
		}
	}
}