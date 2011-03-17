package kirjanpito.db;

import java.util.Date;

/**
 * Sisältää tositteen tiedot.
 *
 * @author Tommi Helineva
 */
public class Document {
	private int id;
	private int number;
	private int periodId;
	private Date date;

	/**
	 * Palauttaa tositteen päivämäärän.
	 *
	 * @return tositteen päivämäärä
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Asettaa tositteen päivämäärän.
	 *
	 * @param date tositteen päivämäärä
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Palauttaa tositteen tunnisteen.
	 *
	 * @return tositteen tunniste
	 */
	public int getId() {
		return id;
	}

	/**
	 * Asettaa tositteen tunnisteen.
	 *
	 * @param id tositteen tunniste
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Palauttaa tositenumeron.
	 *
	 * @return tositenumero
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Asettaa tositenumeron.
	 *
	 * @param number tositenumero
	 */
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * Asettaa tilikauden tunnisteen.
	 *
	 * @return tilikauden tunniste
	 */
	public int getPeriodId() {
		return periodId;
	}

	/**
	 * Asettaa tilikauden tunnisteen.
	 *
	 * @param periodId tilikauden tunniste
	 */
	public void setPeriodId(int periodId) {
		this.periodId = periodId;
	}

	/**
	 * Kopioi tositteen tiedot toiseen olioon.
	 *
	 * @param o kohde
	 */
	public void copy(Document o) {
		o.id = id;
		o.number = number;
		o.periodId = periodId;
		o.date = date;
	}
}
