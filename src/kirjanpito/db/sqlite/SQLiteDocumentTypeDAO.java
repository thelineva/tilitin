package kirjanpito.db.sqlite;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLDocumentTypeDAO;

public class SQLiteDocumentTypeDAO extends SQLDocumentTypeDAO {
	private SQLiteSession sess;

	public SQLiteDocumentTypeDAO(SQLiteSession sess) {
		this.sess = sess;
	}
	
	protected int getGeneratedKey() throws SQLException {
		return sess.getInsertId();
	}
	
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM document_type WHERE id = ?");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO document_type (number, name, number_start, number_end) VALUES (?, ?, ?, ?)");
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, number_start, number_end FROM document_type ORDER BY number");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE document_type SET number=?, name=?, number_start=?, number_end=? WHERE id = ?");
	}
}
