package kirjanpito.reports;

import java.io.IOException;

import kirjanpito.db.DataAccessException;
import kirjanpito.util.CSVWriter;

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
	
	/**
	 * Kirjoittaa tulosteen tiedot CSV-tiedostoon.
	 * 
	 * @param writer CSV-tiedoston kirjoittaja
	 * @throws IOException jos tiedoston kirjoitus epäonnistuu
	 */
	public void writeCSV(CSVWriter writer) throws IOException;
}
