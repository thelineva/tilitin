package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DocumentType;
import kirjanpito.db.DocumentTypeDAO;

public abstract class SQLDocumentTypeDAO implements DocumentTypeDAO {
	public List<DocumentType> getAll() throws DataAccessException {
		ArrayList<DocumentType> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectAllQuery();
			rs = stmt.executeQuery();
			list = new ArrayList<DocumentType>();
			
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
	 * Tallentaa tositelajin tiedot tietokantaan.
	 * 
	 * @param documentType tallennettava tositelaji
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(DocumentType documentType) throws DataAccessException {
		try {
			if (documentType.getId() == 0) {
				executeInsertQuery(documentType);
			}
			else {
				executeUpdateQuery(documentType);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lisää tositelajin tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tositelaji
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(DocumentType obj) throws SQLException {
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
	 * Palauttaa tietokantaan lisätyn tositelajin tunnisteen.
	 *
	 * @return tositelajin tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää tilin tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tili
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(DocumentType obj) throws SQLException {
		PreparedStatement stmt = getUpdateQuery();
		setValuesToStatement(stmt, obj);
		stmt.setInt(5, obj.getId());
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
	 * @param DocumentTypeId poistettavan tilin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int DocumentTypeId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, DocumentTypeId);
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
	 * luo <code>DocumentType</code>-olion.
	 * 
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected DocumentType createObject(ResultSet rs) throws SQLException {
		DocumentType obj = new DocumentType();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getInt(2));
		obj.setName(rs.getString(3));
		obj.setNumberStart(rs.getInt(4));
		obj.setNumberEnd(rs.getInt(5));
		return obj;
	}
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, DocumentType obj)
		throws SQLException
	{
		stmt.setInt(1, obj.getNumber());
		stmt.setString(2, obj.getName());
		stmt.setInt(3, obj.getNumberStart());
		stmt.setInt(4, obj.getNumberEnd());
	}
}
