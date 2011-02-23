package kirjanpito.db.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLCOAHeadingDAO;

/**
 * <code>PSQLCOAHeadingDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilikartan otsikoita sekä hakea olemassa olevien otsikoiden
 * tietoja.
 *
 * @author Tommi Helineva
 */
public class PSQLCOAHeadingDAO extends SQLCOAHeadingDAO {
	private PSQLSession sess;

	public PSQLCOAHeadingDAO(PSQLSession sess) {
		this.sess = sess;
	}

	protected int getGeneratedKey() throws SQLException {
		return sess.getSequenceValue("coa_heading_id_seq");
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, text, level FROM coa_heading ORDER BY number, level");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO coa_heading (id, number, text, level) VALUES (nextval('coa_heading_id_seq'), ?, ?, ?)");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE coa_heading SET number=?, text=?, level=? WHERE id = ?");
	}

	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM coa_heading WHERE id = ?");
	}
}
