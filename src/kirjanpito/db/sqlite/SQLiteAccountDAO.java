package kirjanpito.db.sqlite;

import java.sql.PreparedStatement;
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
	
	/**
	 * Luo <code>SQLiteAccountDAO</code>-olion, joka käyttää
	 * tietokantaistuntoa <code>sess</code>
	 * 
	 * @param sess tietokantaistunto
	 */
	public SQLiteAccountDAO(SQLiteSession sess) {
		this.sess = sess;
	}
	
	/**
	 * Lisää tilin tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tili
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(Account obj) throws SQLException {
		super.executeInsertQuery(obj);
		/* Haetaan uuden rivin tunniste
		 * ja päivitetään se olioon. */ 
		obj.setId(sess.getInsertId());
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikkien rivien
	 * kaikki kentät.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getSelectAllQuery() throws SQLException {
		return sess.prepareStatement("SELECT id, number, name, type, vat_code, vat_rate, vat_account1_id, vat_account2_id FROM account ORDER BY number");
	}
	
	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getInsertQuery() throws SQLException {
		return sess.prepareStatement("INSERT INTO account (id, number, name, type, vat_code, vat_rate, vat_account1_id, vat_account2_id) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)");
	}
	
	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getUpdateQuery() throws SQLException {
		return sess.prepareStatement("UPDATE account SET number=?, name=?, type=?, vat_code=?, vat_rate=?, vat_account1_id=?, vat_account2_id=? WHERE id = ?");
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on tilin tunniste.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected PreparedStatement getDeleteQuery() throws SQLException {
		return sess.prepareStatement("DELETE FROM account WHERE id = ?");
	}
}
