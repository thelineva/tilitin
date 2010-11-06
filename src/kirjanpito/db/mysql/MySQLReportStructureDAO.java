package kirjanpito.db.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLReportStructureDAO;

public class MySQLReportStructureDAO extends SQLReportStructureDAO {
	private MySQLSession sess;
	
	public MySQLReportStructureDAO(MySQLSession sess) {
		this.sess = sess;
	}
	
	protected PreparedStatement getSelectByIdQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, data FROM report_structure WHERE id = ?");
	}
	
	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO report_structure (id, data) VALUES (?, ?)");
	}

	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE report_structure SET id=?, data=? WHERE id = ?");
	}
}
