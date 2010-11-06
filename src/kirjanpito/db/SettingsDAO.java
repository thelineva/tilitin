package kirjanpito.db;

/**
 * <code>SettingsDAO</code>:n avulla voidaan hakea ja tallentaa
 * asetukset.
 * 
 * @author Tommi Helineva
 */
public interface SettingsDAO {
	/**
	 * Tallentaa asetukset tietokantaan.
	 * 
	 * @param settings asetukset
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(Settings settings) throws DataAccessException;
	
	/**
	 * Hakee tietokannasta asetukset.
	 * 
	 * @return asetukset
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public Settings get() throws DataAccessException;
}
