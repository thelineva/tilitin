package kirjanpito.db;

/**
 * Sisältää tilikartan väliotsikon tiedot.
 * 
 * @author Tommi Helineva
 */
public class COAHeading implements Comparable<COAHeading> {
	private int id;
	private String number;
	private String text;
	private int level;
	
	/**
	 * Palauttaa otsikon tunnisteen.
	 * 
	 * @return otsikon tunniste
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Asettaa otsikon tunnisteen.
	 * 
	 * @param id otsikon tunniste
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Palauttaa otsikon tekstin.
	 * 
	 * @return otsikon teksti
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Asettaa otsikon tekstin.
	 * 
	 * @param text otsikon teksti
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Palauttaa otsikon numeron.
	 * 
	 * @return otsikon numero
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * Asettaa otsikon numeron.
	 * 
	 * @param number otsikon numero
	 */
	public void setNumber(String number) {
		this.number = number;
	}

	/**
	 * Palauttaa rivin sisennystason tilikartassa.
	 * 
	 * @return rivin sisennystaso
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Asettaa rivin sisennystason tilikartassa.
	 * 
	 * @param level rivin sisennystaso
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * Vertaa tämän otsikon numeroa toisen otsikon numeroon.
	 * 
	 * @return pienempi kuin 0, jos tämän otsikon numero aakkosjärjestyksessä
	 * aikaisemmin; suurempi kuin 0, jos tämän otsikon numero on aakkosjärjestyksessä
	 * myöhemmin
	 */
	public int compareTo(COAHeading other) {
		int result = number.compareTo(other.number);
		return (result == 0) ? level - other.level : result;
	}
	
	/**
	 * Palauttaa uuden olion, joka sisältää samat tiedot.
	 * 
	 * @return tili
	 */
	public COAHeading copy() {
		COAHeading heading = new COAHeading();
		heading.id = id;
		heading.number = number;
		heading.text = text;
		heading.level = level;
		return heading;
	}
}
