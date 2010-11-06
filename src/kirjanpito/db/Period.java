package kirjanpito.db;

import java.util.Date;

/**
 * Sisältää tilikauden tiedot.
 * 
 * @author Tommi Helineva
 */
public class Period {
	private int id;
	private Date startDate;
	private Date endDate;
	private boolean locked;
	
	/**
	 * Palauttaa tilikauden tunnisteen.
	 * 
	 * @return tilikauden tunniste
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Asettaa tilikauden tunnisteen.
	 * 
	 * @param id tilikauden tunniste
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Palauttaa tilikauden alkamispäivämäärän.
	 * 
	 * @return tilikauden alkamispäivämäärä.
	 */
	public Date getStartDate() {
		return startDate;
	}
	
	/**
	 * Asettaa tilikauden alkamispäivämäärän.
	 * 
	 * @param startDate tilikauden alkamispäivämäärä
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	/**
	 * Palauttaa tilikauden päättymispäivämäärän.
	 * 
	 * @return päättymispäivämäärä
	 */
	public Date getEndDate() {
		return endDate;
	}
	
	/**
	 * Asettaa tilikauden päättymispäivämäärän.
	 * 
	 * @param endDate päättymispäivämäärä
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * Palauttaa <code>true</code>, jos tilikauden tiedot
	 * ovat lukittu.
	 * 
	 * @return <code>true</code>, jos tilikauden tiedot ovat lukittu
	 */
	public boolean isLocked() {
		return locked;
	}
	
	/**
	 * Asetetaan <code>true</code>, jos tilikauden tiedot
	 * ovat lukittu.
	 * 
	 * @param locked <code>true</code>, jos tilikauden tiedot ovat lukittu
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
