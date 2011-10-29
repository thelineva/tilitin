package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentDAO;

/**
 * <code>SQLDocumentDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tositteita sekä hakea olemassa olevien tositteiden
 * tietoja. Aliluokassa on määriteltävä toteutukset metodeilla, jotka
 * palauttavat SQL-kyselymerkkijonot.
 * 
 * @author Tommi Helineva
 */
public abstract class SQLDocumentDAO implements DocumentDAO {
	/**
	 * Luo uuden tositteen. Tositenumeroksi asetetaan seuraava
	 * vapaa numero, ja päivämäärä kopioidaan viimeisestä tositteesta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param numberStart numerovälin alku
	 * @param numberEnd numerovälin loppu
	 * @return uusi tosite
	 * @throws DataAccessException jos tositteen luominen epäonnistuu
	 */
	public Document create(int periodId, int numberStart, int numberEnd)
		throws DataAccessException {
		
		Document obj;
		ResultSet rs;
		int number;
		Date date;
		
		try {
			PreparedStatement stmt = getSelectLastDocumentQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, numberStart);
			stmt.setInt(3, numberEnd);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				number = rs.getInt(1) + 1;
				date = rs.getDate(2);
			}
			else {
				number = numberStart;
				date = new Date();
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		
		obj = new Document();
		obj.setPeriodId(periodId);
		obj.setNumber(number);
		obj.setDate(date);
		return obj;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan suurin tositenumero.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectLastDocumentQuery()
		throws SQLException;
	
	/**
	 * Hakee tietokannasta kaikki tietyn tilikauden tositteet.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param numberOffset tositenumero >= <code>numberOffset</code>
	 * @return tositteet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Document> getByPeriodId(int periodId, int numberOffset)
		throws DataAccessException
	{
		ArrayList<Document> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectByPeriodIdQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, numberOffset);
			rs = stmt.executeQuery();
			list = new ArrayList<Document>();
			
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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikki tietyn
	 * tilikauden tositteet. Kyselyssä on yksi parametri, joka on
	 * tilikauden tunniste.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdQuery() throws SQLException;

	/**
	 * Hakee tietokannasta tietyn tilikauden tositteiden lukumäärän.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param numberOffset tositenumero >= <code>numberOffset</code>
	 * @return tositteiden lukumäärä
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int getCountByPeriodId(int periodId, int numberOffset)
		throws DataAccessException
	{
		int count = 0;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectCountByPeriodIdQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, numberOffset);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				count = rs.getInt(1);
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		
		return count;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tietyn tilikauden
	 * tositteiden lukumäärä.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectCountByPeriodIdQuery() throws SQLException;
	
	/**
	 * Hakee tietokannasta tositteen numerolla <code>number</code>
	 * tietyltä tilikaudelta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param number tositenumero
	 * @return tosite tai <code>null</code>, jos tositetta ei löytynyt
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public Document getByPeriodIdAndNumber(int periodId, int number)
		throws DataAccessException
	{
		Document obj = null;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectByPeriodIdAndNumberQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, number);
			stmt.setInt(3, number);
			stmt.setInt(4, 1);
			stmt.setInt(5, 0);
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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tositteet tietyltä
	 * numeroväliltä ja tietyltä tilikaudelta.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndNumberQuery() throws SQLException;
	
	/**
	 * Hakee tietokannasta tositteiden lukumäärän numeroväliltä
	 * <code>startNumber</code>..<code>endNumber</code>
	 * tietyltä tilikaudelta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param startNumber tositenumerovälin alku
	 * @param endNumber tositenumerovälin loppu
	 * @return tositteet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int getCountByPeriodIdAndNumber(int periodId,
			int startNumber, int endNumber) throws DataAccessException
	{
		int count = 0;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectCountByPeriodIdAndNumberQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, startNumber);
			stmt.setInt(3, endNumber);
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				count = rs.getInt(1);
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		
		return count;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tositteiden lukumäärä
	 * tietyltä numeroväliltä ja tietyltä tilikaudelta.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectCountByPeriodIdAndNumberQuery() throws SQLException;
	
	/**
	 * Hakee tietokannasta sen tositteen järjestysnumeron,
	 * jonka numero on <code>number</code>.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param startNumber tositenumerovälin alku
	 * @param endNumber tositenumerovälin loppu
	 * @param number haettava tositenumero
	 * @return järjestysnumero tai -1, jos tositenumeroa ei löytynyt
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int getIndexByPeriodIdAndNumber(int periodId,
			int startNumber, int endNumber, int number)
			throws DataAccessException {
		
		int index = -1;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectNumberByPeriodIdAndNumberQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, startNumber);
			stmt.setInt(3, endNumber);
			rs = stmt.executeQuery();
			int i = 0;
			
			while (rs.next()) {
				if (rs.getInt(1) == number) {
					index = i;
					break;
				}
				
				i++;
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		
		return index;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tositenumerot
	 * tietyltä numeroväliltä ja tietyltä tilikaudelta.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectNumberByPeriodIdAndNumberQuery() throws SQLException;
	
	/**
	 * Hakee tietokannasta tositteet numeroväliltä
	 * <code>startNumber</code>..<code>endNumber</code>
	 * tietyltä tilikaudelta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param startNumber tositenumerovälin alku
	 * @param endNumber tositenumerovälin loppu
	 * @param offset
	 * @param limit
	 * @return tositteet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Document> getByPeriodIdAndNumber(int periodId,
			int startNumber, int endNumber, int offset, int limit)
			throws DataAccessException
	{
		ArrayList<Document> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectByPeriodIdAndNumberQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, startNumber);
			stmt.setInt(3, endNumber);
			stmt.setInt(4, limit);
			stmt.setInt(5, offset);
			rs = stmt.executeQuery();
			list = new ArrayList<Document>();
			
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
	 * Etsii tositteita hakusanalla <code>q</code> ja palauttaa
	 * tulosten lukumäärän.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param q hakusana
	 * @return tulosten lukumäärä
	 */
	public int getCountByPeriodIdAndPhrase(int periodId, String q) throws DataAccessException {
		int count = 0;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectCountByPeriodIdAndPhraseQuery();
			stmt.setInt(1, periodId);
			stmt.setString(2, escapePhrase(q));
			rs = stmt.executeQuery();
			
			if (rs.next()) {
				count = rs.getInt(1);
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
		
		return count;
	}
	
	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tositteet, joiden
	 * vienneistä löytyy tietty merkkijono.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectCountByPeriodIdAndPhraseQuery() throws SQLException;
	
	/**
	 * Etsii tositteita hakusanalla <code>q</code> ja palauttaa
	 * löytyneet tositteet
	 * 
	 * @param periodId tilikauden tunniste
	 * @param q hakusana
	 * @param offset
	 * @param limit
	 * @return tositteet
	 */
	public List<Document> getByPeriodIdAndPhrase(int periodId,
			String q, int offset, int limit) throws DataAccessException
	{
		ArrayList<Document> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectByPeriodIdAndPhraseQuery();
			stmt.setInt(1, periodId);
			stmt.setString(2, escapePhrase(q));
			stmt.setInt(3, limit);
			stmt.setInt(4, offset);
			rs = stmt.executeQuery();
			list = new ArrayList<Document>();
			
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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tositteet, joiden
	 * vienneistä löytyy tietty merkkijono.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndPhraseQuery() throws SQLException;
	
	private String escapePhrase(String q) {
		q = q.replace("%", "\\%").replace("_", "\\_").replace("*", "%");
		if (!q.endsWith("%")) q += "%";
		return q;
	}
	
	/**
	 * Hakee tietokannasta tositteet tietyltä aikaväliltä.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param startDate aikavälin alku
	 * @param endDate aikavälin loppu
	 * @return tositteet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Document> getByPeriodIdAndDate(int periodId,
			Date startDate, Date endDate) throws DataAccessException {
		
		ArrayList<Document> list;
		ResultSet rs;
		
		try {
			PreparedStatement stmt = getSelectByPeriodIdAndDateQuery();
			stmt.setInt(1, periodId);
			stmt.setDate(2, new java.sql.Date(startDate.getTime()));
			stmt.setDate(3, new java.sql.Date(endDate.getTime()));
			rs = stmt.executeQuery();
			list = new ArrayList<Document>();
			
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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tositteet
	 * tietyltä aikaväliltä.
	 * 
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndDateQuery() throws SQLException;
	
	/**
	 * Tallentaa tositteen tiedot tietokantaan.
	 * 
	 * @param document tallennettava tosite
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Document document) throws DataAccessException {
		try {
			if (document.getId() == 0) {
				executeInsertQuery(document);
			}
			else {
				executeUpdateQuery(document);
			}
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lisää tositteen tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tosite
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(Document obj) throws SQLException {
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
	 * Palauttaa tietokantaan lisätyn tositteen tunnisteen.
	 *
	 * @return tositteen tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää tositteen tiedot tietokantaan.
	 * 
	 * @param obj tallennettava tosite
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(Document obj) throws SQLException {
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
	 * Poistaa tositteen tiedot tietokannasta.
	 * 
	 * @param documentId poistettavan tositteen tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int documentId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, documentId);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on tositteen tunniste.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteQuery() throws SQLException;
	
	/**
	 * Poistaa tietyn tilikauden kaikki tositteet tietokannasta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void deleteByPeriodId(int periodId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteByPeriodIdQuery();
			stmt.setInt(1, periodId);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan kaikki
	 * tietyn tilikauden tositteet.
	 * 
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteByPeriodIdQuery() throws SQLException;
	
	/**
	 * Muuttaa tositenumeroita välillä <code>startNumber</code>..<code>endNumber</code>.
	 * 
	 * @param startNumber välin alku
	 * @param endNumber välin loppu
	 * @param shift muutos
	 * @throws DataAccessException jos tositenumeroiden muuttaminen epäonnistuu
	 */
	public void shiftNumbers(int periodId, int startNumber, int endNumber, int shift)
			throws DataAccessException {
		
		try {
			PreparedStatement stmt = getNumberShiftQuery();
			stmt.setInt(1, shift);
			stmt.setInt(2, periodId);
			stmt.setInt(3, startNumber);
			stmt.setInt(4, endNumber);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	/**
	 * Palauttaa UPDATE-kyselyn, jonka avulla muutetaan tositenumeroita
	 * tietyllä välillä.
	 * 
	 * @return UPDATE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getNumberShiftQuery() throws SQLException;

	/**
	 * Lukee <code>ResultSetistä</code> rivin kentät ja
	 * luo <code>Document</code>-olion.
	 * 
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected Document createObject(ResultSet rs) throws SQLException {
		Document obj = new Document();
		obj.setId(rs.getInt(1));
		obj.setNumber(rs.getInt(2));
		obj.setPeriodId(rs.getInt(3));
		obj.setDate(rs.getDate(4));
		return obj;
	}
	
	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 * 
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, Document obj)
		throws SQLException
	{
		stmt.setInt(1, obj.getNumber());
		stmt.setInt(2, obj.getPeriodId());
		stmt.setDate(3, new java.sql.Date(obj.getDate().getTime()));
	}
}
