package kirjanpito.db.sqlite;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import kirjanpito.db.EntryTemplate;
import kirjanpito.db.sql.SQLEntryTemplateDAO;

public class SQLiteEntryTemplateDAO extends SQLEntryTemplateDAO {
	private SQLiteSession sess;
	
	public SQLiteEntryTemplateDAO(SQLiteSession sess) {
		this.sess = sess;
	}
	
	protected int getGeneratedKey() throws SQLException {
		return sess.getInsertId();
	}
	
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM entry_template WHERE id = ?");
	}

	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO entry_template (id, number, name, account_id, debit, amount, description, row_number) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)");
	}

	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, account_id, debit, amount, description, row_number FROM entry_template ORDER BY number, row_number");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE entry_template SET number=?, name=?, account_id=?, debit=?, amount=?, description=?, row_number=? WHERE id = ?");
	}
	
	protected EntryTemplate createObject(ResultSet rs) throws SQLException {
		EntryTemplate obj = new EntryTemplate();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getInt(2));
		obj.setName(rs.getString(3));
		obj.setAccountId(rs.getInt(4));
		obj.setDebit(rs.getBoolean(5));
		obj.setAmount(new BigDecimal(rs.getString(6)));
		obj.setDescription(rs.getString(7));
		obj.setRowNumber(rs.getInt(8));
		return obj;
	}
	
	protected void setValuesToStatement(PreparedStatement stmt, EntryTemplate obj)
		throws SQLException
	{
		stmt.setInt(1, obj.getNumber());
		stmt.setString(2, obj.getName());
		stmt.setInt(3, obj.getAccountId());
		stmt.setBoolean(4, obj.isDebit());
		stmt.setString(5, obj.getAmount().toString());
		stmt.setString(6, obj.getDescription());
		stmt.setInt(7, obj.getRowNumber());
	}
}
