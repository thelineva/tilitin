package kirjanpito.db.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLAccountDAO;

/**
 * <code>PSQLAccountDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilitietoja sekä hakea olemassa olevien tilien tietoja.
 *
 * @author Tommi Helineva
 */
public class PSQLAccountDAO extends SQLAccountDAO {
	private PSQLSession sess;

	public PSQLAccountDAO(PSQLSession sess) {
		this.sess = sess;
	}

	protected int getGeneratedKey() throws SQLException {
		return sess.getSequenceValue("account_id_seq");
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, type, vat_code, vat_percentage, vat_account1_id, vat_account2_id, flags FROM account ORDER BY number");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO account (id, number, name, type, vat_code, vat_percentage, vat_account1_id, vat_account2_id, flags) VALUES (nextval('account_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?)");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE account SET number=?, name=?, type=?, vat_code=?, vat_percentage=?, vat_account1_id=?, vat_account2_id=?, flags=? WHERE id = ?");
	}

	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM account WHERE id = ?");
	}
}
