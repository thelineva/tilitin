package kirjanpito.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.db.CountDTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AppSettings;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.Registry;

/**
 * Malli tilikartan muokkausikkunalle.
 *
 * @author Tommi Helineva
 */
public class COAModel {
	private Registry registry;
	private List<Account> accounts;
	private List<COAHeading> coaHeadings;
	private List<DataSourceAction> dataSourceActions;
	private ChartOfAccounts coa;
	private Account defaultAccount;
	private boolean changed;
	private boolean nonFavouriteAccountsHidden;

	public COAModel(Registry registry) {
		this.registry = registry;
		coa = new ChartOfAccounts();
		dataSourceActions = new ArrayList<DataSourceAction>();
		updateChartOfAccounts(registry.getAccounts(),
				registry.getCOAHeadings());
		loadSettings();
	}

	/**
	 * Asettaa tilikartan sisällön.
	 *
	 * @param accounts tilikartan tilit
	 * @param headings tilikartan otsikot
	 */
	public void updateChartOfAccounts(List<Account> accounts,
			List<COAHeading> headings)
	{
		this.accounts = accounts;
		this.coaHeadings = headings;
		coa.set(accounts, headings);

		if (nonFavouriteAccountsHidden) {
			coa.filterNonFavouriteAccounts();
		}
	}

	/**
	 * Palauttaa tilikartan.
	 *
	 * @return tilikartta
	 */
	public ChartOfAccounts getChartOfAccounts() {
		return coa;
	}

	/**
	 * Hakee tilin tunnisteen perusteella.
	 *
	 * @param id haettava tunniste
	 * @return löytynyt tili tai <code>null</code>.
	 */
	public Account getAccountById(int id) {
		Account account = null;

		for (Account a : accounts) {
			if (a.getId() == id) {
				account = a;
				break;
			}
		}

		return account;
	}

	/**
	 * Hakee tilin numeron perusteella.
	 *
	 * @param number haettava numero
	 * @return löytynyt tili tai <code>null</code>.
	 */
	public Account getAccountByNumber(String number) {
		Account account = null;

		for (Account a : accounts) {
			if (a.getNumber().equals(number)) {
				account = a;
				break;
			}
		}

		return account;
	}

	/**
	 * Palauttaa <code>true</code>, jos tilikartan tietoja
	 * on muokattu.
	 *
	 * @return <code>true</code>, jos tilikartan tietoja
	 * on muokattu, mutta tietoja ei ole vielä tallennettu
	 * tietokantaan
	 */
	public boolean isChanged() {
		return changed;
	}

	/**
	 * Palauttaa oletustilin.
	 *
	 * @return oletustili
	 */
	public Account getDefaultAccount() {
		return defaultAccount;
	}

	/**
	 * Asettaa oletustilin.
	 *
	 * @param defaultAccount oletustili
	 */
	public void setDefaultAccount(Account defaultAccount) {
		this.defaultAccount = defaultAccount;
		this.changed = true;
	}

	/**
	 * Lisää tilin riville <code>index</code>.
	 *
	 * @param index rivinumero, johon uusi tili lisätään
	 * @return uuden tilin rivinumero
	 */
	public int addAccount(int index) {
		Account prev = nextAccount(index, -1);
		Account account = new Account();
		account.setName("");
		account.setVatAccount1Id(-1);
		account.setVatAccount2Id(-1);

		if (prev == null) {
			account.setNumber("1");
			account.setType(0);
		}
		else {
			account.setNumber(nextAccountNumber(prev.getNumber()));
			account.setType(prev.getType());
		}

		dataSourceActions.add(new SaveAccountAction(account));
		changed = true;
		accounts.add(account);
		Collections.sort(accounts);
		updateChartOfAccounts(accounts, coaHeadings);
		return coa.indexOfAccount(account);
	}

	/**
	 * Lisää otsikon riville <code>index</code>.
	 *
	 * @param index rivinumero, johon uusi otsikko lisätään
	 */
	public int addHeading(int index) {
		COAHeading heading = new COAHeading();
		heading.setText("");
		heading.setLevel(1);

		/* Uuden otsikon numeroksi asetetaan tilikartassa seuraavana
		 * olevan tilin numero. */
		Account account = nextAccount(index, 1);
		heading.setNumber((account == null) ? "1" : account.getNumber());

		dataSourceActions.add(new SaveCOAHeadingAction(heading));
		coaHeadings.add(heading);
		Collections.sort(coaHeadings);
		updateChartOfAccounts(accounts, coaHeadings);
		return coa.indexOfHeading(heading);
	}

	/**
	 * Merkitsee rivillä <code>index</code> olevan tilin
	 * tai otsikon muuttuneeksi.
	 *
	 * @param index rivinumero
	 * @param positionChanged jos <code>true</code>, myös rivien
	 * järjestys päivitetään
	 */
	public void updateRow(int index, boolean positionChanged) {
		DataSourceAction action;

		if (coa.getType(index) == ChartOfAccounts.TYPE_ACCOUNT) {
			Account account = coa.getAccount(index);
			action = new SaveAccountAction(account);

			if (positionChanged) {
				/* Järjestetään tilit uudelleen ja
				 * päivitetään tilikartta. */
				Collections.sort(accounts);
				updateChartOfAccounts(accounts, coaHeadings);
			}
		}
		else {
			COAHeading heading = coa.getHeading(index);
			action = new SaveCOAHeadingAction(heading);

			if (positionChanged) {
				/* Järjestetään otsikot uudelleen ja
				 * päivitetään tilikartta. */
				Collections.sort(coaHeadings);
				updateChartOfAccounts(accounts, coaHeadings);
			}
		}

		if (!dataSourceActions.contains(action)) {
			dataSourceActions.add(action);
			changed = true;
		}
	}

	/**
	 * Poistaa rivin <code>index</code>.
	 *
	 * @param index rivinumero
	 */
	public void removeRow(int index) {
		if (coa.getType(index) == ChartOfAccounts.TYPE_ACCOUNT) {
			Account account = coa.getAccount(index);
			dataSourceActions.add(new DeleteAccountAction(account));
			accounts.remove(account);
		}
		else {
			COAHeading heading = coa.getHeading(index);
			dataSourceActions.add(new DeleteCOAHeadingAction(heading));
			coaHeadings.remove(heading);
		}

		changed = true;
		updateChartOfAccounts(accounts, coaHeadings);
	}

	/**
	 * Siirtää otsikkoa ylös.
	 *
	 * @param index otsikon rivinumero
	 */
	public int moveHeading(int index, boolean down) {
		COAHeading heading = coa.getHeading(index);
		Account account = null;

		if (down) {
			boolean x = false;

			for (int i = index; i < coa.getSize(); i++) {
				if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
					if (!x) {
						x = true;
					}
					else {
						account = coa.getAccount(i);
						break;
					}
				}
			}
		}
		else {
			for (int i = index; i >= 0; i--) {
				if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
					account = coa.getAccount(i);
					break;
				}
			}
		}

		if (account == null) {
			return -1;
		}

		heading.setNumber(account.getNumber());
		Collections.sort(coaHeadings);
		updateChartOfAccounts(accounts, coaHeadings);
		SaveCOAHeadingAction action = new SaveCOAHeadingAction(heading);

		if (!dataSourceActions.contains(action)) {
			dataSourceActions.add(action);
			changed = true;
		}

		return coa.indexOfHeading(heading);
	}

	/**
	 * Suorittaa kaikki jonossa olevat tietokantatehtävät.
	 *
	 * @throws DataAccessException jos tehtävien suorittaminen epäonnistuu
	 */
	public void save() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();

			for (DataSourceAction action : dataSourceActions)
				action.execute(dataSource, sess);

			saveSettings(dataSource, sess);
			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		dataSourceActions.clear();
		changed = false;
		registry.updateChartOfAccounts();
		registry.fireChartOfAccountsChanged();
	}

	/**
	 * Ilmoittaa, piilotetaanko tilit, joita ei ole määritelty suosikkitileiksi.
	 *
	 * @return <code>true</code>, jos näytetään vain suosikkitilit
	 */
	public boolean isNonFavouriteAccountsHidden() {
		return nonFavouriteAccountsHidden;
	}

	/**
	 * Määrittää, piilotetaanko tilit, joita ei ole määritelty suosikkitileiksi.
	 *
	 * @param nonFavouriteAccountsHidden <code>true</code>, jos näytetään vain suosikkitilit
	 */
	public void setNonFavouriteAccountsHidden(boolean nonFavouriteAccountsHidden) {
		this.nonFavouriteAccountsHidden = nonFavouriteAccountsHidden;
		updateChartOfAccounts(accounts, coaHeadings);
	}

	/**
	 * Ilmoittaa, voiko tilin <code>account</code> poistaa tietokannasta.
	 * Tiliä ei voi poistaa, jos jossakin viennissä on viittaus tiliin.
	 *
	 * @param account tarkistettava tili
	 * @return 0, jos tilin voi poistaa; 1, jos jossakin viennissä on viittaus tiliin;
	 *         2, jos tili on määritelty ALV-vastatiliksi
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int canRemoveAccount(Account account) throws DataAccessException {
		int accountId = account.getId();

		for (Account a : accounts) {
			if (a.getVatAccount1Id() == accountId ||
					a.getVatAccount2Id() == accountId) {
				return 2;
			}
		}

		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			CountDTOCallback<Entry> callback = new CountDTOCallback<Entry>();
			dataSource.getEntryDAO(sess).getByPeriodIdAndAccountId(-1,
					accountId, EntryDAO.ORDER_BY_DOCUMENT_DATE, callback);
			return callback.getCount() == 0 ? 0 : 1;
		}
		finally {
			if (sess != null) sess.close();
		}
	}

	/**
	 * Palauttaa ensimmäisen tilin, joka on tilikartassa
	 * ennen riviä <code>position</code> (<code>dir == -1</code>) tai
	 * rivin <code>position</code> jälkeen (<code>dir == 1</code>).
	 *
	 * @param position rivinumero, josta hakua aletaan suorittaa
	 * @param dir -1, jos haetaan taaksepäin tai 1, jos haetaan eteenpäin
	 * @return tili tai <code>null</code>, jos tiliä ei löytynyt
	 */
	private Account nextAccount(int position, int dir) {
		Account account = null;
		int i = position;

		while (i > 0 && i < coa.getSize()) {
			if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
				account = coa.getAccount(i);
				i = -1;
			}

			i += dir;
		}

		return account;
	}

	/**
	 * Muodostaa uuden tilinumeron, joka on järjestyksessä
	 * seuraavana numeron <code>number</code> jälkeen.
	 *
	 * @param number tilinumero
	 * @return uusi tilinumero
	 */
	private String nextAccountNumber(String number) {
		int intValue = -1;

		try {
			intValue = Integer.parseInt(number);
		}
		catch (NumberFormatException e) { }

		if (intValue >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(intValue + 1);

			/* Lisätään eteen nollia, jotta uusi numero
			 * on yhtä pitkä kuin parametrina annettu numero. */
			while (sb.length() < number.length())
				sb.insert(0, "0");

			return sb.toString();
		}

		return number;
	}

	private void loadSettings() {
		Settings settings = registry.getSettings();
		String accountIdString = settings.getProperty("defaultAccount", "");

		try {
			int accountId = Integer.parseInt(accountIdString);
			defaultAccount = getAccountById(accountId);
		}
		catch (NumberFormatException e) {
			defaultAccount = null;
		}

		AppSettings appSettings = AppSettings.getInstance();
		nonFavouriteAccountsHidden = appSettings.getBoolean("chart-of-accounts.hide-non-favourite-accounts", false);

		if (nonFavouriteAccountsHidden) {
			coa.filterNonFavouriteAccounts();
		}
	}

	private void saveSettings(DataSource dataSource, Session sess) throws DataAccessException {
		Settings settings = registry.getSettings();
		String accountIdString = null;

		if (defaultAccount != null) {
			accountIdString = Integer.toString(defaultAccount.getId());
		}

		settings.setProperty("defaultAccount", accountIdString);
		dataSource.getSettingsDAO(sess).save(settings);

		AppSettings appSettings = AppSettings.getInstance();
		appSettings.set("chart-of-accounts.hide-non-favourite-accounts", nonFavouriteAccountsHidden);
	}

	private static interface DataSourceAction {
		public void execute(DataSource ds, Session sess)
			throws DataAccessException;
	}

	/**
	 * Toiminto, joka tallentaa tilin tietokantaan.
	 */
	private static class SaveAccountAction implements DataSourceAction {
		private Account account;

		public SaveAccountAction(Account account) {
			this.account = account;
		}

		public boolean equals(Object other) {
			return (other != null && other instanceof SaveAccountAction &&
					((SaveAccountAction)other).account == account);
		}

		public void execute(DataSource ds, Session sess)
			throws DataAccessException
		{
			ds.getAccountDAO(sess).save(account);
		}
	}

	/**
	 * Toiminto, joka poistaa tilin tietokannasta.
	 */
	private static class DeleteAccountAction implements DataSourceAction {
		private Account account;

		public DeleteAccountAction(Account account) {
			this.account = account;
		}

		public boolean equals(Object other) {
			return (other != null && other instanceof DeleteAccountAction &&
					((DeleteAccountAction)other).account == account);
		}

		public void execute(DataSource ds, Session sess)
			throws DataAccessException
		{
			ds.getAccountDAO(sess).delete(account.getId());
		}
	}

	/**
	 * Toiminto, joka tallentaa tilikartan väliotsikon tietokantaan.
	 */
	private static class SaveCOAHeadingAction implements DataSourceAction {
		private COAHeading heading;

		public SaveCOAHeadingAction(COAHeading heading) {
			this.heading = heading;
		}

		public boolean equals(Object other) {
			return (other != null && other instanceof SaveCOAHeadingAction &&
					((SaveCOAHeadingAction)other).heading == heading);
		}

		public void execute(DataSource ds, Session sess)
			throws DataAccessException
		{
			ds.getCOAHeadingDAO(sess).save(heading);
		}
	}

	/**
	 * Toiminto, joka poistaa tilikartan väliotsikon tietokannasta.
	 */
	private static class DeleteCOAHeadingAction implements DataSourceAction {
		private COAHeading heading;

		public DeleteCOAHeadingAction(COAHeading heading) {
			this.heading = heading;
		}

		public boolean equals(Object other) {
			return (other != null && other instanceof DeleteCOAHeadingAction &&
					((DeleteCOAHeadingAction)other).heading == heading);
		}

		/**
		 * Poistaa tilikartan väliotsikon tietokannasta.
		 */
		public void execute(DataSource ds, Session sess)
			throws DataAccessException
		{
			ds.getCOAHeadingDAO(sess).delete(heading.getId());
		}
	}
}
