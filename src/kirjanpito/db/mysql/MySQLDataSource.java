package kirjanpito.db.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
public class MySQLDataSource implements DataSource {
	private Connection conn;

	private static final String JDBC_DRIVER_CLASS = "com.mysql.jdbc.Driver";

	public void open(String url, String username, String password)
		throws DataAccessException
	{
		try {
			Class.forName(JDBC_DRIVER_CLASS);
		}
		catch (ClassNotFoundException e) {
			throw new DataAccessException(
					"MySQL-tietokanta-ajuria ei löytynyt", e);
		}

		try {
			conn = DriverManager.getConnection(url, username, password);
			conn.setAutoCommit(false);
			upgradeDatabase(conn);
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

	public void backup() {
	}

	public AccountDAO getAccountDAO(Session session) {
		return new MySQLAccountDAO((MySQLSession)session);
	}

	public COAHeadingDAO getCOAHeadingDAO(Session session) {
		return new MySQLCOAHeadingDAO((MySQLSession)session);
	}

	public DocumentDAO getDocumentDAO(Session session) {
		return new MySQLDocumentDAO((MySQLSession)session);
	}

	public EntryDAO getEntryDAO(Session session) {
		return new MySQLEntryDAO((MySQLSession)session);
	}

	public PeriodDAO getPeriodDAO(Session session) {
		return new MySQLPeriodDAO((MySQLSession)session);
	}

	public SettingsDAO getSettingsDAO(Session session) {
		return new MySQLSettingsDAO((MySQLSession)session);
	}

	public ReportStructureDAO getReportStructureDAO(Session session) {
		return new MySQLReportStructureDAO((MySQLSession)session);
	}

	public EntryTemplateDAO getEntryTemplateDAO(Session session) {
		return new MySQLEntryTemplateDAO((MySQLSession)session);
	}

	public DocumentTypeDAO getDocumentTypeDAO(Session session) {
		return new MySQLDocumentTypeDAO((MySQLSession)session);
	}

	public Session openSession() throws DataAccessException {
		return new MySQLSession(conn);
	}

	private static void createTables(Connection conn)
		throws DataAccessException {

		try {
			DatabaseUpgradeUtil.executeQueries(conn,
					MySQLDataSource.class.getResourceAsStream("database.sql"));
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		catch (IOException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	private static void upgradeDatabase(Connection conn)
		throws DataAccessException {

		int version = 0;

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT version FROM settings");

			if (rs.next()) {
				version = rs.getInt(1);
			}

			if (version == 1) {
				upgrade1to2(conn, stmt);
				version = 2;
			}

			if (version == 2) {
				upgrade2to3(conn, stmt);
				version = 3;
			}

			if (version == 3) {
				DatabaseUpgradeUtil.upgrade3to4(conn, stmt);
				version = 4;
			}

			if (version == 4) {
				DatabaseUpgradeUtil.upgrade4to5(conn, stmt);
				version = 5;
			}

			if (version == 5) {
				DatabaseUpgradeUtil.upgrade5to6(conn, stmt);
				version = 6;
			}

			if (version == 6) {
				DatabaseUpgradeUtil.upgrade6to7(conn, stmt);
				version = 7;
			}

			if (version == 7) {
				DatabaseUpgradeUtil.upgrade7to8(conn, stmt);
				version = 8;
			}

			if (version == 8) {
				DatabaseUpgradeUtil.upgrade8to9(conn, stmt);
				version = 9;
			}

			if (version == 9) {
				DatabaseUpgradeUtil.upgrade9to10(conn, stmt);
				version = 10;
			}

			if (version == 10) {
				DatabaseUpgradeUtil.upgrade10to11(conn, stmt);
				version = 11;
			}

			if (version == 11) {
				DatabaseUpgradeUtil.upgrade11to12(conn, stmt);
				version = 12;
			}

			if (version == 12) {
				DatabaseUpgradeUtil.upgrade12to13(conn, stmt);
				version = 13;
			}

			if (version == 13) {
				DatabaseUpgradeUtil.upgrade13to14(conn, stmt, false);
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

			if (e.getMessage() != null || e.getMessage().contains("exist")) {
				createTables(conn);
				return;
			}

			Logger logger = Logger.getLogger("kirjanpito.db.sqlite");
			logger.log(Level.SEVERE, "Tietokannan päivittäminen epäonnistui", e);
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	private static void upgrade1to2(Connection conn, Statement stmt) throws SQLException {
		stmt.execute("CREATE TABLE entry_template (id int auto_increment NOT NULL, number int NOT NULL, name varchar(100) NOT NULL, account_id int NOT NULL, debit bool NOT NULL, amount numeric(10, 2) NOT NULL, description varchar(100) NOT NULL, row_number int NOT NULL, PRIMARY KEY (id), FOREIGN KEY (account_id) REFERENCES account (id)) ENGINE=InnoDB");
		stmt.executeUpdate("UPDATE settings SET version=2");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db.mysql");
		logger.info("Tietokannan päivittäminen versioon 2 onnistui");
	}

	private static void upgrade2to3(Connection conn, Statement stmt) throws SQLException {
		stmt.execute("CREATE TABLE document_type (id int auto_increment NOT NULL, number int NOT NULL, name varchar(100) NOT NULL, number_start int NOT NULL, number_end int NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB");
		stmt.execute("ALTER TABLE settings ADD document_type_id integer, ADD FOREIGN KEY (document_type_id) REFERENCES document_type (id)");
		stmt.executeUpdate("UPDATE settings SET version=3");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db.mysql");
		logger.info("Tietokannan päivittäminen versioon 3 onnistui");
	}
}
