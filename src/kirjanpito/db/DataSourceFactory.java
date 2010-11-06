package kirjanpito.db;

public class DataSourceFactory {
	private DataSourceFactory() {
	}
	
	public static DataSource create(String url, String username, String password)
		throws DataAccessException {
		
		String[] prefixes = {
			"jdbc:sqlite:",
			"jdbc:postgresql:",
			"jdbc:mysql:"
		};
		
		String[] classNames = {
			"kirjanpito.db.sqlite.SQLiteDataSource",
			"kirjanpito.db.postgresql.PSQLDataSource",
			"kirjanpito.db.mysql.MySQLDataSource"
		};
		
		int index = 0;
		
		for (String prefix : prefixes) {
			if (url.startsWith(prefix)) {
				DataSource dataSource;
				
				try {
					dataSource = (DataSource)Class.forName(
							classNames[index]).newInstance();
				}
				catch (Exception e) {
					throw new DataAccessException(
							"Ilmentymän luonti luokasta " + classNames[index] +
							" epäonnistui", e);
				}
				
				dataSource.open(url, username, password);
				return dataSource;
			}
			
			index++;
		}
		
		throw new DataAccessException("Virheellinen tietokantapalvelimen osoite");
	}
}
