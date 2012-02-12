package kirjanpito.util;

public class XMLWriter {
	private StringBuilder buffer;
	private boolean elementOpen;
	
	public XMLWriter() {
		this.buffer = new StringBuilder();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	}
	
	public void startElement(String name) {
		closeElement();
		openElement();
		write(name);
	}
	
	public void endElement(String name) {
		closeElement();
		openElement();
		buffer.append('/');
		write(name);
		closeElement();
	}
	
	public void endElement() {
		if (!elementOpen) {
			throw new RuntimeException();
		}
		
		buffer.append('/');
		closeElement();
	}
	
	public void writeAttribute(String name, String value) {
		if (!elementOpen) {
			throw new RuntimeException();
		}
		
		buffer.append(' ');
		write(name);
		buffer.append('=');
		buffer.append('"');
		write(value);
		buffer.append('"');
	}

	public void writeText(String text) {
		closeElement();
		write(text);
	}
	
	public void writeTextElement(String name, String text) {
		startElement(name);
		writeText(text);
		endElement(name);
	}
	
	private void openElement() {
		if (!elementOpen) {
			buffer.append('<');
			elementOpen = true;
		}
	}
	
	private void closeElement() {
		if (elementOpen) {
			buffer.append('>');
			elementOpen = false;
		}
	}
	
	private void write(String s) {
		int len = s.length();
		
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			
			if (c == '&') {
				buffer.append("&amp;");
			}
			else if (c == '"') {
				buffer.append("&quot;");
			}
			else if (c == '<') {
				buffer.append("&lt;");
			}
			else if (c == '>') {
				buffer.append("&gt;");
			}
			else {
				buffer.append(c);
			}
		}
	}
	
	public String toString() {
		return buffer.toString();
	}
}
