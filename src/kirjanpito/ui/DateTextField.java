package kirjanpito.ui;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextField;

/**
 * <code>JTextField</code>in aliluokka, johon voidaan syöttää vain
 * päivämääriä. Päivämäärä voidaan syöttää seuraavissa muodoissa:
 * d.M.yyyy; d.M; ddMMyyyy; ddM; d.
 * 
 * @author Tommi Helineva
 */
public class DateTextField extends JTextField {
	private DateFormat dateFormat;
	private static Pattern[] patterns;
	
	private static final long serialVersionUID = 1L;
	
	static {
		patterns = new Pattern[] {
			/* d.M.yyyy */
			Pattern.compile(
				"^(\\d{1,2})[\\.,](\\d{1,2})[\\.,](\\d{4})$"),
			
			/* d.M[.] */
			Pattern.compile(
				"^(\\d{1,2})[\\.,](\\d{1,2})[\\.,]?$"),
			
			/* ddMMyyyy */
			Pattern.compile("^(\\d{2})(\\d{2})(\\d{4})$"),
			
			/* ddM */
			Pattern.compile("^(\\d{2})(\\d{1,2})$"),
			
			/* d[.] */
			Pattern.compile("^(\\d{1,2})[\\.,]?$")
		};
	}
	
	public DateTextField() {
		dateFormat = new SimpleDateFormat("d.M.yyyy");
		addFocusListener(focusListener);
	}
	
	public void setDate(Date date) {
		String str = (date == null) ? "" : dateFormat.format(date);
		setText(str);
	}
	
	public Date getDate() throws ParseException {
		String str = getText();
		
		if (str.length() == 0) {
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setLenient(false);
		cal.clear();
		
		Calendar now = Calendar.getInstance();
		Matcher matcher = patterns[0].matcher(str);
		
		if (matcher.matches()) {
			cal.set(
					Integer.parseInt(matcher.group(3)), // vuosi
					Integer.parseInt(matcher.group(2)) - 1, // kuukausi
					Integer.parseInt(matcher.group(1)) // päivä
			);
		}
		else {
			matcher = patterns[1].matcher(str);
			
			if (matcher.matches()) {
				cal.set(
						now.get(Calendar.YEAR), // vuosi
						Integer.parseInt(matcher.group(2)) - 1, // kuukausi
						Integer.parseInt(matcher.group(1)) // päivä
				);
			}
		}
		
		if (!matcher.matches()) {
			matcher = patterns[2].matcher(str);
			
			if (matcher.matches()) {
				cal.set(
						Integer.parseInt(matcher.group(3)), // vuosi
						Integer.parseInt(matcher.group(2)) - 1, // kuukausi
						Integer.parseInt(matcher.group(1)) // päivä
				);
			}
		}
		
		if (!matcher.matches()) {
			matcher = patterns[3].matcher(str);
			
			if (matcher.matches()) {
				cal.set(
						now.get(Calendar.YEAR), // vuosi
						Integer.parseInt(matcher.group(2)) - 1, // kuukausi
						Integer.parseInt(matcher.group(1)) // päivä
				);
			}
		}
		
		if (!matcher.matches()) {
			matcher = patterns[4].matcher(str);
			
			if (matcher.matches()) {
				cal.set(
						now.get(Calendar.YEAR), // vuosi
						now.get(Calendar.MONTH), // kuukausi
						Integer.parseInt(matcher.group(1)) // päivä
				);
			}
		}

		if (!matcher.matches()) {
			throw new ParseException("Virheellinen päivämäärän muoto", 0);
		}
		
		Date date;
		
		try {
			date = cal.getTime();
		}
		catch (IllegalArgumentException e) {
			throw new ParseException("Virheellinen päivämäärä", 0);
		}
		
		return date;
	}
	
	private void autoComplete() {
		Date date;
		
		try {
			date = getDate();
		}
		catch (ParseException e) {
			return;
		}
		
		setDate(date);
	}
	
	private FocusListener focusListener = new FocusAdapter() {
		public void focusLost(FocusEvent arg0) {
			autoComplete();
		}
	};
}
