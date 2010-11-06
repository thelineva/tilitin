package kirjanpito.db;

import java.util.List;

/**
 * <code>COAHeadingDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilikartan otsikoita sekä hakea olemassa olevien otsikoiden
 * tietoja.
 * 
 * @author Tommi Helineva
 */
public interface COAHeadingDAO {
	/**
	 * Tallentaa tilikartan otsikon tiedot tietokantaan.
	 * 
	 * @param heading tallennettava otsikko
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(COAHeading heading) throws DataAccessException;

	/**
	 * Poistaa tilikartan otsikon tiedot tietokannasta.
	 * 
	 * @param headingId poistettavan otsikon tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int headingId) throws DataAccessException;
	
	/**
	 * Hakee kaikkien otsikoiden tiedot tietokannasta
	 * numerojärjestyksessä.
	 * 
	 * @return otsikot
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<COAHeading> getAll() throws DataAccessException;
}
