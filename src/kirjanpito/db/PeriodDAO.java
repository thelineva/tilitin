package kirjanpito.db;

import java.util.List;

/**
 * <code>PeriodDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tilikausia sekä hakea olemassa olevien tilikausien
 * tietoja.
 * 
 * @author Tommi Helineva
 */
public interface PeriodDAO {
	/**
	 * Tallentaa tilikauden tiedot tietokantaan.
	 * 
	 * @param period tilikausi
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Period period) throws DataAccessException;
	
	/**
	 * Poistaa tilikauden tiedot tietokannasta.
	 * 
	 * @param periodId poistettavan tilikauden tunnus
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void delete(int periodId) throws DataAccessException;
	
	/**
	 * Hakee tietokannasta kaikkien tilikausien tiedot
	 * aikajärjestyksessä.
	 * 
	 * @return tilikaudet
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<Period> getAll() throws DataAccessException;
	
	/**
	 * Hakee tietokannasta nykyisen tilikauden tiedot.
	 *  
	 * @return tilikausi
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public Period getCurrent() throws DataAccessException;
}
