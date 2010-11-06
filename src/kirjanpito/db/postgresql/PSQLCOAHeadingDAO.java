package kirjanpito.db.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import kirjanpito.db.COAHeading;
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
	
	/**
	 * Luo <code>PSQLCOAHeadingDAO</code>-olion, joka käyttää
	 * tietokantaistuntoa <code>sess</code>
	 * 
	 * @param sess tietokantaistunto
	 */
	public PSQLCOAHeadingDAO(PSQLSession sess) {
		this.sess = sess;
	}
	
	/**
	 * Lisää otsikon tiedot tietokantaan.
	 * 
	 * @param obj tallennettava otsikko
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(COAHeading obj) throws SQLException {
		super.executeInsertQuery(obj);
		/* Haetaan palvelimelta uusi sekvenssin arvo
		 * ja päivitetään se olioon. */
		obj.setId(sess.getSequenceValue("coa_heading_id_seq"));
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikkien rivien
	 * kaikki kentät.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, text, level FROM coa_heading ORDER BY number, level");
	}
	
	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO coa_heading (id, number, text, level) VALUES (nextval('coa_heading_id_seq'), ?, ?, ?)");
	}
	
	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE coa_heading SET number=?, text=?, level=? WHERE id = ?");
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on otsikon tunniste.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM coa_heading WHERE id = ?");
	}
}
