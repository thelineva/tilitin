package kirjanpito.db;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Sisältää asetuksia.
 * 
 * @author Tommi Helineva
 */
public class Settings {
	private String name;
	private String businessId;
	private int currentPeriodId;
	private int documentTypeId;
	private HashMap<String, String> properties;

	public Settings() {
		properties = new HashMap<String, String>();
	}
	
	/**
	 * Palauttaa nykyisen tilikauden tunnisteen.
	 * 
	 * @return nykyisen tilikauden tunniste
	 */
	public int getCurrentPeriodId() {
		return currentPeriodId;
	}

	/**
	 * Asettaa nykyisen tilikauden tunnisteen.
	 * 
	 * @param currentPeriodId nykyisen tilikauden tunniste
	 */
	public void setCurrentPeriodId(int currentPeriodId) {
		this.currentPeriodId = currentPeriodId;
	}

	/**
	 * Palauttaa valitun tositelajin tunnisteen.
	 * 
	 * @return tositelajin tunniste
	 */
	public int getDocumentTypeId() {
		return documentTypeId;
	}
	
	/**
	 * Asettaa valitun tositelajin tunnisteen.
	 * 
	 * @param documentTypeId tositelajin tunniste
	 */
	public void setDocumentTypeId(int documentTypeId) {
		this.documentTypeId = documentTypeId;
	}
	
	/**
	 * Palauttaa kirjanpitotietojen omistajan nimen.
	 * 
	 * @return nimi
	 */
	public String getName() {
		return name;
	}

	/**
	 * Asettaa kirjanpitotietojen omistajan nimen.
	 * 
	 * @param name nimi
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Palauttaa Y-tunnuksen.
	 * 
	 * @return y-tunnus
	 */
	public String getBusinessId() {
		return businessId;
	}

	/**
	 * Asettaa Y-tunnuksen.
	 * 
	 * @param businessId y-tunnus
	 */
	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}
	
	public void parseProperties(String s) {
		String[] lines = s.split("\n");
		properties.clear();
		
		for (String line : lines) {
			int pos = line.indexOf('=');
			if (pos < 0) continue;
			String key = line.substring(0, pos);
			String value = unescape(line.substring(pos + 1));
			properties.put(key, value);
		}
	}
	
	public String propertiesToString() {
		StringBuilder sb = new StringBuilder();
		String[] keys = new String[properties.size()];
		properties.keySet().toArray(keys);
		Arrays.sort(keys);
		
		for (String key : keys) {
			sb.append(key);
			sb.append('=');
			sb.append(escape(properties.get(key)));
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
	public void setProperty(String key, String value) {
		if (value == null || value.isEmpty()) {
			properties.remove(key);
		}
		else {
			properties.put(key, value);
		}
	}
	
	public String getProperty(String key, String def) {
		String value = properties.get(key);
		
		if (value == null) {
			value = def;
		}
		
		return value;
	}
	
	private String escape(String s) {
		return s.replace("\r", "").replace(
				"\\", "\\\\").replace("\n", "\\n");
	}
	
	private String unescape(String s) {
		StringBuilder sb = new StringBuilder(s);
		int pos = sb.indexOf("\\n");
		
		while (pos >= 0) {
			if (pos == 0 || sb.charAt(pos - 1) != '\\') {
				sb.replace(pos, pos + 2, "\n");
			}
			
			pos = sb.indexOf("\\n", pos + 1);
		}
		
		return sb.toString().replace("\\\\", "\\");
	}
}
