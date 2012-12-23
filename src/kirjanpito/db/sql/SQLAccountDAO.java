package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.AccountDAO;
import kirjanpito.db.DataAccessException;

/**
 * <code>SQLAccountDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilitietoja sekä hakea olemassa olevien tilien tietoja.
 * Aliluokassa on määriteltävä toteutukset metodeilla, jotka palauttavat
 * SQL-kyselymerkkijonot.
 *
 * @author Tommi Helineva
 */
public abstract class SQLAccountDAO implements AccountDAO {
	/**
	 * Hakee tietokannasta kaikkien tilien tiedot
	 * numerojärjestyksessä.
	 *
	 * @return tilit
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Account> getAll() throws DataAccessException {
		ArrayList<Account> list;
		ResultSet rs;

		try {
			PreparedStatement stmt = getSelectAllQuery();
			rs = stmt.executeQuery();
			list = new ArrayList<Account>();

			/* Luetaan tiedot ResultSetistä ja luodaan oliot. */
			while (rs.next()) {
				list.add(createObject(rs));
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}

		return list;
	}

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikkien rivien
	 * kaikki kentät.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectAllQuery() throws SQLException;

	/**
	 * Tallentaa tilin tiedot tietokantaan.
	 *
	 * @param account tallennettava tili
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Account account) throws DataAccessException {
		try {
			if (account.getId() == 0) {
				executeInsertQuery(account);
			}
			else {
				executeUpdateQuery(account);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Lisää tilin tiedot tietokantaan.
	 *
	 * @param obj tallennettava tili
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(Account obj) throws SQLException {
		PreparedStatement stmt = getInsertQuery();
		setValuesToStatement(stmt, obj);
		stmt.executeUpdate();
		stmt.close();
		obj.setId(getGeneratedKey());
	}

	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 *
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getInsertQuery() throws SQLException;

	/**
	 * Palauttaa tietokantaan lisätyn tilin tunnisteen.
	 *
	 * @return tilin tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää tilin tiedot tietokantaan.
	 *
	 * @param obj tallennettava tili
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(Account obj) throws SQLException {
		PreparedStatement stmt = getUpdateQuery();
		setValuesToStatement(stmt, obj);
		stmt.setInt(9, obj.getId());
		stmt.executeUpdate();
		stmt.close();
	}

	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 *
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getUpdateQuery() throws SQLException;

	/**
	 * Poistaa tilin tiedot tietokannasta.
	 *
	 * @param accountId poistettavan tilin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int accountId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, accountId);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on tilin tunniste.
	 *
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteQuery() throws SQLException;

	/**
	 * Lukee <code>ResultSetistä</code> rivin kentät ja
	 * luo <code>Account</code>-olion.
	 *
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected Account createObject(ResultSet rs) throws SQLException {
		Account obj = new Account();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getString(2));
		obj.setName(rs.getString(3));
		obj.setType(rs.getInt(4));
		obj.setVatCode(rs.getInt(5));
		obj.setVatRate(rs.getBigDecimal(6));
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

	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 *
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, Account obj)
		throws SQLException
	{
		stmt.setString(1, obj.getNumber());
		stmt.setString(2, obj.getName());
		stmt.setInt(3, obj.getType());
		stmt.setInt(4, obj.getVatCode());
		stmt.setBigDecimal(5, obj.getVatRate());

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