package kirjanpito.db;

/**
 * <code>ReportStructureDAO</code>:n avulla voidaan tallentaa ja
 * hakea tulosteiden rakennetietoja.
 * 
 * @author Tommi Helineva
 */
public interface ReportStructureDAO {
	/**
	 * Tallentaa tulosteen rakennetiedot tietokantaan.
	 * 
	 * @param structure tallennettavat tiedot
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save(ReportStructure structure) throws DataAccessException;
	
	/**
	 * Hakee tulosteen rakennetiedot tunnisteen perusteella.
	 * 
	 * @param id tulosteen tunniste
	 * @return tulosteen rakennetiedot
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public ReportStructure getById(String id) throws DataAccessException;
}
