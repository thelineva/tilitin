package kirjanpito.db;

import java.util.List;

/**
 * <code>EntryTemplateDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa vientimalleja sekä hakea olemassa olevien vientimallien
 * tietoja.
 * 
 * @author Tommi Helineva
 */
public interface EntryTemplateDAO {
	/**
	 * Tallentaa vientimallin tiedot tietokantaan.
	 * 
	 * @param template tallennettava vientimalli
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(EntryTemplate template) throws DataAccessException;
	
	/**
	 * Poistaa vientimallin tiedot tietokannasta.
	 * 
	 * @param accountId poistettavan vientimallin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int templateId) throws DataAccessException;
	
	/**
	 * Hakee tietokannasta kaikkien vientimallien tiedot
	 * numerojärjestyksessä.
	 * 
	 * @return vientimallit
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<EntryTemplate> getAll() throws DataAccessException;
}
