package kirjanpito.db.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLEntryTemplateDAO;

public class PSQLEntryTemplateDAO extends SQLEntryTemplateDAO {
	private PSQLSession sess;
	
	public PSQLEntryTemplateDAO(PSQLSession sess) {
		this.sess = sess;
	}
	
	protected int getGeneratedKey() throws SQLException {
		return sess.getSequenceValue("entry_template_id_seq");
	}
	
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM entry_template WHERE id = ?");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO entry_template (id, number, name, account_id, debit, amount, description, row_number) VALUES (nextval('entry_template_id_seq'), ?, ?, ?, ?, ?, ?, ?)");
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, account_id, debit, amount, description, row_number FROM entry_template ORDER BY number, row_number");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE entry_template SET number=?, name=?, account_id=?, debit=?, amount=?, description=?, row_number=? WHERE id = ?");
	}
}
