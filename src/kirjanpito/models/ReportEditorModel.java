package kirjanpito.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.HashMap;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.ReportStructure;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.reports.Print;
import kirjanpito.util.Registry;

public class ReportEditorModel {
	private Registry registry;
	private HashMap<String, ReportStructure> reportStructures;
	private String[] defaultHeaders;
	private String[] headers;
	private String[] footers;
	
	public static final String[] REPORTS = new String[] {
		"income-statement",
		"income-statement-detailed",
		"balance-sheet",
		"balance-sheet-detailed"
	};
	
	public static final String[] REPORTS2 = new String[] {
		"accountSummary", "documentPrint", "accountStatement",
		"incomeStatement", "incomeStatementDetailed",
		"balanceSheet", "balanceSheetDetailed",
		"generalJournal", "generalLedger", "vatReport"
	};
	
	public ReportEditorModel(Registry registry) {
		this.registry = registry;
		this.reportStructures = new HashMap<String, ReportStructure>();
	}
	
	public void load() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			
			for (String id : REPORTS) {
				reportStructures.put(id, dataSource.getReportStructureDAO(
					sess).getById(id));
			}
		}
		finally {
			if (sess != null) sess.close();
		}
		
		Settings settings = registry.getSettings();
		defaultHeaders = new String[REPORTS2.length];
		headers = new String[REPORTS2.length];
		
		for (int i = 0; i < REPORTS2.length; i++) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						Print.class.getResourceAsStream(String.format("header-%s.txt", REPORTS2[i])),
						Charset.forName("UTF-8")));
				
				StringBuilder sb = new StringBuilder();
				String line;
				
				while ((line = reader.readLine()) != null) {
					sb.append(line).append('\n');
				}
				
				reader.close();
				defaultHeaders[i] = sb.toString().trim();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < REPORTS2.length; i++) {
			headers[i] = settings.getProperty(REPORTS2[i] + "/header", "");
			
			if (headers[i].isEmpty()) {
				headers[i] = defaultHeaders[i];
			}
		}
		
		footers = new String[REPORTS2.length];
		
		for (int i = 0; i < REPORTS2.length; i++) {
			footers[i] = settings.getProperty(REPORTS2[i] + "/footer", "");
		}
	}
	
	public void save() throws DataAccessException {
		Settings settings = registry.getSettings();
		
		for (int i = 0; i < REPORTS2.length; i++) {
			String s = headers[i].trim();
			
			if (s.equals(defaultHeaders[i])) {
				s = "";
			}
			
			settings.setProperty(REPORTS2[i] + "/header", s);
		}
		
		for (int i = 0; i < REPORTS2.length; i++) {
			settings.setProperty(REPORTS2[i] + "/footer", footers[i].trim());
		}
		
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			
			for (String id : REPORTS) {
				dataSource.getReportStructureDAO(
					sess).save(reportStructures.get(id));
			}
			
			dataSource.getSettingsDAO(sess).save(settings);
			sess.commit();
		}
		finally {
			if (sess != null) sess.close();
		}
	}
	
	public void parseContent(String id, String content) throws ParseException {
		checkErrors(content);
		reportStructures.get(id).setData(content);
	}
	
	public String getContent(String id) {
		return reportStructures.get(id).getData();
	}
	
	private static void checkErrors(String content) throws ParseException {
		BufferedReader reader = new BufferedReader(new StringReader(content));
		int number = 0;
		String line;
		int count, len;
		char ch;
		
		try {
			while ((line = reader.readLine()) != null) {
				number++;
				len = line.length();
				
				if (len == 0)
					continue;
				
				if (line.equals("-")) {
					continue;
				}
				
				if (line.length() < 4) {
					throw new ParseException("Ensimmäisessä kentässä on oltava vähintään kolme merkkiä", number);
				}
				
				int offset = 1;
				
				if (line.charAt(0) == 'D') {
					ch = line.charAt(1);
					
					if (ch == '+' || ch == '-' || ch == '0') {
						offset = 2;
					}
				}
				
				if (line.length() < offset + 3 || line.charAt(offset + 2) != ';') {
					throw new ParseException(String.format("Ensimmäisessä kentässä on oltava %d merkkiä", offset + 3), number);
				}
				
				ch = line.charAt(0);
				boolean accountNumbers = (ch != 'F');
				
				if (ch != 'F' && ch != 'G' && ch != 'H' && ch != 'S' && ch != 'T' && ch != 'D') {
					throw new ParseException("Merkki 1: D, F, G, H, S tai T.", number);
				}
				
				ch = line.charAt(offset);
				
				if (ch != 'P' && ch != 'B' && ch != 'I') {
					throw new ParseException(String.format("Merkki %d: P, B tai I.", offset + 1), number);
				}
				
				ch = line.charAt(offset + 1);
				
				if (!Character.isDigit(ch)) {
					throw new ParseException(String.format("Merkki %d: 0..9.", offset + 2), number);
				}
				
				if (accountNumbers) {
					count = 0;
					
					for (int i = offset + 3; i < len; i++) {
						if (line.charAt(i) == ';') count++;
					}
					
					if ((count % 2) != 0) {
						throw new ParseException("Tilinumeroita on oltava parillinen määrä.", number);
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getDefaultHeader(int index) {
		return defaultHeaders[index];
	}
	
	public String getHeader(int index) {
		return headers[index];
	}
	
	public void setHeader(int index, String header) {
		headers[index] = header;
	}
	
	public String getDefaultFooter(int index) {
		return "";
	}
	
	public String getFooter(int index) {
		return footers[index];
	}
	
	public void setFooter(int index, String footer) {
		footers[index] = footer;
	}
}
