package kirjanpito.db.sqlite;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.sql.SQLReportStructureDAO;

/**
 * <code>SQLiteReportStructureDAO</code>:n avulla voidaan tallentaa ja
 * hakea tulosteiden rakennetietoja.
 * 
 * @author Tommi Helineva
 */
public class SQLiteReportStructureDAO extends SQLReportStructureDAO {
	private SQLiteSession sess;
	
	/**
	 * Luo <code>SQLiteReportStructureDAO</code>-olion, joka käyttää
	 * tietokantaistuntoa <code>sess</code>
	 * 
	 * @param sess tietokantaistunto
	 */
	public SQLiteReportStructureDAO(SQLiteSession sess) {
		this.sess = sess;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan
	 * tulosteen rakennetiedot tunnisteen perusteella.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getSelectByIdQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, data FROM report_structure WHERE id = ?");
	}
	
	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO report_structure (id, data) VALUES (?, ?)");
	}

	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE report_structure SET id=?, data=? WHERE id = ?");
	}
}
