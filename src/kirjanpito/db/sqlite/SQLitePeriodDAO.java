package kirjanpito.db.sqlite;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLPeriodDAO;

/**
 * <code>SQLitePeriodDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilikausia sekä hakea olemassa olevien tilikausien
 * tietoja.
 *
 * @author Tommi Helineva
 */
public class SQLitePeriodDAO extends SQLPeriodDAO {
	private SQLiteSession sess;

	public SQLitePeriodDAO(SQLiteSession sess) {
		this.sess = sess;
	}

	protected int getGeneratedKey() throws SQLException {
		return sess.getInsertId();
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, start_date, end_date, locked FROM period ORDER BY start_date");
	}

	protected PreparedStatement getSelectCurrentQuery() throws SQLException {
		return sess.prepareStatement("SELECT p.id, p.start_date, p.end_date, p.locked FROM period p INNER JOIN settings s ON s.current_period_id = p.id");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO period (id, start_date, end_date, locked) VALUES (NULL, ?, ?, ?)");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE period SET start_date=?, end_date=?, locked=? WHERE id = ?");
	}

	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM period WHERE id = ?");
	}
}
