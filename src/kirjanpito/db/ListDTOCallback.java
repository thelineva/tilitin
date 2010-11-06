package kirjanpito.db;

import java.util.List;

public class ListDTOCallback<T> implements DTOCallback<T> {
	private List<T> list;
	
	public ListDTOCallback(List<T> list) {
		this.list = list;
	}

	public void process(T obj) {
		list.add(obj);
	}
}
