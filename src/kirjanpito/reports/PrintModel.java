package kirjanpito.reports;

import kirjanpito.db.DataAccessException;

/**
 * Tulosteen malli.
 * 
 * @author Tommi Helineva
 */
public interface PrintModel {
	/**
	 * Hakee mallin tarvitsemat tiedot tietokannasta.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void run() throws DataAccessException;
}
