package kirjanpito.db.sqlite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.db.AccountDAO;
import kirjanpito.db.COAHeadingDAO;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.DatabaseUpgradeUtil;
import kirjanpito.db.DocumentDAO;
import kirjanpito.db.DocumentTypeDAO;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.EntryTemplateDAO;
import kirjanpito.db.PeriodDAO;
import kirjanpito.db.ReportStructureDAO;
import kirjanpito.db.Session;
import kirjanpito.db.SettingsDAO;

/**
 * @author Tommi Helineva
 */
public class SQLiteDataSource implements DataSource {
	private String url;
	private File file;
	private Connection conn;

	private static final String JDBC_DRIVER_CLASS = "org.sqlite.JDBC";

	public void open(String url, String username, String password)
		throws DataAccessException
	{
		try {
			Class.forName(JDBC_DRIVER_CLASS);
		}
		catch (ClassNotFoundException e) {
			throw new DataAccessException(
					"SQLite-tietokanta-ajuria ei löytynyt", e);
		}

		if (!url.startsWith("jdbc:sqlite:")) {
			throw new DataAccessException("Virheellinen SQLite-JDBC-osoite: " + url);
		}

		String filename = url.substring(12);
		this.url = url;
		this.file = new File(filename);
		boolean tablesExist = file.exists();

		try {
			conn = DriverManager.getConnection(url);
			conn.setAutoCommit(false);

			if (tablesExist) {
				upgradeDatabase(conn, file);
			}
			else {
				createTables(conn);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	public void close() {
		try {
			conn.close();
		}
		catch (SQLException e) { }
	}

	public void backup() throws DataAccessException {
		close();
		backupDatabase(file);
		open(url, null, null);
	}

	public AccountDAO getAccountDAO(Session session) {
		return new SQLiteAccountDAO((SQLiteSession)session);
	}

	public COAHeadingDAO getCOAHeadingDAO(Session session) {
		return new SQLiteCOAHeadingDAO((SQLiteSession)session);
	}

	public DocumentDAO getDocumentDAO(Session session) {
		return new SQLiteDocumentDAO((SQLiteSession)session);
	}

	public EntryDAO getEntryDAO(Session session) {
		return new SQLiteEntryDAO((SQLiteSession)session);
	}

	public PeriodDAO getPeriodDAO(Session session) {
		return new SQLitePeriodDAO((SQLiteSession)session);
	}

	public SettingsDAO getSettingsDAO(Session session) {
		return new SQLiteSettingsDAO((SQLiteSession)session);
	}

	public ReportStructureDAO getReportStructureDAO(Session session) {
		return new SQLiteReportStructureDAO((SQLiteSession)session);
	}

	public EntryTemplateDAO getEntryTemplateDAO(Session session) {
		return new SQLiteEntryTemplateDAO((SQLiteSession)session);
	}

	public DocumentTypeDAO getDocumentTypeDAO(Session session) {
		return new SQLiteDocumentTypeDAO((SQLiteSession)session);
	}

	public Session openSession() throws DataAccessException {
		return new SQLiteSession(conn);
	}

	private static void createTables(Connection conn)
		throws DataAccessException {

		try {
			DatabaseUpgradeUtil.executeQueries(conn,
					SQLiteDataSource.class.getResourceAsStream("database.sql"));
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		catch (IOException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	private static void upgradeDatabase(Connection conn, File file)
		throws DataAccessException {

		int version = 0;

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT version FROM settings");

			if (rs.next()) {
				version = rs.getInt(1);
			}

			if (version == 1) {
				backupDatabase(file);
				upgrade1to2(conn, stmt);
				version = 2;
			}

			if (version == 2) {
				backupDatabase(file);
				upgrade2to3(conn, stmt);
				version = 3;
			}

			if (version == 3) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade3to4(conn, stmt);
				version = 4;
			}

			if (version == 4) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade4to5(conn, stmt);
				version = 5;
			}

			if (version == 5) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade5to6(conn, stmt);
				version = 6;
			}

			if (version == 6) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade6to7(conn, stmt);
				version = 7;
			}

			if (version == 7) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade7to8(conn, stmt);
				version = 8;
			}

			if (version == 8) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade8to9(conn, stmt);
				version = 9;
			}

			if (version == 9) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade9to10(conn, stmt);
				version = 10;
			}

			if (version == 10) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade10to11(conn, stmt);
				version = 11;
			}

			if (version == 11) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade11to12(conn, stmt);
				version = 12;
			}

			if (version == 12) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade12to13(conn, stmt);
				version = 13;
			}

			if (version == 13) {
				backupDatabase(file);
				DatabaseUpgradeUtil.upgrade13to14(conn, stmt, true);
				version = 14;
			}

			stmt.close();
		}
		catch (Exception e) {
			try {
				conn.rollback();
			}
			catch (SQLException exc) {
			}

			Logger logger = Logger.getLogger("kirjanpito.db.sqlite");
			logger.log(Level.SEVERE, "Tietokannan päivittäminen epäonnistui", e);
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	private static void upgrade1to2(Connection conn, Statement stmt) throws SQLException {
		stmt.execute("CREATE TABLE entry_template (id integer PRIMARY KEY AUTOINCREMENT NOT NULL, number integer NOT NULL, name varchar(100) NOT NULL, account_id integer NOT NULL, debit bool NOT NULL, amount numeric(10, 2) NOT NULL, description varchar(100) NOT NULL, row_number integer NOT NULL, FOREIGN KEY (account_id) REFERENCES account (id))");
		stmt.executeUpdate("UPDATE settings SET version=2");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db.sqlite");
		logger.info("Tietokannan päivittäminen versioon 2 onnistui");
	}

	private static void upgrade2to3(Connection conn, Statement stmt) throws SQLException {
		stmt.execute("CREATE TABLE document_type (id integer PRIMARY KEY AUTOINCREMENT NOT NULL, number integer NOT NULL, name varchar(100) NOT NULL, number_start integer NOT NULL, number_end integer NOT NULL)");
		stmt.execute("ALTER TABLE settings ADD document_type_id integer REFERENCES document_type (id)");
		stmt.executeUpdate("UPDATE settings SET version=3");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db.sqlite");
		logger.info("Tietokannan päivittäminen versioon 3 onnistui");
	}

	private static void backupDatabase(File file) {
		File dir = file.getParentFile();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
		File destination = new File(dir, "kirjanpito-" +
				dateFormat.format(new Date()) + ".sqlite");

		if (destination.exists()) {
			return;
		}

		Logger logger = Logger.getLogger("kirjanpito.db.sqlite");
		logger.severe("Varmuuskopioidaan tietokanta, " + file + " -> " + destination);

		try {
			copyFile(file, destination);
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Tietokannan varmuuskopiointi epäonnistui", e);
		}
	}

	private static void copyFile(File src, File dst) throws IOException {
	    FileInputStream in = new FileInputStream(src);
	    FileOutputStream out = new FileOutputStream(dst);
	    byte[] buf = new byte[1024];
	    int len;

	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }

	    in.close();
	    out.close();
	}
}
