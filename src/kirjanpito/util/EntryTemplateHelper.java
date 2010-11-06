package kirjanpito.util;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;

public class EntryTemplateHelper {
	private DateFormatSymbols symbols;
	private Date date;
	
	public EntryTemplateHelper(Date date) {
		this.symbols = new DateFormatSymbols();
		this.date = date;
	}
	
	public String substitutePlaceholders(String template) {
		StringBuilder result = new StringBuilder();
		Calendar cal = null;
		int len = template.length();
		boolean escaped = false;
		char ch;
		
		for (int i = 0; i < len; i++) {
			ch = template.charAt(i);
			
			if (escaped) {
				escaped = false;
				
				switch (ch) {
				case '-':
					cal.add(Calendar.MONTH, -1);
					escaped = true;
					break;
					
				case '+':
					cal.add(Calendar.MONTH, 1);
					escaped = true;
					break;
					
				case 'd':
					result.append(cal.get(Calendar.DAY_OF_MONTH));
					break;
					
				case 'F':
					upperCaseFirst(symbols.getMonths()[cal.get(
							Calendar.MONTH)], result);
					break;
					
				case 'f':
					result.append(symbols.getMonths()[cal.get(
							Calendar.MONTH)]);
					break;
					
				case 'm':
					result.append(cal.get(Calendar.MONTH) + 1);
					break;
				
				case 't':
					result.append(cal.getActualMaximum(Calendar.DAY_OF_MONTH));
					break;
					
				case 'Y':
					result.append(cal.get(Calendar.YEAR));
					break;
					
				case '%':
					result.append(ch);
					break;
					
				default:
					result.append('%').append(ch);
				}
			}
			else if (ch == '%') {
				escaped = true;
				cal = Calendar.getInstance();
				cal.setTime(date);
			}
			else {
				result.append(ch);
			}
		}
		
		return result.toString();
	}
	
	private void upperCaseFirst(String s, StringBuilder b) {
		if (s.length() >= 1) {
			b.append(Character.toUpperCase(s.charAt(0)));
		}
		
		if (s.length() >= 2) {
			b.append(s.substring(1));
		}
	}
}
