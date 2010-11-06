package kirjanpito.db;

/**
 * Poikkeus, joka sisältää tietoa tietokantavirheestä.
 * 
 * @author Tommi Helineva
 */
public class DataAccessException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Luo uuden poikkeuksen.
	 */
	public DataAccessException() {
		super();
	}

	/**
	 * Luo uuden poikkeuksen.
	 * 
	 * @param message virheilmoitus
	 * @param cause poikkeuksen aiheuttaja
	 */
	public DataAccessException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Luo uuden poikkeuksen.
	 * 
	 * @param message virheilmoitus
	 */
	public DataAccessException(String message) {
		super(message);
	}

	/**
	 * Luo uuden poikkeuksen.
	 * 
	 * @param cause poikkeuksen aiheuttaja
	 */
	public DataAccessException(Throwable cause) {
		super(cause);
	}
}
