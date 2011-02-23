package kirjanpito.db.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLDocumentTypeDAO;

public class PSQLDocumentTypeDAO extends SQLDocumentTypeDAO {
	private PSQLSession sess;

	public PSQLDocumentTypeDAO(PSQLSession sess) {
		this.sess = sess;
	}
	
	protected int getGeneratedKey() throws SQLException {
		return sess.getSequenceValue("document_type_id_seq");
	}
	
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM document_type WHERE id = ?");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO document_type (id, number, name, number_start, number_end) VALUES (nextval('document_type_id_seq'), ?, ?, ?, ?)");
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, number_start, number_end FROM document_type ORDER BY number");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE document_type SET number=?, name=?, number_start=?, number_end=? WHERE id = ?");
	}
}
