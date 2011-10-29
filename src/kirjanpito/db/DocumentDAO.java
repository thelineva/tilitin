package kirjanpito.db;

import java.util.Date;
import java.util.List;

/**
 * <code>DocumentDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tositteita sekä hakea olemassa olevien tositteiden
 * tietoja.
 * 
 * @author Tommi Helineva
 */
public interface DocumentDAO {
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
		throws DataAccessException;
	
	/**
	 * Tallentaa tositteen tiedot tietokantaan.
	 * 
	 * @param document tallennettava tosite
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Document document) throws DataAccessException;
	
	/**
	 * Poistaa tositteen tiedot tietokannasta.
	 * 
	 * @param documentId poistettavan tositteen tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int documentId) throws DataAccessException;
	
	/**
	 * Poistaa tietyn tilikauden kaikki tositteet tietokannasta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void deleteByPeriodId(int periodId) throws DataAccessException;
	
	/**
	 * Muuttaa tositenumeroita välillä <code>startNumber</code>..<code>endNumber</code>.
	 *
	 * @param periodId tilikauden tunniste
	 * @param startNumber välin alku
	 * @param endNumber välin loppu
	 * @param shift muutos
	 * @throws DataAccessException jos tositenumeroiden muuttaminen epäonnistuu
	 */
	public void shiftNumbers(int periodId, int startNumber, int endNumber, int shift) throws DataAccessException;
	
	/**
	 * Hakee tietokannasta kaikki tietyn tilikauden tositteet.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param numberOffset tositenumero >= <code>numberOffset</code>
	 * @return tositteet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Document> getByPeriodId(int periodId, int numberOffset)
		throws DataAccessException;
	
	/**
	 * Hakee tietokannasta tietyn tilikauden tositteiden lukumäärän.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param numberOffset tositenumero >= <code>numberOffset</code>
	 * @return tositteiden lukumäärä
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int getCountByPeriodId(int periodId, int numberOffset)
		throws DataAccessException;
	
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
		throws DataAccessException;
	
	/**
	 * Hakee tietokannasta tositteiden lukumäärän numeroväliltä
	 * <code>startNumber</code>..<code>endNumber</code>
	 * tietyltä tilikaudelta.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param startNumber tositenumerovälin alku
	 * @param endNumber tositenumerovälin loppu
	 * @return tositteiden lukumäärä
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int getCountByPeriodIdAndNumber(int periodId,
			int startNumber, int endNumber) throws DataAccessException;
	
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
			throws DataAccessException;
	
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
			throws DataAccessException;
	
	/**
	 * Etsii tositteita hakusanalla <code>q</code> ja palauttaa
	 * tulosten lukumäärän.
	 * 
	 * @param periodId tilikauden tunniste
	 * @param q hakusana
	 * @return tulosten lukumäärä
	 */
	public int getCountByPeriodIdAndPhrase(int periodId, String q)
			throws DataAccessException;
	
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
	public List<Document> getByPeriodIdAndPhrase(int periodId, String q,
			int offset, int limit) throws DataAccessException;
	
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
			Date startDate, Date endDate) throws DataAccessException;
}
