package kirjanpito.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.ui.Kirjanpito;

/**
 * Tallentaa ohjelman asetukset .properties-tiedostoon.
 *
 * @author Tommi Helineva
 */
public class AppSettings {
	private File file;
	private Properties config;
	private static AppSettings instance;
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);

	private AppSettings() {
		config = new Properties();
	}

	/**
	 * Palauttaa asetustiedoston hakemistopolun.
	 *
	 * @return hakemistopolku
	 */
	public String getDirectoryPath() {
		String path = file.getAbsolutePath();
		return path.substring(0, path.lastIndexOf(File.separatorChar));
	}

	/**
	 * Palauttaa asetustiedoston nimen.
	 *
	 * @return asetustiedoston nimi
	 */
	public String getFilename() {
		return file.getAbsolutePath();
	}

	/**
	 * Lukee asetukset tiedostosta <code>file</code>.
	 *
	 * @param file tiedosto, josta asetukset luetaan
	 */
	public void load(File file) {
		this.file = file;

		if (file.exists()) {
			logger.fine("Luetaan asetukset tiedostosta " + file.getAbsolutePath());

			try {
				FileInputStream input = new FileInputStream(file);
				config.load(input);
				input.close();
			}
			catch (IOException e) {
				logger.log(Level.WARNING, "Asetuksien lukeminen epäonnistui", e);
			}
		}
		else {
			logger.fine(String.format(
					"Asetustiedostoa %s ei löytynyt", file.getAbsolutePath()));
		}
	}

	/**
	 * Tallentaa asetukset siihen tiedostoon, josta ne on luettu.
	 */
	public void save() {
		save(null);
	}

	/**
	 * Tallentaa asetukset tiedostoon <code>file</code>.
	 *
	 * @param file tiedosto, johon asetukset tallennetaan
	 */
	public void save(File file) {
		if (file != null) this.file = file;
		String dirPath = getDirectoryPath();
		File dir = new File(dirPath);

		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				logger.log(Level.SEVERE, String.format(
					"Asetushakemiston %s luominen epäonnistui", dirPath));
				return;
			}
		}

		logger.fine(String.format(
				"Tallennetaan asetukset tiedostoon %s.", this.file));

		try {
			PrintWriter out = new PrintWriter(this.file);
			writeProperties(config, out);
			out.close();
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Asetuksien tallentaminen epäonnistui.", e);
		}
	}

	private void writeProperties(Properties prop, PrintWriter out) {
		String[] keys = new String[prop.keySet().size()];
		int index = 0;

		for (Object obj : prop.keySet()) {
			keys[index++] = obj.toString();
		}

		Arrays.sort(keys);

		for (String key : keys) {
			String value = escapeValue(prop.getProperty(key));
			out.println(String.format("%s = %s", key, value));
		}
	}

	private String escapeValue(String value) {
		value = value.replace("\\", "\\\\");
		return value;
	}

	/**
	 * Palauttaa kokonaislukuasetuksen, joka on tallennettu
	 * nimellä <code>key</code>.
	 *
	 * @param key asetuksen nimi
	 * @param def oletusarvo
	 * @return asetuksen arvo tai <code>def</code>, jos asetusta ei löydy
	 */
	public int getInt(String key, int def) {
		String value = config.getProperty(key);
		int intValue = def;

		if (value != null) {
			try {
				intValue = Integer.parseInt(value);
			}
			catch (NumberFormatException e) { }
		}

		return intValue;
	}

	/**
	 * Palauttaa liukulukuasetuksen, joka on tallennettu
	 * nimellä <code>key</code>.
	 *
	 * @param key asetuksen nimi
	 * @param def oletusarvo
	 * @return asetuksen arvo tai <code>def</code>, jos asetusta ei löydy
	 */
	public double getDouble(String key, double def) {
		String value = config.getProperty(key);
		double doubleValue = def;

		if (value != null) {
			try {
				doubleValue = Double.parseDouble(value);
			}
			catch (NumberFormatException e) { }
		}

		return doubleValue;
	}

	/**
	 * Palauttaa totuusarvoasetuksen, joka on tallennettu
	 * nimellä <code>key</code>.
	 *
	 * @param key asetuksen nimi
	 * @param def oletusarvo
	 * @return asetuksen arvo tai <code>def</code>, jos asetusta ei löydy
	 */
	public boolean getBoolean(String key, boolean def) {
		String value = config.getProperty(key);

		if (value == null) {
			return def;
		}
		else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
			return true;
		}
		else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
			return false;
		}
		else {
			return def;
		}
	}

	/**
	 * Palauttaa merkkijonoasetuksen, joka on tallennettu
	 * nimellä <code>key</code>.
	 *
	 * @param key asetuksen nimi
	 * @param def oletusarvo
	 * @return asetuksen arvo tai <code>def</code>, jos asetusta ei löydy
	 */
	public String getString(String key, String def) {
		String value = config.getProperty(key, def);

		if (value == null)
			value = def;

		return value;
	}

	/**
	 * Palauttaa kokonaislukuasetuksen, joka on tallennettu
	 * nimellä <code>key</code>.
	 *
	 * @param key asetuksen nimi
	 * @return asetuksen arvo tai <code>null</code>, jos asetusta ei löydy
	 */
	public String getString(String key) {
		return config.getProperty(key);
	}

	/**
	 * Palauttaa asetusten nimet, jotka alkavat etuliitteellä
	 * <code>prefix</code>.
	 *
	 * @param prefix etuliite
	 * @return asetusten nimet
	 */
	public String[] getKeys(String prefix) {
		List<String> keys = new ArrayList<String>();

		for (Object obj : config.keySet()) {
			String key = obj.toString();

			if (prefix == null || key.startsWith(prefix)) {
				keys.add(key);
			}
		}

		return keys.toArray(new String[0]);
	}

	/**
	 * Palauttaa kaikkien asetusten nimet.
	 *
	 * @return asetusten nimet
	 */
	public String[] getKeys() {
		return getKeys(null);
	}

	/**
	 * Poistaa asetuksen.
	 *
	 * @param key poistettavan asetuksen nimi
	 */
	public void remove(String key) {
		if (config.containsKey(key))
			config.remove(key);
	}

	/**
	 * Asettaa asetuksen <code>key</code> arvoksi merkkijonon
	 * <code>value</code>.
	 *
	 * @param key asetuksen nimi
	 * @param value asetuksen arvo
	 */
	public void set(String key, String value) {
		config.setProperty(key, value);
	}

	/**
	 * Asettaa asetuksen <code>key</code> arvoksi kokonaisluvun
	 * <code>value</code>.
	 *
	 * @param key asetuksen nimi
	 * @param value asetuksen arvo
	 */
	public void set(String key, int value) {
		config.setProperty(key, Integer.toString(value));
	}

	/**
	 * Asettaa asetuksen <code>key</code> arvoksi liukuluvun
	 * <code>value</code>.
	 *
	 * @param key asetuksen nimi
	 * @param value asetuksen arvo
	 */
	public void set(String key, double value) {
		config.setProperty(key, Double.toString(value));
	}

	/**
	 * Asettaa asetuksen <code>key</code> arvoksi totuusarvon
	 * <code>value</code>.
	 *
	 * @param key asetuksen nimi
	 * @param value asetuksen arvo
	 */
	public void set(String key, boolean value) {
		config.setProperty(key, value ? "true" : "false");
	}

	/**
	 * Palauttaa <code>AppSettings</code>-olion.
	 *
	 * @return asetukset
	 */
	public static AppSettings getInstance() {
		if (instance == null) {
			instance = new AppSettings();
		}

		return instance;
	}

	/**
	 * Muodostaa hakemistopolun, johon ohjelman asetukset
	 * tallennetaan. Windows XP:ssä C:\Documents and
	 * Settings\Käyttäjänimi\Application Data\Ohjelma,
	 * Windows Vistassa C:\Users\Käyttäjänimi\AppData\Roaming\Ohjelma,
	 * Linuxissa ~/.config/ohjelma ja Mac OS X:ssä
	 * ~/Library/Application Support/Ohjelma
	 *
	 * @param appName ohjelman nimi
	 * @return hakemistopolku
	 */
	public static String buildDirectoryPath(String appName) {
		String os = System.getProperty("os.name");
		String dir;

		/* Windowsissa */
		if (os.startsWith("Windows")) {
			String appData = System.getenv("APPDATA");
			File appDataDir = null;

			if (appData != null)
				appDataDir = new File(appData);

			if (appData != null && appDataDir.isDirectory()) {
				dir = appData + File.separator + appName;
			}
			else {
				String home = System.getProperty("user.home");
				dir = home + File.separator + appName;
			}
		}
		/* Mac OS X:ssä */
		else if (os.equals("Mac OS X")) {
			String home = System.getProperty("user.home");
			String lib = home + "/Library/Application Support";
			dir = lib + File.separator + appName;
		}
		/* Linuxissa (ja muissa käyttöjärjestelmissä) */
		else {
			String home = System.getProperty("user.home");
			String config = home + File.separator + ".config";
			dir = config + File.separator + appName.toLowerCase();
		}

		return dir;
	}
}