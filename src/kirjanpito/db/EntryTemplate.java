package kirjanpito.db;

import java.math.BigDecimal;

/**
 * Sisältää vientimallin tiedot.
 * 
 * @author Tommi Helineva
 */
public class EntryTemplate implements Comparable<EntryTemplate> {
	private int id;
	private int number;
	private String name;
	private int accountId;
	private boolean debit;
	private BigDecimal amount;
	private String description;
	private int rowNumber;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getNumber() {
		return number;
	}
	
	public void setNumber(int number) {
		this.number = number;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getAccountId() {
		return accountId;
	}
	
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	
	public boolean isDebit() {
		return debit;
	}
	
	public void setDebit(boolean debit) {
		this.debit = debit;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public int getRowNumber() {
		return rowNumber;
	}
	
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public int compareTo(EntryTemplate o) {
		if (number == o.number) {
			return rowNumber - o.rowNumber;
		}
		else {
			return number - o.number;
		}
	}
}
