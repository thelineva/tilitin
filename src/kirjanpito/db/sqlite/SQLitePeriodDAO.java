package kirjanpito.db.sqlite;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.Period;
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
	
	/**
	 * Luo <code>SQLitePeriodDAO</code>-olion, joka käyttää
	 * tietokantaistuntoa <code>sess</code>
	 * 
	 * @param sess tietokantaistunto
	 */
	public SQLitePeriodDAO(SQLiteSession sess) {
		this.sess = sess;
	}
	
	/**
	 * Lisää tilikauden tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tilikausi
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(Period obj) throws SQLException {
		super.executeInsertQuery(obj);
		/* Haetaan palvelimelta uuden rivin tunniste
		 * ja päivitetään se olioon. */ 
		obj.setId(sess.getInsertId());
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikkien tilikausien
	 * tiedot aikajärjestyksessä.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, start_date, end_date, locked FROM period ORDER BY start_date");
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan nykyisen
	 * tilikauden tiedot.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getSelectCurrentQuery() throws SQLException {
		return sess.prepareStatement("SELECT p.id, p.start_date, p.end_date, p.locked FROM period p INNER JOIN settings s ON s.current_period_id = p.id");
	}
	
	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO period (id, start_date, end_date, locked) VALUES (NULL, ?, ?, ?)");
	}
	
	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE period SET start_date=?, end_date=?, locked=? WHERE id = ?");
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on tilikauden tunniste.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM period WHERE id = ?");
	}
}
