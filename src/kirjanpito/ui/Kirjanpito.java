package kirjanpito.ui;

import java.awt.Toolkit;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import kirjanpito.models.DocumentModel;
import kirjanpito.util.AppSettings;
import kirjanpito.util.Registry;

/**
 * Kirjanpito-ohjelman käynnistävä luokka.
 *
 * @author Tommi Helineva
 */
public class Kirjanpito implements Runnable {
	public static File logFile;
	private boolean debug;
	private File configFile;
	private String jdbcUrl;
	private String username;
	private String password;

	public static final String APP_NAME = "Tilitin";
	public static final String APP_VERSION = "1.5.0";
	public static final String LOGGER_NAME = "kirjanpito";

	private Kirjanpito() {
	}

	/**
	 * Avaa tositteiden muokkausikkunan.
	 */
	public void run() {
		AppSettings settings = AppSettings.getInstance();

		if (configFile == null) {
			File file = new File(AppSettings.buildDirectoryPath(APP_NAME),
				"asetukset.properties");

			settings.load(file);
		}
		else {
			settings.load(configFile);
		}

		configureLogging(settings.getDirectoryPath());
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.startsWith("mac os x")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (osName.startsWith("linux")) {
			try {
				/* Vaihdetaan WM_CLASS. */
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				java.lang.reflect.Field awtAppClassNameField =
					toolkit.getClass().getDeclaredField("awtAppClassName");
				awtAppClassNameField.setAccessible(true);
				awtAppClassNameField.set(toolkit, "Tilitin");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (jdbcUrl != null) {
			if (!jdbcUrl.startsWith("jdbc:")) {
				jdbcUrl = String.format("jdbc:sqlite:%s", jdbcUrl.replace(File.pathSeparatorChar, '/'));
			}

			settings.set("database.url", jdbcUrl);
		}

		if (username != null) {
			settings.set("database.username", username);
		}

		if (password != null) {
			settings.set("database.password", password);
		}

		Registry registry = new Registry();
		DocumentFrame frame = new DocumentFrame(registry,
				new DocumentModel(registry));
		frame.create();
		frame.setVisible(true);
		frame.openDataSource();
	}

	private void configureLogging(String dirname) {
		Level level = debug ? Level.FINEST : Level.WARNING;
		File dir = new File(dirname);
		boolean foundConsoleHandler = false;
		boolean foundFileHandler = false;

		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				System.err.println(String.format("Hakemiston %s luominen epäonnistui.", dirname));
			}
		}

		try {
			Handler[] handlers = Logger.getLogger("").getHandlers();

			/* Tarkistetaan, onko ConsoleHandler tai
			 * FileHandler jo lisätty. */
			for (int index = 0; index < handlers.length; index++) {
				if (handlers[index] instanceof ConsoleHandler) {
					foundConsoleHandler = true;
					handlers[index].setLevel(level);
				}
				else if (handlers[index] instanceof FileHandler) {
					foundFileHandler = true;
					handlers[index].setLevel(level);
				}
			}

			if (!foundConsoleHandler && debug) {
				/* Jos debug-asetus on päällä, kirjoitetaan
				 * loki myös päätteeseen. */
				ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel(level);
				consoleHandler.setFormatter(new SimpleFormatter());
				Logger.getLogger(LOGGER_NAME).addHandler(consoleHandler);
			}

			if (!foundFileHandler) {
				/* Kirjoitetaan loki tiedostoon. */
				logFile = new File(dir, LOGGER_NAME + ".log.txt");
				FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), 20 * 1024, 1, true);
				fileHandler.setLevel(level);
				fileHandler.setFormatter(new SimpleFormatter());
				Logger.getLogger(LOGGER_NAME).addHandler(fileHandler);
			}

			Logger.getLogger(LOGGER_NAME).setLevel(level);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Kirjanpito p = new Kirjanpito();
		boolean invalid = false;

		/* Tarkistetaan komentoriviparametrit. */
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-d") || args[i].equals("--debug")) {
				p.debug = true;
			}
			else if (args[i].equals("-c") || args[i].equals("--config")) {
				try {
					p.configFile = new File(args[i+1]);
					i++;
				}
				catch (Exception e) {
					invalid = true;
				}
			}
			else if (args[i].equals("-u") || args[i].equals("--username")) {
				try {
					p.username = args[i+1];
					i++;
				}
				catch (Exception e) {
					invalid = true;
				}
			}
			else if (args[i].equals("-p") || args[i].equals("--password")) {
				try {
					p.password = args[i+1];
					i++;
				}
				catch (Exception e) {
					invalid = true;
				}
			}
			else if (i == args.length - 1 && !args[i].startsWith("-")) {
				p.jdbcUrl = args[i];
			}
			else {
				invalid = true;
			}
		}

		if (invalid) {
			printUsage();
			System.exit(1);
		}

		SwingUtilities.invokeLater(p);
	}

	private static void printUsage() {
		System.err.println("Käyttö: tilitin [-c|--config CONFIG] [-d|--debug] ");
		System.err.println("        [-u|--username USERNAME] [-p|--password PASSWORD] [FILE|JDBC_URL]");
	}

	private static class ExceptionHandler implements UncaughtExceptionHandler {
		public void uncaughtException(Thread t, Throwable e) {
			Logger.getLogger("kirjanpito").log(Level.SEVERE, "Uncaught exception", e);
		}
	}
}
