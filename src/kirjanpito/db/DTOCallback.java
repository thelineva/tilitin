package kirjanpito.db;

public interface DTOCallback<T> {
	public void process(T obj);
}
