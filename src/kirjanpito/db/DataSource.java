package kirjanpito.db;

/**
 * Ylläpitää tietokantayhteyttä.
 * 
 * @author Tommi Helineva
 */
public interface DataSource {
	/**
	 * Avaa tietokantayhteyden.
	 * 
	 * @param url JDBC-URL
	 * @param username käyttäjänimi
	 * @param password salasana
	 * @throws DataAccessException jos tietokantayhteyden avaaminen epäonnistuu
	 */
	public void open(String url, String username, String password)
		throws DataAccessException;
	
	/**
	 * Sulkee tietokantayhteyden.
	 */
	public void close();
	
	/**
	 * Varmuuskopioi tietokannan.
	 */
	public void backup() throws DataAccessException;
	
	/**
	 * Avaa uuden tietokantaistunnon. Yhteysaltaasta (connection pool)
	 * varataan tietokantayhteys istunnon käyttöön. Yhteys on vapautettava
	 * lopuksi kutsumalla <code>Session</code>-olion <code>close()</code>-metodia.
	 * 
	 * @return tietokantaistunto
	 * @throws DataAccessException jos istunnon luominen epäonnistuu
	 */
	public Session openSession() throws DataAccessException;
	
	/**
	 * Palauttaa <code>AccountDAO</code>-olion, jonka avulla voidaan lisätä,
	 * muokata ja poistaa tilitietoja sekä hakea olemassa olevien tilien
	 * tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>AccountDAO</code>-olio
	 */
	public AccountDAO getAccountDAO(Session session);
	
	/**
	 * Palauttaa <code>COAHeadingDAO</code>-olion, jonka avulla voidaan
	 * lisätä, muokata ja poistaa tilikartan otsikoita sekä hakea
	 * olemassa olevien otsikoiden tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>COAHeadingDAO</code>-olio
	 */
	public COAHeadingDAO getCOAHeadingDAO(Session session);
	
	/**
	 * Palauttaa <code>DocumentDAO</code>-olion, jonka avulla voidaan lisätä,
	 * muokata ja poistaa tositteita sekä hakea olemassa olevien
	 * tositteiden tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>DocumentDAO</code>-olio
	 */
	public DocumentDAO getDocumentDAO(Session session);
	
	/**
	 * Palauttaa <code>EntryDAO</code>-olion, jonka avulla voidaan lisätä,
	 * muokata ja poistaa vientejä sekä hakea olemassa olevien
	 * vientien tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>EntryDAO</code>-olio
	 */
	public EntryDAO getEntryDAO(Session session);
	
	/**
	 * Palauttaa <code>PeriodDAO</code>-olion, jonka avulla voidaan lisätä,
	 * muokata ja poistaa tilikausia sekä hakea olemassa olevien
	 * tilikausien tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>PeriodDAO</code>-olio
	 */
	public PeriodDAO getPeriodDAO(Session session);
	
	/**
	 * Palauttaa <code>SettingsDAO</code>-olion, jonka avulla voidaan hakea
	 * ja tallentaa asetukset.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>SettingsDAO</code>-olio
	 */
	public SettingsDAO getSettingsDAO(Session session);
	
	/**
	 * Palauttaa <code>ReportStructureDAO</code>-olion, jonka avulla voidaan
	 * hakea ja tallentaa tulosteiden rakennemäärittelyt.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>ReportStructureDAO</code>-olio
	 */
	public ReportStructureDAO getReportStructureDAO(Session session);
	
	/**
	 * Palauttaa <code>EntryTemplateDAO</code>-olion, jonka avulla voidaan lisätä,
	 * muokata ja poistaa vientimalleja sekä hakea olemassa olevien
	 * vientimallien tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>EntryTemplateDAO</code>-olio
	 */
	public EntryTemplateDAO getEntryTemplateDAO(Session session);
	
	/**
	 * Palauttaa <code>DocumentTypeDAO</code>-olion, jonka avulla voidaan lisätä,
	 * muokata ja poistaa tositelajeja sekä hakea olemassa olevien
	 * tositelajien tietoja.
	 * 
	 * @param session tietokantaistunto
	 * @return <code>DocumentTypeDAO</code>-olio
	 */
	public DocumentTypeDAO getDocumentTypeDAO(Session session);
}
