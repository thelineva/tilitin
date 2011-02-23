package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.EntryTemplate;
import kirjanpito.db.EntryTemplateDAO;

/**
 * <code>SQLEntryTemplateDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa vientimalleja sekä hakea olemassa olevien vientimallien tietoja.
 * Aliluokassa on määriteltävä toteutukset metodeilla, jotka palauttavat
 * SQL-kyselymerkkijonot.
 * 
 * @author Tommi Helineva
 */
public abstract class SQLEntryTemplateDAO implements EntryTemplateDAO {
	/**
	 * Hakee tietokannasta kaikkien vientimallien tiedot
	 * numerojärjestyksessä.
	 * 
	 * @return tilit
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<EntryTemplate> getAll() throws DataAccessException {
		ArrayList<EntryTemplate> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectAllQuery();
			rs = stmt.executeQuery();
			list = new ArrayList<EntryTemplate>();
			
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
	 * Tallentaa vientimallin tiedot tietokantaan.
	 * 
	 * @param EntryTemplate tallennettava vientimalli
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(EntryTemplate template) throws DataAccessException {
		try {
			if (template.getId() == 0) {
				executeInsertQuery(template);
			}
			else {
				executeUpdateQuery(template);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lisää vientimallin tiedot tietokantaan.
	 * 
	 * @param obj tallennettava vientimalli
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(EntryTemplate obj) throws SQLException {
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
	 * Palauttaa tietokantaan lisätyn vientimallin tunnisteen.
	 *
	 * @return vientimallin tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää vientimallin tiedot tietokantaan.
	 * 
	 * @param obj tallennettava vientimalli
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(EntryTemplate obj) throws SQLException {
		PreparedStatement stmt = getUpdateQuery();
		setValuesToStatement(stmt, obj);
		stmt.setInt(8, obj.getId());
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
	 * @param EntryTemplateId poistettavan tilin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int templateId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, templateId);
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
	 * luo <code>EntryTemplate</code>-olion.
	 * 
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected EntryTemplate createObject(ResultSet rs) throws SQLException {
		EntryTemplate obj = new EntryTemplate();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getInt(2));
		obj.setName(rs.getString(3));
		obj.setAccountId(rs.getInt(4));
		obj.setDebit(rs.getBoolean(5));
		obj.setAmount(rs.getBigDecimal(6));
		obj.setDescription(rs.getString(7));
		obj.setRowNumber(rs.getInt(8));
		return obj;
	}
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, EntryTemplate obj)
		throws SQLException
	{
		stmt.setInt(1, obj.getNumber());
		stmt.setString(2, obj.getName());
		stmt.setInt(3, obj.getAccountId());
		stmt.setBoolean(4, obj.isDebit());
		stmt.setBigDecimal(5, obj.getAmount());
		stmt.setString(6, obj.getDescription());
		stmt.setInt(7, obj.getRowNumber());
	}
}