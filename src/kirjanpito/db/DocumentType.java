package kirjanpito.db;

/**
 * Sisältää tositelajin tiedot.
 * 
 * @author Tommi Helineva
 */
public class DocumentType implements Comparable<DocumentType> {
	private int id;
	private int number;
	private String name;
	private int numberStart;
	private int numberEnd;
	
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
	
	public int getNumberStart() {
		return numberStart;
	}
	
	public void setNumberStart(int numberStart) {
		this.numberStart = numberStart;
	}
	
	public int getNumberEnd() {
		return numberEnd;
	}
	
	public void setNumberEnd(int numberEnd) {
		this.numberEnd = numberEnd;
	}

	public int compareTo(DocumentType t) {
		return number - t.number;
	}
}
