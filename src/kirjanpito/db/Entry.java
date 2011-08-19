package kirjanpito.db;

import java.math.BigDecimal;

/**
 * Sisältää viennin tiedot.
 * 
 * @author Tommi Helineva
 */
public class Entry {
	private int id;
	private int documentId;
	private int accountId;
	private boolean debit;
	private BigDecimal amount;
	private String description;
	private int rowNumber;
	private int flags;
	
	/**
	 * Palauttaa viennin tunnisteen.
	 * 
	 * @return viennin tunniste
	 */
	public int getId() {
		return id;
	}

	/**
	 * Asettaa viennin tunnisteen.
	 * 
	 * @param id viennin tunniste
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Palauttaa tilin tunnisteen.
	 * 
	 * @return tilin tunniste
	 */
	public int getAccountId() {
		return accountId;
	}
	
	/**
	 * Asettaa tilin tunnisteen.
	 * 
	 * @param accountId tilin tunniste
	 */
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	
	/**
	 * Palauttaa <code>true</code>, jos vienti on kirjattu tilin
	 * debet-puolelle.
	 * 
	 * @return <code>true</code>, jos vienti on kirjattu debet-puolelle
	 */
	public boolean isDebit() {
		return debit;
	}
	
	/**
	 * Asetetetaan <code>true</code>, jos vienti on kirjattu
	 * debet-puolelle.
	 * 
	 * @param debit <code>true</code>, jos vienti on kirjattu debet-puolelle
	 */
	public void setDebit(boolean debit) {
		this.debit = debit;
	}
	
	/**
	 * Palauttaa tositteen tunnisteen.
	 * 
	 * @return tositteen tunniste
	 */
	public int getDocumentId() {
		return documentId;
	}
	
	/**
	 * Asettaa tositteen tunnisteen.
	 * 
	 * @param documentId tositteen tunniste
	 */
	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}
	
	/**
	 * Palauttaa viennin rahamäärän.
	 * 
	 * @return viennin rahamäärä
	 */
	public BigDecimal getAmount() {
		return amount;
	}
	
	/**
	 * Asettaa viennin rahamäärän.
	 * 
	 * @param value viennin rahamäärä
	 */
	public void setAmount(BigDecimal value) {
		this.amount = value;
	}

	/**
	 * Palauttaa viennin selitteen.
	 * 
	 * @return selite
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Asettaa viennin selitteen.
	 * 
	 * @param description selite
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Palauttaa viennin järjestysnumeron tositteessa.
	 * 
	 * @return järjestysnumero
	 */
	public int getRowNumber() {
		return rowNumber;
	}

	/**
	 * Asettaa viennin järjestysnumeron tositteessa.
	 * 
	 * @param rowNumber järjestysnumero
	 */
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}
	
	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public boolean getFlag(int index) {
		return (flags & (1 << index)) > 0;
	}

	public void setFlag(int index, boolean value) {
		if (value) {
			flags |= 1 << index;
		}
		else {
			flags &= ~(1 << index);
		}
	}

	/**
	 * Luo oliosta kopion.
	 * 
	 * @return kopio
	 */
	public Entry clone() {
		Entry entry = new Entry();
		entry.id = id;
		entry.documentId = documentId;
		entry.accountId = accountId;
		entry.debit = debit;
		entry.amount = amount;
		entry.description = description;
		entry.rowNumber = rowNumber;
		return entry;
	}
}
