package kirjanpito.db;

public class CountDTOCallback<T> implements DTOCallback<T> {
	private int count;
	
	public int getCount() {
		return count;
	}
	
	public void process(T obj) {
		count++;
	}
}
