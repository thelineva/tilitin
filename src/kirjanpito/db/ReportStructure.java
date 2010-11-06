package kirjanpito.db;

/**
 * Sisältää tulosteen rakennemäärittelyt.
 * 
 * @author Tommi Helineva
 */
public class ReportStructure {
	private String id;
	private String data;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}
}
