package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kirjanpito.db.COAHeading;
import kirjanpito.db.COAHeadingDAO;
import kirjanpito.db.DataAccessException;

/**
 * <code>SQLCOAHeadingDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilikartan otsikoita sekä hakea olemassa olevien otsikoiden
 * tietoja. Aliluokassa on määriteltävä toteutukset metodeilla, jotka
 * palauttavat SQL-kyselymerkkijonot.
 * 
 * @author Tommi Helineva
 */
public abstract class SQLCOAHeadingDAO implements COAHeadingDAO {
	/**
	 * Hakee kaikkien otsikoiden tiedot tietokannasta
	 * numerojärjestyksessä.
	 * 
	 * @return otsikot
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<COAHeading> getAll() throws DataAccessException {
		ArrayList<COAHeading> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectAllQuery();
			rs = stmt.executeQuery();
			list = new ArrayList<COAHeading>();
			
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
	 * Tallentaa tilikartan otsikon tiedot tietokantaan.
	 * 
	 * @param obj tallennettava otsikko
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(COAHeading obj) throws DataAccessException {
		try {
			if (obj.getId() == 0) {
				executeInsertQuery(obj);
			}
			else {
				executeUpdateQuery(obj);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lisää otsikon tiedot tietokantaan.
	 * 
	 * @param obj tallennettava otsikko
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(COAHeading obj) throws SQLException {
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
	 * Palauttaa tietokantaan lisätyn otsikon tunnisteen.
	 *
	 * @return otsikon tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää otsikon tiedot tietokantaan.
	 * 
	 * @param obj tallennettava otsikko
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(COAHeading obj) throws SQLException {
		PreparedStatement stmt = getUpdateQuery();
		setValuesToStatement(stmt, obj);
		stmt.setInt(4, obj.getId());
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
	 * Poistaa tilikartan otsikon tiedot tietokannasta.
	 * 
	 * @param headingId poistettavan otsikon tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int headingId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, headingId);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on otsikon tunniste.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteQuery() throws SQLException;
	
	/**
	 * Lukee <code>ResultSetistä</code> rivin kentät ja
	 * luo <code>COAHeading</code>-olion.
	 * 
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected COAHeading createObject(ResultSet rs) throws SQLException {
		COAHeading obj = new COAHeading();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getString(2));
		obj.setText(rs.getString(3));
		obj.setLevel(rs.getInt(4)); 
		return obj;
	}
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, COAHeading obj)
		throws SQLException
	{
		stmt.setString(1, obj.getNumber());
		stmt.setString(2, obj.getText());
		stmt.setInt(3, obj.getLevel());
	}
}
