package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.ReportStructure;
import kirjanpito.db.ReportStructureDAO;

/**
 * <code>SQLReportStructureDAO</code>:n avulla voidaan tallentaa ja
 * hakea tulosteiden rakennemäärittelyt. Aliluokassa on määriteltävä
 * toteutukset metodeilla, jotka palauttavat SQL-kyselymerkkijonot.
 * 
 * @author Tommi Helineva
 */
public abstract class SQLReportStructureDAO implements ReportStructureDAO {
	/**
	 * Hakee tulosteen rakennemäärittelyt tunnisteen perusteella.
	 * 
	 * @param id tulosteen tunniste
	 * @return tulosteen rakennetiedot
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public ReportStructure getById(String id) throws DataAccessException {
		ReportStructure obj = null;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectByIdQuery();
			stmt.setString(1, id);
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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan
	 * tulosteen rakennemäärittelyt tunnisteen perusteella.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByIdQuery() throws SQLException;
	
	/**
	 * Tallentaa tulosteen rakennemäärittelyt tietokantaan.
	 * 
	 * @param structure tulosteen rakennetiedot
	 */
	public void save(ReportStructure structure) throws DataAccessException {
		try {
			if (!executeUpdateQuery(structure)) {
				executeInsertQuery(structure);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lisää tulosteen rakennemäärittelyt tietokantaan.
	 * 
	 * @param obj tallennettava tosite
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(ReportStructure obj) throws SQLException {
		PreparedStatement stmt = getInsertQuery();
		setValuesToStatement(stmt, obj);
		stmt.executeUpdate();
		stmt.close();
	}
	
	/**
	 * Palauttaa INSERT-kyselyn, jonka avulla rivi lisätään.
	 * 
	 * @return INSERT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getInsertQuery() throws SQLException;
	
	/**
	 * Päivittää tulosteen rakennemäärittelyt tietokantaan.
	 * 
	 * @param obj tallennettava tosite
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected boolean executeUpdateQuery(ReportStructure obj) throws SQLException {
		PreparedStatement stmt = getUpdateQuery();
		setValuesToStatement(stmt, obj);
		stmt.setString(3, obj.getId());
		boolean updated = (stmt.executeUpdate() > 0);
		stmt.close();
		return updated;
	}
	
	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla rivin kaikki kentät päivitetään.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getUpdateQuery() throws SQLException;
	
	/**
	 * Lukee <code>ResultSetistä</code> rivin kentät ja
	 * luo <code>ReportStructure</code>-olion.
	 * 
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected ReportStructure createObject(ResultSet rs) throws SQLException {
		ReportStructure obj = new ReportStructure();
		obj.setId(rs.getString(1));
		obj.setData(rs.getString(2));
		return obj;
	}
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, ReportStructure obj)
		throws SQLException
	{
		stmt.setString(1, obj.getId());
		stmt.setString(2, obj.getData());
	}
}
