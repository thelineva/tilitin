package kirjanpito.models;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.Registry;

/**
 * Malli perustietojen muokkausikkunalle.
 *
 * @author Tommi Helineva
 */
public class PropertiesModel {
	private Registry registry;
	private Settings settings;
	private List<Period> periods;
	private HashSet<Period> changedPeriods;
	private int currentPeriodIndex;

	public PropertiesModel(Registry registry) {
		this.registry = registry;
		this.settings = registry.getSettings();
		this.changedPeriods = new HashSet<Period>();
	}

	/**
	 * Palauttaa asetukset.
	 *
	 * @return asetukset
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Hakee tarvittavat tiedot tietokannasta.
	 *
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void initialize() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			periods = dataSource.getPeriodDAO(sess).getAll();
		}
		finally {
			if (sess != null) sess.close();
		}

		changedPeriods.clear();
		int index = 0;

		/* Etsitään nykyinen tilikausi. */
		for (Period period : periods) {
			if (period.getId() == settings.getCurrentPeriodId()) {
				currentPeriodIndex = index;
				break;
			}

			index++;
		}
	}

	/**
	 * Tallentaa muutokset tietokantaan.
	 *
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			savePeriods(sess);
			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		registry.setPeriod(periods.get(currentPeriodIndex));
		registry.fireSettingsChanged();
		registry.firePeriodChanged();
	}

	private void savePeriods(Session sess) throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Period prevPeriod = null;
		boolean created;

		for (Period period : periods) {
			if (changedPeriods.contains(period)) {
				created = period.getId() <= 0;
				dataSource.getPeriodDAO(sess).save(period);

				/* Jos tilikausi on uusi, luodaan 0-tosite, johon tallennetaan
				 * alkusaldot. */
				if (created) {
					copyStartingBalances(sess, period, prevPeriod);
				}
				/* Varmistetaan, että 0-tositteessa on tilikauden
				 * alkamispäivämäärä. */
				else {
					Document doc = dataSource.getDocumentDAO(
							sess).getByPeriodIdAndNumber(period.getId(), 0);

					if (doc == null) {
						doc = new Document();
						doc.setPeriodId(period.getId());
						doc.setNumber(0);
					}

					doc.setDate(period.getStartDate());
					dataSource.getDocumentDAO(sess).save(doc);
				}
			}

			prevPeriod = period;
		}

		changedPeriods.clear();
		settings.setCurrentPeriodId(periods.get(currentPeriodIndex).getId());
		dataSource.getSettingsDAO(sess).save(settings);
	}

	/**
	 * Palauttaa nykyisen tilikauden järjestysnumeron.
	 *
	 * @return nykyisen tilikauden järjestysnumero
	 */
	public int getCurrentPeriodIndex() {
		return currentPeriodIndex;
	}

	/**
	 * Asettaa nykyisen tilikauden järjestysnumeron.
	 *
	 * @param currentPeriodIndex nykyisen tilikauden järjestysnumero
	 */
	public void setCurrentPeriodIndex(int currentPeriodIndex) {
		this.currentPeriodIndex = currentPeriodIndex;
	}

	/**
	 * Palauttaa tilikausien lukumäärän.
	 *
	 * @return tilikausien lukumäärä
	 */
	public int getPeriodCount() {
		return periods.size();
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan tilikauden.
	 *
	 * @param index rivinumero
	 * @return tilikausi
	 */
	public Period getPeriod(int index) {
		return periods.get(index);
	}

	/**
	 * Merkitsee rivillä <code>index</code> olevan
	 * tilikauden tiedot muuttuneiksi.
	 *
	 * @param index rivinumero
	 */
	public void updatePeriod(int index) {
		changedPeriods.add(periods.get(index));
	}

	/**
	 * Luo uuden tilikauden.
	 */
	public void createPeriod() {
		Calendar cal = Calendar.getInstance();

		/* Asetetaan uuden tilikauden alkamisajaksi nykyisen tilikauden
		 * päättymisaika + 1 päivä ja uuden tilikauden päättymisajaksi
		 * alkamisaika + 1 vuosi - 1 päivä. */
		Period period = periods.get(periods.size() - 1);

		cal.setTime(period.getEndDate());
		cal.add(Calendar.DAY_OF_MONTH, 1);
		Date startDate = cal.getTime();
		cal.add(Calendar.YEAR, 1);
		cal.add(Calendar.DAY_OF_MONTH, -1);
		Date endDate = cal.getTime();

		period = new Period();
		period.setStartDate(startDate);
		period.setEndDate(endDate);
		period.setLocked(false);
		periods.add(period);
		changedPeriods.add(period);
		currentPeriodIndex = periods.size() - 1;
	}

	/**
	 * Poistaa rivillä <code>index</code> olevan tilikauden tiedot.
	 *
	 * @param index rivinumero
	 */
	public void deletePeriod(int index) throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Period period = periods.get(index);
		Session sess = null;
		changedPeriods.remove(period);
		periods.remove(index);
		currentPeriodIndex = periods.size() - 1;
		settings.setCurrentPeriodId(periods.get(currentPeriodIndex).getId());

		try {
			dataSource.backup();
			sess = dataSource.openSession();
			dataSource.getEntryDAO(sess).deleteByPeriodId(period.getId());
			dataSource.getDocumentDAO(sess).deleteByPeriodId(period.getId());
			savePeriods(sess);
			dataSource.getPeriodDAO(sess).delete(period.getId());
			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		registry.setPeriod(periods.get(currentPeriodIndex));
		registry.fireSettingsChanged();
		registry.firePeriodChanged();
	}

	public void copyStartingBalances(Session sess,
			Period period, Period prevPeriod) throws DataAccessException {

		DataSource dataSource = registry.getDataSource();
		List<Account> accounts = dataSource.getAccountDAO(sess).getAll();
		final AccountBalances balances = new AccountBalances(accounts);

		/* Luetaan edellisen tilikauden viennit ja lasketaan taseen tilien
		 * loppusaldot. */
		dataSource.getEntryDAO(sess).getByPeriodId(prevPeriod.getId(), EntryDAO.ORDER_BY_DOCUMENT_NUMBER,
				new DTOCallback<Entry>() {
					public void process(Entry obj) {
						balances.addEntry(obj);
					}
				});

		/* Luodaan 0-tosite. */
		Document doc = new Document();
		doc.setDate(period.getStartDate());
		doc.setPeriodId(period.getId());
		doc.setNumber(0);
		dataSource.getDocumentDAO(sess).save(doc);

		BigDecimal balance;

		for (Account account : accounts) {
			/* Jos tili kuuluu taseeseen, lisätään vienti. */
			if (account.getType() == Account.TYPE_ASSET ||
					account.getType() == Account.TYPE_LIABILITY ||
					account.getType() == Account.TYPE_EQUITY ||
					account.getType() == Account.TYPE_PROFIT_PREV)
			{
				balance = balances.getBalance(account.getId());

				if (account.getType() == Account.TYPE_PROFIT_PREV) {
					if (balance == null)
						balance = BigDecimal.ZERO;

					balance = balance.add(balances.getProfit());
				}

				if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
					Entry entry = new Entry();
					StartingBalanceModel.createStartingBalanceEntry(account,
							balance, doc.getId(), entry);
					dataSource.getEntryDAO(sess).save(entry);
				}
			}
		}
	}
}
