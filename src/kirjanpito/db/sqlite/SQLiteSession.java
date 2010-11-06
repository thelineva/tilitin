package kirjanpito.db.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.Session;

/**
 * SQLite-tietokantaistunto.
 * 
 * @author Tommi Helineva
 */
public class SQLiteSession implements Session {
	private static Logger logger = Logger.getLogger("kirjanpito.db.sqlite");
	
	private Connection conn;

	public SQLiteSession(Connection conn) {
		this.conn = conn;
	}
	
	/**
	 * Palauttaa tietokantayhteyden.
	 * 
	 * @return tietokantayhteys
	 */
	public Connection getConnection() {
		return conn;
	}
	
	public void commit() throws DataAccessException {
		try {
			conn.commit();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	public void rollback() throws DataAccessException {
		try {
			conn.rollback();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	public void close() {
	}
	
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		logger.log(Level.FINER, "Suoritetaan tietokantakysely: " + sql);
		return conn.prepareStatement(sql);
	}
	
	/**
	 * Palauttaa viimeksi lisätyn rivin AUTO_INCREMENT-kentän arvon.
	 * 
	 * @return laskurikentän arvo
	 * @throws SQLException jos hakeminen epäonnistuu
	 */
	public int getInsertId() throws SQLException {
		PreparedStatement stmt = prepareStatement(
				"SELECT last_insert_rowid()");
		ResultSet rs = stmt.executeQuery();
		int insertId = -1;
		
		if (rs.next()) {
			insertId = rs.getInt(1);
		}
		
		rs.close();
		stmt.close();
		return insertId;
	}
}
