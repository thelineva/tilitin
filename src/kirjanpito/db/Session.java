package kirjanpito.db;

/**
 * Tietokantaistunto.
 * 
 * @author Tommi Helineva
 */
public interface Session {
	/**
	 * Päättää transaktion, ja tehdyt muutokset hyväksytään.
	 * 
	 * @throws DataAccessException jos transaktion päättäminen epäonnistuu
	 */
	public void commit() throws DataAccessException;
	
	/**
	 * Päättää transaktion, ja tehdyt muutokset perutaan.
	 * 
	 * @throws DataAccessException jos transaktion päättäminen epäonnistuu
	 */
	public void rollback() throws DataAccessException;
	
	/**
	 * Päättää istunnon.
	 */
	public void close();
}
