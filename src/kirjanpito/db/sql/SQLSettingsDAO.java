package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.Settings;
import kirjanpito.db.SettingsDAO;

/**
 * <code>SQLSettingsDAO</code>:n avulla voidaan hakea ja tallentaa
 * asetukset. Aliluokassa on määriteltävä toteutukset metodeilla, jotka
 * palauttavat SQL-kyselymerkkijonot.
 * 
 * @author Tommi Helineva
 */
public abstract class SQLSettingsDAO implements SettingsDAO {
	/**
	 * Hakee tietokannasta asetukset.
	 * 
	 * @return asetukset
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public Settings get() throws DataAccessException {
		Settings obj = new Settings();
		
		try {
			PreparedStatement stmt = getSelectQuery();
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				obj.setName(rs.getString(1));
				obj.setBusinessId(rs.getString(2));
				obj.setCurrentPeriodId(rs.getInt(3));
				obj.setDocumentTypeId(rs.getInt(4));
				if (rs.wasNull()) obj.setDocumentTypeId(-1);
				obj.parseProperties(rs.getString(5));
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
	 * Palauttaa SELECT-kyselyn, jonka avulla asetukset haetaan
	 * tietokannasta. Kysely palauttaa vain yhden rivin.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectQuery() throws SQLException;
	
	/**
	 * Tallentaa asetukset tietokantaan.
	 * 
	 * @param settings asetukset
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Settings settings) throws DataAccessException {
		int updatedRows;
		
		try {
			PreparedStatement stmt = getUpdateQuery();
			setValuesToStatement(stmt, settings);
			updatedRows = stmt.executeUpdate();
			
			if (updatedRows == 0) {
				stmt.close();
				stmt = getInsertQuery();
				setValuesToStatement(stmt, settings);
				stmt.executeUpdate();
			}
			
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getInsertQuery() throws SQLException;
	
	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getUpdateQuery() throws SQLException;
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, Settings obj)
		throws SQLException
	{
		stmt.setString(1, obj.getName());
		stmt.setString(2, obj.getBusinessId());
		stmt.setInt(3, obj.getCurrentPeriodId());
		
		if (obj.getDocumentTypeId() < 0) {
			stmt.setNull(4, Types.INTEGER);
		}
		else {
			stmt.setInt(4, obj.getDocumentTypeId());
		}
		
		stmt.setString(5, obj.propertiesToString());
	}
}
