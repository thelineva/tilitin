package kirjanpito.db;

import java.util.List;

/**
 * <code>AccountDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilitietoja sekä hakea olemassa olevien tilien tietoja.
 * 
 * @author Tommi Helineva
 */
public interface AccountDAO {
	/**
	 * Tallentaa tilin tiedot tietokantaan.
	 * 
	 * @param account tallennettava tili
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Account account) throws DataAccessException;
	
	/**
	 * Poistaa tilin tiedot tietokannasta.
	 * 
	 * @param accountId poistettavan tilin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int accountId) throws DataAccessException;
	
	/**
	 * Hakee tietokannasta kaikkien tilien tiedot
	 * numerojärjestyksessä.
	 * 
	 * @return tilit
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Account> getAll() throws DataAccessException;
}
