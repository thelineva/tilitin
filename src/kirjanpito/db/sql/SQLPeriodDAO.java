package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.Period;
import kirjanpito.db.PeriodDAO;

/**
 * <code>SQLPeriodDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilikausia sekä hakea olemassa olevien tilikausien
 * tietoja. Aliluokassa on määriteltävä toteutukset metodeilla, jotka
 * palauttavat SQL-kyselymerkkijonot.
 * 
 * @author Tommi Helineva
 */
public abstract class SQLPeriodDAO implements PeriodDAO {
	/**
	 * Hakee tietokannasta kaikkien tilikausien tiedot
	 * aikajärjestyksessä.
	 * 
	 * @return tilikaudet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Period> getAll() throws DataAccessException {
		ArrayList<Period> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectAllQuery();
			rs = stmt.executeQuery();
			list = new ArrayList<Period>();
			
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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikkien tilikausien
	 * tiedot aikajärjestyksessä.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectAllQuery() throws SQLException;

	/**
	 * Hakee tietokannasta nykyisen tilikauden tiedot.
	 *  
	 * @return tilikausi
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public Period getCurrent() throws DataAccessException {
		Period obj = null;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectCurrentQuery();
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				obj = createObject(rs);
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		
		return obj;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan nykyisen
	 * tilikauden tiedot.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectCurrentQuery() throws SQLException;

	/**
	 * Tallentaa tilikauden tiedot tietokantaan.
	 * 
	 * @param period tilikausi
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Period period) throws DataAccessException {
		try {
			if (period.getId() == 0) {
				executeInsertQuery(period);
			}
			else {
				executeUpdateQuery(period);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lisää tilikauden tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tilikausi
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(Period obj) throws SQLException {
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
	 * Palauttaa tietokantaan lisätyn tilikauden tunnisteen.
	 *
	 * @return tilikauden tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää tilikauden tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tilikausi
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(Period obj) throws SQLException {
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
	 * Poistaa tilikauden tiedot tietokannasta.
	 * 
	 * @param periodId poistettavan tilikauden tunnus
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void delete(int periodId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, periodId);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on tilikauden tunniste.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteQuery() throws SQLException;
	
	/**
	 * Lukee <code>ResultSetistä</code> rivin kentät ja
	 * luo <code>Period</code>-olion.
	 * 
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected Period createObject(ResultSet rs) throws SQLException {
		Period obj = new Period();
		obj.setId(rs.getInt(1));
		obj.setStartDate(rs.getDate(2));
		obj.setEndDate(rs.getDate(3));
		obj.setLocked(rs.getBoolean(4));
		return obj;
	}
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, Period obj)
		throws SQLException
	{
		stmt.setDate(1, new java.sql.Date(obj.getStartDate().getTime()));
		stmt.setDate(2, new java.sql.Date(obj.getEndDate().getTime()));
		stmt.setBoolean(3, obj.isLocked());
	}
}
