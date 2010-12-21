package kirjanpito.util;

import java.io.IOException;
import java.io.Writer;

public class CSVWriter {
	private Writer writer;
	private boolean firstField;
	private char delimiter;
	
	public CSVWriter(Writer writer) {
		this.writer = writer;
		this.firstField = true;
		this.delimiter = ',';
	}
	
	public char getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}

	public void writeField(String field) throws IOException {
		boolean quotes = field.indexOf(delimiter) >= 0 ||
			field.indexOf('"') >= 0;
		
		if (!firstField) {
			writer.append(delimiter);
		}
		else {
			firstField = false;
		}
			
		if (quotes) {
			field = field.replace("\"", "\"\"");
			writer.append('"').append(field).append('"');
		}
		else {
			writer.append(field);
		}
	}
	
	public void writeLine() throws IOException {
		writer.append('\n');
		firstField = true;
	}
	
	public void close() throws IOException {
		writer.close();
	}
}
