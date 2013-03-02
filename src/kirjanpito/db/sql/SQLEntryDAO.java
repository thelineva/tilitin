package kirjanpito.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;

/**
 * <code>SQLEntryDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa vientejä sekä hakea olemassa olevien vientien
 * tietoja. Aliluokassa on määriteltävä toteutukset metodeilla, jotka
 * palauttavat SQL-kyselymerkkijonot.
 *
 * @author Tommi Helineva
 */
public abstract class SQLEntryDAO implements EntryDAO {
	public List<Entry> getByDocumentId(int documentId) throws DataAccessException {
		ArrayList<Entry> list;
		ResultSet rs;

		try {
			PreparedStatement stmt = getSelectByDocumentIdQuery();
			stmt.setInt(1, documentId);
			rs = stmt.executeQuery();
			list = new ArrayList<Entry>();

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
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tiettyyn tositteeseen
	 * kuuluvat viennit.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByDocumentIdQuery()
		throws SQLException;

	public void getByDocuments(List<Document> documents, DTOCallback<Entry> callback)
		throws DataAccessException {

		ResultSet rs;
		StringBuilder sb = new StringBuilder();

		for (Document document : documents) {
			if (sb.length() > 0) sb.append(',');
			sb.append(document.getId());
		}

		try {
			PreparedStatement stmt = getSelectByDocumentIdsQuery(
					sb.toString());

			rs = stmt.executeQuery();

			while (rs.next()) {
				callback.process(createObject(rs));
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tiettyihin tositteisiin
	 * kuuluvat viennit.
	 *
	 * @param documentIds lista tositteiden tunnisteista
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByDocumentIdsQuery(
		String documentIds) throws SQLException;

	public void getByPeriodId(int periodId, int orderBy,
			DTOCallback<Entry> callback) throws DataAccessException
	{
		ResultSet rs;

		try {
			PreparedStatement stmt = null;

			if (orderBy == ORDER_BY_DOCUMENT_NUMBER)
				stmt = getSelectByPeriodIdOrderByNumberQuery();
			else if (orderBy == ORDER_BY_DOCUMENT_DATE)
				stmt = getSelectByPeriodIdOrderByDateQuery();
			else if (orderBy == ORDER_BY_ACCOUNT_NUMBER_AND_DOCUMENT_NUMBER)
				stmt = getSelectByPeriodIdOrderByAccountAndNumberQuery();
			else if (orderBy == ORDER_BY_ACCOUNT_NUMBER_AND_DOCUMENT_DATE)
				stmt = getSelectByPeriodIdOrderByAccountAndDateQuery();

			stmt.setInt(1, periodId);
			rs = stmt.executeQuery();

			while (rs.next()) {
				callback.process(createObject(rs));
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikki tietyn
	 * tilikauden viennit. Rivit järjestetään tositenumeron mukaan.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdOrderByNumberQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikki tietyn
	 * tilikauden viennit. Rivit järjestetään päivämäärän mukaan.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdOrderByDateQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikki tietyn
	 * tilikauden viennit. Rivit järjestetään tilinumeron ja tositenumeron mukaan.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdOrderByAccountAndNumberQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikki tietyn
	 * tilikauden viennit. Rivit järjestetään tilinumeron ja päivämäärän mukaan.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdOrderByAccountAndDateQuery() throws SQLException;

	public void getByPeriodIdAndAccountId(int periodId, int accountId, int orderBy,
			DTOCallback<Entry> callback) throws DataAccessException
	{
		ResultSet rs;

		try {
			PreparedStatement stmt;

			if (periodId < 0) {
				stmt = getSelectByAccountIdQuery();
				stmt.setInt(1, accountId);
			}
			else if (orderBy == ORDER_BY_DOCUMENT_NUMBER) {
				stmt = getSelectByPeriodIdAndAccountIdOrderByNumberQuery();
				stmt.setInt(1, periodId);
				stmt.setInt(2, accountId);
			}
			else {
				stmt = getSelectByPeriodIdAndAccountIdOrderByDateQuery();
				stmt.setInt(1, periodId);
				stmt.setInt(2, accountId);
			}

			rs = stmt.executeQuery();

			while (rs.next()) {
				callback.process(createObject(rs));
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan kaikkien tilikausien
	 * viennit, jotka kohdistuvat tiettyyn tiliin.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByAccountIdQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tietyn tilikauden
	 * viennit, jotka kohdistuvat tiettyyn tiliin. Rivit järjestetään päivämäärän mukaan.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndAccountIdOrderByDateQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tietyn tilikauden
	 * viennit, jotka kohdistuvat tiettyyn tiliin. Rivit järjestetään tositenumeron mukaan.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndAccountIdOrderByNumberQuery() throws SQLException;

	public void getByPeriodIdAndDate(int periodId, Date startDate,
			Date endDate, int startNumber, DTOCallback<Entry> callback) throws DataAccessException {

		ResultSet rs;

		try {
			PreparedStatement stmt;
			int index = 1;

			if (startNumber >= 0) {
				stmt = getSelectByPeriodIdAndDateAndNumberQuery();
				stmt.setInt(index++, periodId);
				stmt.setInt(4, startNumber);
			}
			else if (periodId > 0) {
				stmt = getSelectByPeriodIdAndDateQuery();
				stmt.setInt(index++, periodId);
			}
			else {
				stmt = getSelectByDateQuery();
			}

			stmt.setTimestamp(index++, new java.sql.Timestamp(startDate.getTime()));
			stmt.setTimestamp(index++, new java.sql.Timestamp(endDate.getTime()));
			rs = stmt.executeQuery();

			while (rs.next()) {
				callback.process(createObject(rs));
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	public void getByPeriodIdAndDate(int periodId, Date startDate,
			Date endDate, DTOCallback<Entry> callback) throws DataAccessException {
		getByPeriodIdAndDate(periodId, startDate, endDate, -1, callback);
	}

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan viennit
	 * tietyltä aikaväliltä
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByDateQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tietyn tilikauden
	 * viennit tietyltä aikaväliltä. Kyselyssä on kolme parametria:
	 * 1. tilikauden tunniste, 2. alkamispäivämäärä ja 3. päättymispäivämäärä.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndDateQuery() throws SQLException;

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan tietyn tilikauden
	 * viennit tietyltä aikaväliltä. Kyselyssä on neljä parametria:
	 * 1. tilikauden tunniste, 2. alkamispäivämäärä, 3. päättymispäivämäärä ja
	 * 4. tositenumerovälin alku.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndDateAndNumberQuery() throws SQLException;

	public void getByPeriodIdAndNumber(int periodId,
			int startNumber, int endNumber, DTOCallback<Entry> callback)
			throws DataAccessException
	{
		ResultSet rs;

		try {
			PreparedStatement stmt = getSelectByPeriodIdAndNumberQuery();
			stmt.setInt(1, periodId);
			stmt.setInt(2, startNumber);
			stmt.setInt(3, endNumber);
			rs = stmt.executeQuery();

			while (rs.next()) {
				callback.process(createObject(rs));
			}

			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Palauttaa SELECT-kyselyn, jonka avulla haetaan viennit
	 * tietyltä tositenumeroväliltä ja tietyltä tilikaudelta.
	 *
	 * @return SELECT-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getSelectByPeriodIdAndNumberQuery() throws SQLException;

	/**
	 * Tallentaa viennin tiedot tietokantaan.
	 *
	 * @param obj tallennettava vienti
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Entry obj) throws DataAccessException {
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
	 * Lisää viennin tiedot tietokantaan.
	 *
	 * @param obj tallennettava vienti
	 * @throws SQLException jos tallentaminen epäonnistuu
	 */
	protected void executeInsertQuery(Entry obj) throws SQLException {
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
	 * Palauttaa tietokantaan lisätyn viennin tunnisteen.
	 *
	 * @return viennin tunniste
	 * @throws SQLException jos tunnisteen hakeminen epäonnistuu
	 */
	protected abstract int getGeneratedKey() throws SQLException;

	/**
	 * Päivittää viennin tiedot tietokantaan.
	 *
	 * @param obj tallennettava tosite
	 * @throws SQLException jos kyselyn suorittaminen epäonnistuu
	 */
	protected void executeUpdateQuery(Entry obj) throws SQLException {
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
	 * Poistaa viennin tiedot tietokannasta.
	 *
	 * @param entryId poistettavan viennin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int entryId) throws DataAccessException {
		try {
			PreparedStatement stmt = getDeleteQuery();
			stmt.setInt(1, entryId);
			stmt.executeUpdate();
			stmt.close();
		}
		catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Palauttaa DELETE-kyselyn, jonka avulla poistetaan rivi. Kyselyssä
	 * on yksi parametri, joka on viennin tunniste.
	 *
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteQuery() throws SQLException;

	/**
	 * Poistaa tietyn tilikauden kaikki viennit tietokannasta.
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
	 * tietyn tilikauden viennit.
	 *
	 * @return DELETE-kysely
	 * @throws SQLException jos kyselyn luominen epäonnistuu
	 */
	protected abstract PreparedStatement getDeleteByPeriodIdQuery() throws SQLException;

	/**
	 * Lukee <code>ResultSetistä</code> rivin kentät ja
	 * luo <code>Entry</code>-olion.
	 *
	 * @param rs <code>ResultSet</code>-olio, josta kentät luetaan.
	 * @return luotu olio
	 * @throws SQLException jos kenttien lukeminen epäonnistuu
	 */
	protected Entry createObject(ResultSet rs) throws SQLException {
		Entry obj = new Entry();
		obj.setId(rs.getInt(1));
		obj.setDocumentId(rs.getInt(2));
		obj.setAccountId(rs.getInt(3));
		obj.setDebit(rs.getBoolean(4));
		obj.setAmount(rs.getBigDecimal(5));
		obj.setDescription(rs.getString(6));
		obj.setRowNumber(rs.getInt(7));
		obj.setFlags(rs.getInt(8));
		return obj;
	}

	/**
	 * Asettaa valmistellun kyselyn parametreiksi olion tiedot.
	 *
	 * @param stmt kysely, johon tiedot asetetaan
	 * @param obj olio, josta tiedot luetaan
	 * @throws SQLException jos tietojen asettaminen epäonnistuu
	 */
	protected void setValuesToStatement(PreparedStatement stmt, Entry obj)
		throws SQLException
	{
		stmt.setInt(1, obj.getDocumentId());
		stmt.setInt(2, obj.getAccountId());
		stmt.setBoolean(3, obj.isDebit());
		stmt.setBigDecimal(4, obj.getAmount());
		stmt.setString(5, obj.getDescription());
		stmt.setInt(6, obj.getRowNumber());
		stmt.setInt(7, obj.getFlags());
	}
}
