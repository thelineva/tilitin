package kirjanpito.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class CSVReader {
	private Reader reader;
	
	public CSVReader(Reader reader) {
		this.reader = reader;
	}
	
	public String[] readLine() throws IOException {
		ArrayList<String> fields = new ArrayList<String>();
		StringBuilder buffer = new StringBuilder();
		boolean quoted = false;
		int c = 0;
		int p = 0;
		
		while ((c = reader.read()) >= 0) {
			if (c == '"') {
				quoted = !quoted;
				
				if (p == '"') {
					buffer.append((char)c);
					c = 0;
				}
			}
			else if (c == ',' && !quoted) {
				fields.add(buffer.toString());
				buffer = new StringBuilder();
			}
			else if (c == '\n') {
				break;
			}
			else if (c != '\r') {
				buffer.append((char)c);
			}
			
			p = c;
		}
		
		if (c < 0) return null;
		fields.add(buffer.toString());
		String[] fieldArray = new String[fields.size()];
		fields.toArray(fieldArray);
		return fieldArray;
	}
}
