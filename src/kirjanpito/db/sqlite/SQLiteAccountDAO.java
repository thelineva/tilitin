package kirjanpito.db.sqlite;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import kirjanpito.db.Account;
import kirjanpito.db.sql.SQLAccountDAO;

/**
 * <code>SQLiteAccountDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilitietoja sekä hakea olemassa olevien tilien tietoja.
 *
 * @author Tommi Helineva
 */
public class SQLiteAccountDAO extends SQLAccountDAO {
	private SQLiteSession sess;

	public SQLiteAccountDAO(SQLiteSession sess) {
		this.sess = sess;
	}

	protected int getGeneratedKey() throws SQLException {
		return sess.getInsertId();
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, type, vat_code, vat_percentage, vat_account1_id, vat_account2_id, flags FROM account ORDER BY number");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO account (id, number, name, type, vat_code, vat_percentage, vat_account1_id, vat_account2_id, flags) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?)");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE account SET number=?, name=?, type=?, vat_code=?, vat_percentage=?, vat_account1_id=?, vat_account2_id=?, flags=? WHERE id = ?");
	}

	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM account WHERE id = ?");
	}

	protected Account createObject(ResultSet rs) throws SQLException {
		Account obj = new Account();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getString(2));
		obj.setName(rs.getString(3));
		obj.setType(rs.getInt(4));
		obj.setVatCode(rs.getInt(5));
		obj.setVatRate(new BigDecimal(rs.getString(6)));
		obj.setVatAccount1Id(rs.getInt(7));

		if (rs.wasNull()) {
			obj.setVatAccount1Id(-1);
		}

		obj.setVatAccount2Id(rs.getInt(8));

		if (rs.wasNull()) {
			obj.setVatAccount2Id(-1);
		}

		obj.setFlags(rs.getInt(9));
		return obj;
	}

	protected void setValuesToStatement(PreparedStatement stmt, Account obj)
		throws SQLException
	{
		stmt.setString(1, obj.getNumber());
		stmt.setString(2, obj.getName());
		stmt.setInt(3, obj.getType());
		stmt.setInt(4, obj.getVatCode());
		stmt.setString(5, obj.getVatRate().toString());

		if (obj.getVatAccount1Id() <= 0) {
			stmt.setNull(6, java.sql.Types.INTEGER);
		}
		else {
			stmt.setInt(6, obj.getVatAccount1Id());
		}

		if (obj.getVatAccount2Id() <= 0) {
			stmt.setNull(7, java.sql.Types.INTEGER);
		}
		else {
			stmt.setInt(7, obj.getVatAccount2Id());
		}

		stmt.setInt(8, obj.getFlags());
	}
}
