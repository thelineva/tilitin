package kirjanpito.db.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLReportStructureDAO;

public class PSQLReportStructureDAO extends SQLReportStructureDAO {
	private PSQLSession sess;
	
	public PSQLReportStructureDAO(PSQLSession sess) {
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
