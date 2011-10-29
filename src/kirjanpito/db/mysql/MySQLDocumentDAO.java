package kirjanpito.db.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLDocumentDAO;

/**
 * <code>MySQLDocumentDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tositteita sekä hakea olemassa olevien tositteiden
 * tietoja.
 * 
 * @author Tommi Helineva
 */
public class MySQLDocumentDAO extends SQLDocumentDAO {
	private MySQLSession sess;
	
	public MySQLDocumentDAO(MySQLSession sess) {
		this.sess = sess;
	}
	
	protected int getGeneratedKey() throws SQLException {
		return sess.getInsertId();
	}
	
	protected PreparedStatement getSelectLastDocumentQuery() throws SQLException {
		return sess.prepareStatement("SELECT number, date FROM document WHERE period_id = ? AND number BETWEEN ? AND ? ORDER BY number DESC LIMIT 1");
	}
	
	protected PreparedStatement getSelectByPeriodIdQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, period_id, date FROM document WHERE period_id = ? AND number >= ? ORDER BY number");
	}
	
	protected PreparedStatement getSelectCountByPeriodIdQuery() throws SQLException {
		return sess.prepareStatement("SELECT count(*) FROM document WHERE period_id = ? AND number >= ?");
	}
	
	protected PreparedStatement getSelectByPeriodIdAndNumberQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, period_id, date FROM document WHERE period_id = ? AND number BETWEEN ? AND ? ORDER BY number LIMIT ? OFFSET ?");
	}
	
	protected PreparedStatement getSelectCountByPeriodIdAndNumberQuery() throws SQLException {
		return sess.prepareStatement("SELECT count(*) FROM document WHERE period_id = ? AND number BETWEEN ? AND ?");
	}
	
	protected PreparedStatement getSelectNumberByPeriodIdAndNumberQuery() throws SQLException {
		return sess.prepareStatement("SELECT number FROM document WHERE period_id = ? AND number BETWEEN ? AND ? ORDER BY number");
	}
	
	protected PreparedStatement getSelectCountByPeriodIdAndPhraseQuery() throws SQLException {
		return sess.prepareStatement("SELECT count(*) FROM document d WHERE d.period_id = ? AND (SELECT count(*) FROM entry e WHERE e.document_id = d.id AND e.description LIKE ?) > 0");
	}
	
	protected PreparedStatement getSelectByPeriodIdAndPhraseQuery() throws SQLException {
		return sess.prepareStatement("SELECT d.id, d.number, d.period_id, d.date FROM document d WHERE d.period_id = ? AND (SELECT count(*) FROM entry e WHERE e.document_id = d.id AND e.description LIKE ?) > 0 ORDER BY d.number LIMIT ? OFFSET ?");
	}
	
	protected PreparedStatement getSelectByPeriodIdAndDateQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, period_id, date FROM document WHERE period_id = ? AND date BETWEEN ? AND ? ORDER BY number");
	}
	
	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO document (number, period_id, date) VALUES (?, ?, ?)");
	}
	
	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE document SET number=?, period_id=?, date=? WHERE id = ?");
	}
	
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM document WHERE id = ?");
	}
	
	protected PreparedStatement getDeleteByPeriodIdQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM document WHERE period_id = ?");
	}
	
	protected PreparedStatement getNumberShiftQuery() throws SQLException {
		return sess.prepareStatement("UPDATE document SET number = number + ? WHERE period_id = ? AND number BETWEEN ? AND ?");
	}
}
