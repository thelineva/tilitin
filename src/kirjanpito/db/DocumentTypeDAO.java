package kirjanpito.db;

import java.util.List;

/**
 * <code>DocumentTypeDAO</code>:n avulla voidaan lisätä, muokata ja
 * poistaa tositelajeja sekä hakea olemassa olevien tositelajien
 * tietoja.
 * 
 * @author Tommi Helineva
 */
public interface DocumentTypeDAO {
	/**
	 * Tallentaa vientimallin tiedot tietokantaan.
	 * 
	 * @param template tallennettava vientimalli
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(DocumentType documentType) throws DataAccessException;
	
	/**
	 * Poistaa tositelajin tiedot tietokannasta.
	 * 
	 * @param typeId poistettavan tositelajin tunniste
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void delete(int typeId) throws DataAccessException;
	
	/**
	 * Hakee tietokannasta kaikkien tositelajien tiedot
	 * numerojärjestyksessä.
	 * 
	 * @return tositelajit
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public List<DocumentType> getAll() throws DataAccessException;
}
