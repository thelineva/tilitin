package kirjanpito.db.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.Session;

/**
 * PostgreSQL-tietokantaistunto
 * 
 * @author Tommi Helineva
 */
public class PSQLSession implements Session {
	private static Logger logger = Logger.getLogger("kirjanpito.db.postgresql");
	
	protected Connection conn;

	public PSQLSession(Connection conn) {
		this.conn = conn;
	}
	
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
	 * Hakee tietokannasta sekvenssin <code>name</code> nykyisen arvon.
	 * 
	 * @param name sekvenssin nimi
	 * @return sekvenssin arvo
	 * @throws SQLException jos hakeminen epäonnistuu
	 */
	public int getSequenceValue(String name) throws SQLException {
		PreparedStatement stmt = prepareStatement(
				"SELECT currval('" + name + "')");
		
		ResultSet rs = stmt.executeQuery();
		int seqValue = -1;
		
		if (rs.next()) {
			seqValue = rs.getInt(1);
		}
		
		rs.close();
		stmt.close();
		return seqValue;
	}
}
