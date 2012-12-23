package kirjanpito.db;

import java.math.BigDecimal;

/**
 * Sisältää tilin tiedot.
 *
 * @author Tommi Helineva
 */
public class Account implements Comparable<Account> {
	private int id;
	private String number;
	private String name;
	private int type;
	private int vatCode;
	private BigDecimal vatRate;
	private int vatAccount1Id;
	private int vatAccount2Id;
	private int flags;

	/**
	 * Vastaavaa
	 */
	public static final int TYPE_ASSET = 0;

	/**
	 * Vastattavaa
	 */
	public static final int TYPE_LIABILITY = 1;

	/**
	 * Oma pääoma
	 */
	public static final int TYPE_EQUITY = 2;

	/**
	 * Tulot
	 */
	public static final int TYPE_REVENUE = 3;

	/**
	 * Menot
	 */
	public static final int TYPE_EXPENSE = 4;

	/**
	 * Edellisten tilikausien voitto
	 */
	public static final int TYPE_PROFIT_PREV = 5;

	/**
	 * Tilikauden voitto
	 */
	public static final int TYPE_PROFIT = 6;

	public Account() {
		vatRate = BigDecimal.ZERO;
	}

	/**
	 * Palauttaa tilin tunnisteen.
	 *
	 * @return tilin tunniste
	 */
	public int getId() {
		return id;
	}

	/**
	 * Asettaa tilin tunnisteen.
	 *
	 * @param id tilin tunniste
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Palauttaa tilin nimen.
	 *
	 * @return tilin nimi
	 */
	public String getName() {
		return name;
	}

	/**
	 * Asettaa tilin nimen.
	 *
	 * @param name tilin nimi
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Palauttaa tilinumeron.
	 *
	 * @return tilinumero
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * Asettaa tilinumeron arvon.
	 *
	 * @param number tilinumero
	 */
	public void setNumber(String number) {
		this.number = number;
	}

	/**
	 * Palauttaa tilin tyypin. Arvo on jokin <code>TYPE_*</code>-vakioista.
	 *
	 * @return tilin tyyppi
	 */
	public int getType() {
		return type;
	}

	/**
	 * Asettaa tilin tyypin. Arvo on jokin <code>TYPE_*</code>-vakioista.
	 *
	 * @param type tilin tyyppi
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Palauttaa ALV-koodin.
	 *
	 * @return alv-koodi
	 */
	public int getVatCode() {
		return vatCode;
	}

	/**
	 * Asettaa ALV-koodin.
	 *
	 * @param vatCode alv-koodi
	 */
	public void setVatCode(int vatCode) {
		this.vatCode = vatCode;
	}

	/**
	 * Palauttaa ALV-prosentin.
	 *
	 * @return alv-prosentti
	 */
	public BigDecimal getVatRate() {
		return vatRate;
	}

	/**
	 * Asettaa ALV-prosentin.
	 *
	 * @param vatRate alv-prosentti
	 */
	public void setVatRate(BigDecimal vatRate) {
		this.vatRate = vatRate;
	}

	/**
	 * Palauttaa ALV-vastatilin (1) tunnisteen.
	 *
	 * @return alv-vastatilin tunniste
	 */
	public int getVatAccount1Id() {
		return vatAccount1Id;
	}

	/**
	 * Asettaa ALV-vastatilin (1) tunnisteen.
	 *
	 * @param vatAccountId alv-vastatilin tunniste
	 */
	public void setVatAccount1Id(int vatAccountId) {
		this.vatAccount1Id = vatAccountId;
	}

	/**
	 * Palauttaa ALV-vastatilin (2) tunnisteen.
	 *
	 * @return alv-vastatilin tunniste
	 */
	public int getVatAccount2Id() {
		return vatAccount2Id;
	}

	/**
	 * Asettaa ALV-vastatilin (2) tunnisteen.
	 *
	 * @param vatAccountId alv-vastatilin tunniste
	 */
	public void setVatAccount2Id(int vatAccountId) {
		this.vatAccount2Id = vatAccountId;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Vertaa tämän tilin numeroa toisen tilin numeroon.
	 *
	 * @return pienempi kuin 0, jos tämän tilin numero aakkosjärjestyksessä
	 * aikaisemmin; suurempi kuin 0, jos tämän tilin numero on aakkosjärjestyksessä
	 * myöhemmin
	 */
	public int compareTo(Account other) {
		return number.compareTo(other.number);
	}
}
