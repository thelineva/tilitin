package kirjanpito.db;

import java.util.Date;
import java.util.List;

/**
 * <code>EntryDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa vientejä sekä hakea olemassa olevien vientien
 * tietoja.
 *
 * @author Tommi Helineva
 */
public interface EntryDAO {
	/**
	 * Järjestetään tositenumeron mukaan.
	 */
	public static final int ORDER_BY_DOCUMENT_NUMBER = 1;

	/**
	 * Järjestetään päivämäärän mukaan.
	 */
	public static final int ORDER_BY_DOCUMENT_DATE = 2;

	/**
	 * Järjestetään tilinumeron ja tositenumeron mukaan.
	 */
	public static final int ORDER_BY_ACCOUNT_NUMBER_AND_DOCUMENT_NUMBER = 3;

	/**
	 * Järjestetään tilinumeron ja päivämäärän mukaan.
	 */
	public static final int ORDER_BY_ACCOUNT_NUMBER_AND_DOCUMENT_DATE = 4;

	/**
	 * Tallentaa viennin tiedot tietokantaan.
	 *
	 * @param entry tallennettava vienti
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Entry entry) throws DataAccessException;

	/**
	 * Poistaa viennin tiedot tietokannasta.
	 *
	 * @param entryId poistettavan viennin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int entryId) throws DataAccessException;

	/**
	 * Poistaa tietyn tilikauden kaikki viennit tietokannasta.
	 *
	 * @param periodId tilikauden tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void deleteByPeriodId(int periodId) throws DataAccessException;

	/**
	 * Hakee tietokannasta tiettyyn tositteeseen kuuluvat
	 * viennit.
	 *
	 * @param documentId tositteen tunniste
	 * @return viennit
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Entry> getByDocumentId(int documentId)
		throws DataAccessException;

	/**
	 * Hakee tietokannasta tiettyihin tositteisiin kuuluvat
	 * viennit.
	 *
	 * @param documentIds tositteiden tunnisteet
	 * @param callback callback
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void getByDocuments(List<Document> documents, DTOCallback<Entry> callback)
		throws DataAccessException;

	/**
	 * Hakee tietokannasta kaikki tietyn tilikauden viennit.
	 *
	 * @param periodId tilikauden tunniste
	 * @param orderBy <code>ORDER_BY_DOCUMENT_NUMBER</code> tai
	 * <code>ORDER_BY_ACCOUNT_NUMBER</code>
	 * @param callback callback
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void getByPeriodId(int periodId, int orderBy, DTOCallback<Entry> callback)
		throws DataAccessException;

	/**
	 * Hakee tietokannasta tietyn tilikauden viennit, jotka
	 * kohdistuvat tiettyyn tiliin. Viennit järjestetään päivämäärän mukaan.
	 *
	 * @param periodId tilikauden tunniste
	 * @param accountId tilin tunniste
	 * @param orderBy <code>ORDER_BY_DOCUMENT_NUMBER</code> tai
	 * <code>ORDER_BY_DOCUMENT_DATE</code>
	 * @param callback callback
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void getByPeriodIdAndAccountId(int periodId,
			int accountId, int orderBy, DTOCallback<Entry> callback) throws DataAccessException;

	/**
	 * Hakee tietokannasta tietyn tilikauden viennit tietyltä
	 * aikaväliltä.
	 *
	 * @param periodId tilikauden tunniste
	 * @param startDate alkamispäivämäärä
	 * @param endDate päättymispäivämäärä
	 * @param callback callback
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void getByPeriodIdAndDate(int periodId,
			Date startDate, Date endDate, DTOCallback<Entry> callback)
			throws DataAccessException;

	/**
	 * Hakee tietokannasta tietyn tilikauden viennit tietyltä
	 * aikaväliltä.
	 *
	 * @param periodId tilikauden tunniste
	 * @param startDate alkamispäivämäärä
	 * @param endDate päättymispäivämäärä
	 * @param startNumber tositenumerovälin alku
	 * @param callback callback
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void getByPeriodIdAndDate(int periodId,
			Date startDate, Date endDate, int startNumber,
			DTOCallback<Entry> callback)
			throws DataAccessException;

	/**
	 * Hakee tietokannasta viennit tositenumeroväliltä
	 * <code>startNumber</code>..<code>endNumber</code>
	 * tietyltä tilikaudelta.
	 *
	 * @param periodId tilikauden tunniste
	 * @param startNumber tositenumerovälin alku
	 * @param endNumber tositenumerovälin loppu
	 * @param callback callback
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void getByPeriodIdAndNumber(int periodId,
			int startNumber, int endNumber, DTOCallback<Entry> callback)
			throws DataAccessException;
}
