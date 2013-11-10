package kirjanpito.models;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.DataSourceFactory;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.EntryTemplate;
import kirjanpito.db.EntryTemplateDAO;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.AppSettings;
import kirjanpito.util.AutoCompleteSupport;
import kirjanpito.util.DocumentRecordSet;
import kirjanpito.util.DummyAutoCompleteSupport;
import kirjanpito.util.EntryTemplateHelper;
import kirjanpito.util.FilteredDocumentRecordSet;
import kirjanpito.util.Registry;
import kirjanpito.util.SearchRecordSet;
import kirjanpito.util.TreeMapAutoCompleteSupport;
import kirjanpito.util.VATUtil;

/**
 * Malli tositteiden muokkausikkunalle.
 *
 * @author Tommi Helineva
 */
public class DocumentModel {
	private DocumentRecordSet recordSet;
	private Registry registry;
	private Document document;
	private List<Entry> entries;
	private ArrayList<BigDecimal> amounts;
	private ArrayList<BigDecimal> vatAmounts;
	private HashSet<Entry> deletedEntries;
	private int documentTypeIndex;
	private int documentCountTotal;
	private boolean changed;
	private boolean editable;
	private boolean autoCompleteEnabled;
	private AutoCompleteSupport autoCompleteSupport;
	private SimpleDateFormat monthFormat;
	private String[] lockedMonths;

	public static final int FETCH_FIRST = -4;
	public static final int FETCH_LAST = -3;
	public static final int FETCH_PREVIOUS = -2;
	public static final int FETCH_NEXT = -1;

	public DocumentModel(Registry registry) {
		this.registry = registry;
		deletedEntries = new HashSet<Entry>();
		amounts = new ArrayList<BigDecimal>();
		vatAmounts = new ArrayList<BigDecimal>();
		documentTypeIndex = -1;
		monthFormat = new SimpleDateFormat("yyyy-MM");
	}

	/**
	 * Avaa tietokantayhteyden.
	 *
	 * @throws DataAccessException jos yhteyden muodostaminen epäonnistuu
	 */
	public void openDataSource() throws DataAccessException {
		/* Luetaan tietokanta-asetukset. */
		AppSettings settings = AppSettings.getInstance();

		String url = settings.getString(
				"database.url", null);

		String username = settings.getString(
				"database.username", "kirjanpito");

		String password = settings.getString(
				"database.password", "kirjanpito");

		if (url == null) {
			url = buildDefaultJDBCURL();
		}

		registry.setDataSource(DataSourceFactory.create(url, username, password));
	}

	/**
	 * Sulkee tietokantayhteyden.
	 */
	public void closeDataSource() {
		registry.clear();
		recordSet = null;
		document = null;
		entries = null;
		amounts.clear();
		vatAmounts.clear();
		deletedEntries.clear();
		documentCountTotal = 0;
		documentTypeIndex = -1;
		editable = false;

		DataSource dataSource = registry.getDataSource();

		if (dataSource != null) {
			dataSource.close();
			registry.setDataSource(null);
		}
	}

	/**
	 * Palauttaa tietokannan oletus-JDBC-osoitteen.
	 *
	 * @return JDBC-osoite
	 */
	public String buildDefaultJDBCURL() {
		AppSettings settings = AppSettings.getInstance();
		File file = new File(settings.getDirectoryPath(), "kirjanpito.sqlite");
		return "jdbc:sqlite:" + file.getAbsolutePath();
	}

	public File getDatabaseDir() {
		AppSettings settings = AppSettings.getInstance();
		String url = settings.getString("database.url", null);
		File dir = null;

		if (url == null) {
			url = buildDefaultJDBCURL();
		}

		if (url.startsWith("jdbc:sqlite:")) {
			dir = new File(url.substring(12)).getParentFile();

			if (!dir.exists()) {
				dir = null;
			}
		}

		return dir;
	}

	/**
	 * Hakee tietokannasta tarvittavat tiedot.
	 *
	 * @return <code>true</code>, jos hakeminen onnistuu; <code>false</code>, jos
	 * tietokantaan ei ole lisätty vielä perustietoja
	 * @throws DataAccessException
	 */
	public boolean initialize() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		if (autoCompleteSupport == null) {
			setAutoCompleteEnabled(false);
		}

		try {
			sess = dataSource.openSession();

			/* Jos tietokantaan ei ole lisätty vielä perustietoja,
			 * keskeytetään tietojen hakeminen. */
			if (!registry.fetchPeriod(sess)) {
				return false;
			}

			registry.fetchSettings(sess);
			registry.fetchChartOfAccounts(sess);
			registry.fetchEntryTemplates(sess);
			registry.fetchDocumentTypes(sess);

			Period period = registry.getPeriod();
			loadLockedMonths();
			documentCountTotal = dataSource.getDocumentDAO(
					sess).getCountByPeriodId(period.getId(), 1);

			DocumentType type = findDocumentType();
			recordSet = new FilteredDocumentRecordSet(dataSource, period, type,
					autoCompleteSupport);
			recordSet.open(sess);

			if (recordSet.getCount() > 0) {
				fetchDocument();
			}
			else {
				createDocument(sess);
			}
		}
		catch (DataAccessException e) {
			closeDataSource();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		return true;
	}

	/**
	 * Hakee valitun tositelajin tositteet.
	 *
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void fetchDocuments(int position) throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();
		Session sess = null;
		editable = !period.isLocked();

		try {
			sess = dataSource.openSession();

			if (position < 0) {
				documentCountTotal = dataSource.getDocumentDAO(
						sess).getCountByPeriodId(period.getId(), 1);

				int documentTypeCount = registry.getDocumentTypes().size();

				if (documentTypeCount == 0) {
					documentTypeIndex = -1;
				}
				else if (documentTypeIndex >= documentTypeCount) {
					documentTypeIndex = documentTypeCount - 1;
				}
			}

			recordSet = new FilteredDocumentRecordSet(dataSource, period,
					getDocumentType(), autoCompleteSupport);
			recordSet.open(sess);

			if (position >= 0 && position < recordSet.getCount()) {
				goToDocument(position);
			}
			else if (recordSet.getCount() > 0) {
				fetchDocument();
			}
			else {
				createDocument(sess);
			}
		}
		finally {
			if (sess != null) sess.close();
		}
	}

	/**
	 * Hakee tositteita hakusanalla <code>q</code>.
	 *
	 * @param q hakusana
	 * @return tuloksien lukumäärä
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public int search(String q) throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();
		DocumentRecordSet searchRecordSet = new SearchRecordSet(dataSource,
				period, q, autoCompleteSupport);
		searchRecordSet.open();
		int count = searchRecordSet.getCount();

		if (count > 0) {
			recordSet = searchRecordSet;
			fetchDocument();
		}

		return count;
	}

	/**
	 * Palauttaa valitun tositteen.
	 *
	 * @return tosite
	 */
	public Document getDocument() {
		return document;
	}

	/**
	 * Palauttaa valitun tositelajin järjestysnumeron.
	 *
	 * @return valitun tositelajin järjestysnumero
	 */
	public int getDocumentTypeIndex() {
		return documentTypeIndex;
	}

	/**
	 * Asettaa valitun tositelajin järjestysnumeron.
	 *
	 * @param documentTypeIndex valitun tositelajin järjestysnumero
	 */
	public void setDocumentTypeIndex(int documentTypeIndex) {
		this.documentTypeIndex = documentTypeIndex;
	}

	/**
	 * Palauttaa valitun tositelajin.
	 *
	 * @return valittu tositelaji
	 */
	public DocumentType getDocumentType() {
		return (documentTypeIndex < 0) ? null :
			registry.getDocumentTypes().get(documentTypeIndex);
	}

	/**
	 * Palauttaa <code>true</code>, jos tositteen tai
	 * jonkin viennin tiedot ovat muuttuneet, ja tietoja
	 * ei ole vielä tallennettu tietokantaan.
	 *
	 * @return <code>true</code>, jos tositteen tai jonkin viennin
	 * tiedot ovat muuttuneet
	 */
	public boolean isDocumentChanged() {
		return changed;
	}

	/**
	 * Merkitsee tositteen tiedot muuttuneiksi.
	 */
	public void setDocumentChanged() {
		changed = true;
	}

	/**
	 * Palauttaa valitun tositteen järjestysnumeron.
	 *
	 * @return tositteen järjestysnumero
	 */
	public int getDocumentPosition() {
		if (recordSet == null) {
			return 0;
		}
		else if (recordSet.getCount() == 0) {
			return 0;
		}
		else {
			int pos = recordSet.getPosition();
			return (document.getId() == 0) ? pos + 1 : pos;
		}
	}

	/**
	 * Palauttaa tositteiden lukumäärän.
	 *
	 * @return tositteiden lukumäärä
	 */
	public int getDocumentCount() {
		if (recordSet == null) {
			return 0;
		}
		else {
			int count = recordSet.getCount();
			return (document.getId() == 0) ? count + 1 : count;
		}
	}

	public int getDocumentCountTotal() {
		if (recordSet == null) {
			return 0;
		}
		else {
			return (document.getId() == 0) ?
					documentCountTotal + 1 : documentCountTotal;
		}
	}

	/**
	 * Ilmoittaa, voiko nykyisen tilikauden tietoja muokata.
	 * @return <code>true</code>, jos nykyisen tilikauden tietoja voi muokata
	 */
	public boolean isPeriodEditable() {
		return editable;
	}

	/**
	 * Ilmoittaa, voiko valittua tositetta muokata.
	 *
	 * @return <code>true</code>, jos tositetta voi muokata
	 */
	public boolean isDocumentEditable() {
		boolean monthEditable = true;

		if (document != null) {
			monthEditable = isMonthEditable(document.getDate());
		}

		return monthEditable && editable;
	}

	/**
	 * Siirtyy tiettyyn tositteeseen.
	 *
	 * @param index tositteen järjestysnumero
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void goToDocument(int index) throws DataAccessException {
		if (recordSet.getCount() == 0) {
			createDocument();
		}
		else if (index == FETCH_NEXT) {
			if (recordSet.getPosition() == recordSet.getCount() - 1) {
				if (document.getId() > 0 && editable)
					createDocument();
			}
			else {
				recordSet.next();
				fetchDocument();
			}
		}
		else if (index == FETCH_PREVIOUS) {
			if (document.getId() == 0 && recordSet.getCount() > 0) {
				fetchDocument();
			}
			else if (recordSet.getPosition() > 0) {
				recordSet.previous();
				fetchDocument();
			}
		}
		else if (index == FETCH_FIRST) {
			recordSet.first();
			fetchDocument();
		}
		else if (index == FETCH_LAST) {
			recordSet.last();
			fetchDocument();
		}
		else {
			recordSet.move(index);
			fetchDocument();
		}
	}

	/**
	 * Luo uuden tositteen.
	 *
	 * @throws DataAccessException jos tositteen luominen epäonnistuu
	 */
	public void createDocument() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			createDocument(sess);
		}
		finally {
			if (sess != null) sess.close();
		}
	}

	private void createDocument(Session sess) throws DataAccessException {
		if (recordSet.getCount() > 0)
			recordSet.last(sess);

		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();
		DocumentType type = getDocumentType();

		if (type == null) {
			document = dataSource.getDocumentDAO(sess).create(
					period.getId(), 1, Integer.MAX_VALUE);
		}
		else {
			document = dataSource.getDocumentDAO(sess).create(
					period.getId(), type.getNumberStart(), type.getNumberEnd());
		}

		if (lockedMonths.length > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setLenient(true);
			cal.setTime(document.getDate());

			while (!isMonthEditable(cal.getTime())) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}

			document.setDate(cal.getTime());
		}

		entries = new ArrayList<Entry>();
		amounts.clear();
		vatAmounts.clear();
	}

	/**
	 * Poistaa valitun tositteen.
	 *
	 * @throws DataAccessException jos poistaminen epäonnistuu
	 */
	public void deleteDocument() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		boolean newDocument = (document.getId() == 0);
		Session sess = null;
		EntryDAO entryDAO;

		try {
			sess = dataSource.openSession();

			/* Tietokantaan ei tarvitse tehdä muutoksia, jos tosite
			 * on vasta luotu eikä sitä ole vielä tietokannassa. */
			if (newDocument) {
				if (recordSet.getCount() > 0) {
					fetchDocument();
				}
				else {
					createDocument(sess);
				}
			}
			else {
				entryDAO = dataSource.getEntryDAO(sess);

				/* Poistetaan tositteeseen kuuluvat viennit. */
				for (Entry entry : entries) {
					if (entry.getId() > 0) {
						entryDAO.delete(entry.getId());
					}
				}

				dataSource.getDocumentDAO(sess).delete(document.getId());
				sess.commit();
				documentCountTotal--;
				recordSet.remove();

				if (recordSet.getPosition() > 0) {
					recordSet.previous(sess);
					fetchDocument();
				}
				else {
					createDocument(sess);
				}
			}
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		deletedEntries.clear();
		changed = false;
	}

	/**
	 * Tallentaa valitun tositteen tiedot tietolähteeseen.
	 */
	public void saveDocument() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();

		if (period.isLocked()) {
			throw new RuntimeException("Tilikausi on lukittu");
		}

		boolean newDocument = (document.getId() == 0);
		Session sess = null;
		EntryDAO entryDAO;

		try {
			sess = dataSource.openSession();
			entryDAO = dataSource.getEntryDAO(sess);

			for (Entry entry : deletedEntries) {
				entryDAO.delete(entry.getId());
			}

			dataSource.getDocumentDAO(sess).save(document);

			for (Entry entry : entries) {
				entry.setDocumentId(document.getId());
				entryDAO.save(entry);
			}

			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		if (newDocument) {
			recordSet.add();
			documentCountTotal++;
		}

		recordSet.setDocument(document);
		recordSet.setEntries(entries);
		deletedEntries.clear();
		changed = false;
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan viennin.
	 *
	 * @param index rivinumero
	 * @return vienti
	 */
	public Entry getEntry(int index) {
		return entries.get(index);
	}

	/**
	 * Palauttaa valitussa tositteessa olevien vientien lukumäärän.
	 *
	 * @return vientien lukumäärä
	 */
	public int getEntryCount() {
		return amounts.size();
	}

	/**
	 * Lisää uuden viennin.
	 *
	 * @return uuden viennin järjestysnumero
	 */
	public int addEntry() {
		int count = getEntryCount();
		int rowNumber = 0;
		String description = "";
		Entry entry;

		for (int i = 0; i < count; i++) {
			entry = entries.get(i);
			rowNumber = Math.max(rowNumber, entry.getRowNumber());
			description = entry.getDescription();
		}

		entry = new Entry();
		entry.setAccountId(-1);
		entry.setDebit(true);
		entry.setAmount(BigDecimal.ZERO);
		entry.setDescription(description);
		entry.setRowNumber(rowNumber + 1);
		entries.add(count, entry);
		amounts.add(BigDecimal.ZERO);
		vatAmounts.add(BigDecimal.ZERO);
		setDefaultAccount(count);
		setDocumentChanged();
		return count;
	}

	/**
	 * Palauttaa viennin ALV:n määrän.
	 *
	 * @param index rivinumero
	 * @return alv
	 */
	public BigDecimal getVatAmount(int index) {
		return vatAmounts.get(index);
	}

	/**
	 * Palauttaa viennin verollisen rahamäärän.
	 *
	 * @param index rivinumero
	 * @return rahamäärä
	 */
	public BigDecimal getVatIncludedAmount(int index) {
		return amounts.get(index);
	}

	public void updateAccountId(int index, int accountId) {
		Entry entry = entries.get(index);
		Account account = registry.getAccountById(accountId);
		BigDecimal amount = getVatIncludedAmount(index);
		entry.setAccountId(accountId);

		/* Jos tili on valittu, mutta rahamäärää ei ole
		 * vielä syötetty, asetetaan rahamääräksi
		 * debet- ja kreditvientien erotus. */
		if (account != null && amount.compareTo(BigDecimal.ZERO) == 0) {
			BigDecimal diff = calculateDebitCreditDifference();

			if (account.getType() == Account.TYPE_REVENUE) {
				entry.setDebit(false);
			}
			else if (account.getType() == Account.TYPE_EXPENSE) {
				entry.setDebit(true);
			}
			else if (diff.compareTo(BigDecimal.ZERO) > 0) {
				entry.setDebit(false);
			}
			else {
				entry.setDebit(true);
			}

			if (entry.isDebit() && diff.compareTo(BigDecimal.ZERO) < 0) {
				amount = diff.abs();
			}
			else if (!entry.isDebit() && diff.compareTo(BigDecimal.ZERO) > 0) {
				amount = diff;
			}
		}
		else if (account != null) {
			if (account.getType() == Account.TYPE_REVENUE) {
				entry.setDebit(false);
			}
			else if (account.getType() == Account.TYPE_EXPENSE) {
				entry.setDebit(true);
			}
		}

		updateAmount(index, amount, true);
	}

	private BigDecimal calculateDebitCreditDifference() {
		int count = amounts.size();
		BigDecimal diff = BigDecimal.ZERO;
		Entry entry;

		for (int i = 0; i < count; i++) {
			entry = entries.get(i);

			if (entry.isDebit()) {
				diff = diff.add(getVatIncludedAmount(i));
			}
			else {
				diff = diff.subtract(getVatIncludedAmount(i));
			}
		}

		return diff;
	}

	/**
	 * Päivittää viennin rahamäärän.
	 *
	 * @param index rivinumero
	 * @param amount rahamäärä
	 * @param addVatEntries määrittää, lisätäänkö myös ALV-viennit
	 */
	public void updateAmount(int index, BigDecimal amount, boolean addVatEntries) {
		Entry entry = entries.get(index);
		BigDecimal vatAmount, vatExcludedAmount;

		if (amount == null)
			amount = BigDecimal.ZERO;

		if (entry.getAccountId() >= 0 && addVatEntries) {
			Account account = registry.getAccountById(entry.getAccountId());

			if (account == null || account.getVatAccount1Id() < 0) {
				/* ALV:tä ei lasketa, jos vastatiliä ei ole määritetty. */
				vatAmount = BigDecimal.ZERO;
				vatExcludedAmount = amount;
				removeVatEntry(entry, 1);
				removeVatEntry(entry, 2);
				removeVatEntry(entry, 3);
			}
			else if (account.getVatCode() == 9 || account.getVatCode() == 11) { // Yhteisöosto tai rakentamispalvelun osto
				vatAmount = VATUtil.addVatAmount(account.getVatRate(), amount);
				vatExcludedAmount = amount;
				removeVatEntry(entry, 1);
				updateVatEntry(entry, account.getVatAccount1Id(),
						vatAmount, entry.isDebit(), 2);

				updateVatEntry(entry, account.getVatAccount2Id(),
						vatAmount, !entry.isDebit(), 3);
			}
			else {
				vatAmount = VATUtil.subtractVatAmount(account.getVatRate(), amount);
				vatExcludedAmount = amount.subtract(vatAmount);
				updateVatEntry(entry, account.getVatAccount1Id(),
						vatAmount, entry.isDebit(), 1);

				removeVatEntry(entry, 2);
				removeVatEntry(entry, 3);
			}
		}
		else {
			vatAmount = BigDecimal.ZERO;
			vatExcludedAmount = amount;
			removeVatEntry(entry, 1);
			removeVatEntry(entry, 2);
			removeVatEntry(entry, 3);
		}

		entry.setAmount(vatExcludedAmount);
		amounts.set(index, amount);
		vatAmounts.set(index, vatAmount);
	}

	/**
	 * Päivittää ALV:n määrän.
	 *
	 * @param index rivinumero
	 * @param amount ALV:n määrä
	 */
	public void updateVatAmount(int index, BigDecimal vatAmount) {
		Entry entry = entries.get(index);

		if (vatAmount == null) {
			vatAmount = BigDecimal.ZERO;
		}
		else if (vatAmount.compareTo(BigDecimal.ZERO) < 0) {
			vatAmount = vatAmount.negate();
		}

		if (entry.getAccountId() >= 0) {
			Account account = registry.getAccountById(entry.getAccountId());

			if (account == null || account.getVatAccount1Id() < 0) {
				/* ALV:tä ei lasketa, jos vastatiliä ei ole määritetty. */
				vatAmount = BigDecimal.ZERO;
				removeVatEntry(entry, 1);
				removeVatEntry(entry, 2);
				removeVatEntry(entry, 3);
			}
			else if (account.getVatCode() == 9 || account.getVatCode() == 11) { // Yhteisöosto tai rakentamispalvelun osto
				removeVatEntry(entry, 1);
				updateVatEntry(entry, account.getVatAccount1Id(),
						vatAmount, entry.isDebit(), 2);

				updateVatEntry(entry, account.getVatAccount2Id(),
						vatAmount, !entry.isDebit(), 3);
			}
			else {
				updateVatEntry(entry, account.getVatAccount1Id(),
						vatAmount, entry.isDebit(), 1);

				removeVatEntry(entry, 2);
				removeVatEntry(entry, 3);
			}
		}
		else {
			removeVatEntry(entry, 1);
			removeVatEntry(entry, 2);
			removeVatEntry(entry, 3);
		}

		entry.setAmount(amounts.get(index).subtract(vatAmount));
		vatAmounts.set(index, vatAmount);
	}

	/**
	 * Poistaa viennin riviltä <code>index</code>.
	 *
	 * @param index
	 */
	public void removeEntry(int index) {
		Entry entry = entries.remove(index);

		/* Jos vienti on tallennettu tietokantaan, poistetaan
		 * se, kun tosite tallennetaan seuraavan kerran. */
		if (entry.getId() > 0) {
			deletedEntries.add(entry);
		}

		amounts.remove(index);
		vatAmounts.remove(index);
		removeVatEntry(entry, 1);
		removeVatEntry(entry, 2);
		removeVatEntry(entry, 3);
		setDocumentChanged();
	}

	/**
	 * Luo ALV-tilien päättämistositteen.
	 *
	 * @throws DataAccessException jos ALV-tietojen hakeminen epäonnistuu
	 * @return <code>false</code>, jos ALV-velkatiliä ei ole määritetty
	 */
	public boolean createVATDocument() throws DataAccessException {
		final AccountBalances balances = new AccountBalances(registry.getAccounts());
		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			/* Lasketaan ALV-tilien saldot. */
			dataSource.getEntryDAO(sess).getByPeriodId(period.getId(), EntryDAO.ORDER_BY_DOCUMENT_NUMBER,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());

						if (account.getVatCode() == 2 || account.getVatCode() == 3) {
							balances.addEntry(entry);
						}
					}
				});
			createDocument(sess);
		}
		finally {
			if (sess != null) sess.close();
		}

		BigDecimal balance;
		BigDecimal debt = BigDecimal.ZERO;
		Account debtAccount = null;
		Entry entry;

		/* Lisätään viennit. */
		for (Account account : registry.getAccounts()) {
			balance = balances.getBalance(account.getId());

			if (account.getVatCode() == 1) {
				debtAccount = account;
			}

			if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
				entry = new Entry();
				entry.setAccountId(account.getId());
				debt = debt.add(balance);

				if (balance.compareTo(BigDecimal.ZERO) < 0) {
					balance = balance.negate();
					entry.setDebit(false);
				}
				else {
					entry.setDebit(true);
				}

				entry.setAmount(balance);
				entry.setDescription("");
				entry.setRowNumber(amounts.size());
				entry.setFlag(0, true);

				this.amounts.add(balance);
				this.vatAmounts.add(BigDecimal.ZERO);
				this.entries.add(entry);
			}
		}

		/* Lisätään ALV-velkavienti, jos ALV-velkatili on määritetty. */
		if (debtAccount != null && debt.compareTo(BigDecimal.ZERO) != 0) {
			entry = new Entry();
			entry.setAccountId(debtAccount.getId());

			if (debt.compareTo(BigDecimal.ZERO) < 0) {
				debt = debt.negate();
				entry.setDebit(true);
			}
			else {
				entry.setDebit(false);
			}

			entry.setAmount(debt);
			entry.setDescription("");
			entry.setRowNumber(amounts.size());

			this.amounts.add(debt);
			this.vatAmounts.add(BigDecimal.ZERO);
			this.entries.add(entry);
		}

		setDocumentChanged();
		return debtAccount != null;
	}

	/**
	 * Lisää viennit mallin perusteella.
	 *
	 * @param number vientimallin numero
	 */
	public void addEntriesFromTemplate(int number) {
		Entry entry;
		EntryTemplateHelper helper = new EntryTemplateHelper(document.getDate());

		for (EntryTemplate template : registry.getEntryTemplates()) {
			if (template.getNumber() == number) {
				entry = new Entry();
				entry.setAccountId(template.getAccountId());
				entry.setDebit(template.isDebit());
				entry.setDescription(helper.substitutePlaceholders(template.getDescription()));
				entry.setRowNumber(amounts.size());

				this.amounts.add(BigDecimal.ZERO);
				this.vatAmounts.add(BigDecimal.ZERO);
				this.entries.add(entry.getRowNumber(), entry);
				updateAmount(entry.getRowNumber(), template.getAmount(), true);
			}
		}

		setDocumentChanged();
	}

	public int createEntryTemplateFromDocument() throws DataAccessException {
		int count = getEntryCount();

		if (count == 0) {
			return -1;
		}

		EntryTemplate template;
		int number = 1;
		boolean match = true;

		/* Etsitään seuraava vapaa numero. */
		while (match) {
			match = false;

			for (EntryTemplate t : registry.getEntryTemplates()) {
				if (t.getNumber() == number) {
					number++;
					match = true;
					break;
				}
			}
		}

		DataSource dataSource = registry.getDataSource();
		EntryTemplateDAO dao;
		Session sess = null;
		String name = entries.get(0).getDescription();

		try {
			sess = dataSource.openSession();
			dao = dataSource.getEntryTemplateDAO(sess);

			for (int i = 0; i < count; i++) {
				Entry entry = entries.get(i);
				template = new EntryTemplate();
				template.setNumber(number);
				template.setName(name);
				template.setAccountId(entry.getAccountId());
				template.setAmount(amounts.get(i));
				template.setDebit(entry.isDebit());
				template.setDescription(entry.getDescription());
				template.setRowNumber(i);
				dao.save(template);
			}

			sess.commit();
			registry.fetchEntryTemplates(sess);
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		return number;
	}

	/**
	 * Etsii tositteen numerolla <code>number</code> ja palauttaa
	 * tositteen järjestysnumeron.
	 *
	 * @param documentTypeIndex tositelajin järjestysnumero
	 * @param number tositenumero
	 * @return löytyneen tositteen järjestysnumero tai -1, jos tositetta ei löytynyt
	 */
	public int findDocumentByNumber(int documentTypeIndex, int number)
		throws DataAccessException {

		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();
		Session sess = null;
		int index;

		try {
			sess = dataSource.openSession();

			if (documentTypeIndex < 0) {
				index = dataSource.getDocumentDAO(sess).getIndexByPeriodIdAndNumber(
						period.getId(), 1, Integer.MAX_VALUE, number);
			}
			else {
				DocumentType type = registry.getDocumentTypes(
						).get(documentTypeIndex);

				index = dataSource.getDocumentDAO(sess).getIndexByPeriodIdAndNumber(
						period.getId(), type.getNumberStart(),
						type.getNumberEnd(), number);
			}
		}
		finally {
			if (sess != null) sess.close();
		}

		return index;
	}

	/**
	 * Tallentaa oletustositelajin tietokantaan.
	 *
	 * @throws DataAccessException jos tietojen tallentaminen epäonnistuu
	 */
	public void saveDocumentType() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Settings settings = registry.getSettings();
		DocumentType type = getDocumentType();
		int documentTypeId = (type == null) ? -1 : type.getId();

		/* Tallennetaan asetukset, jos tositelaji on muuttunut. */
		if (settings != null && documentTypeId != settings.getDocumentTypeId()) {
			settings.setDocumentTypeId(documentTypeId);
			Session sess = null;

			try {
				sess = dataSource.openSession();
				dataSource.getSettingsDAO(sess).save(settings);
				sess.commit();
			}
			catch (DataAccessException e) {
				if (sess != null) sess.rollback();
				throw e;
			}
			finally {
				if (sess != null) sess.close();
			}
		}
	}

	public boolean isAutoCompleteEnabled() {
		return autoCompleteEnabled;
	}

	public void setAutoCompleteEnabled(boolean enabled) {
		autoCompleteEnabled = enabled;
		autoCompleteSupport = enabled ? new TreeMapAutoCompleteSupport() :
			new DummyAutoCompleteSupport();

		if (recordSet != null) {
			recordSet.setAutoCompleteSupport(autoCompleteSupport);
		}
	}

	/**
	 * Täydentää viennin selitteen.
	 *
	 * @param accountId tilin tunniste
	 * @param description selitteen alkuosa
	 * @return selite
	 */
	public String autoCompleteEntryDescription(int accountId, String description) {
		return autoCompleteSupport.autoCompleteEntryDescription(accountId, description);
	}

	/**
	 * Tarkistaa tositenumeron oikeellisuuden. Numero on virheellinen, jos
	 * se on jo käytössä tai se ei kuulu valitun tositelajin numerovälille.
	 *
	 * @param number tarkistettava tositenumero
	 * @return <0, jos virheellinen
	 * @throws DataAccessException jos tositetietojen hakeminen epäonnistuu
	 */
	public int validateDocumentNumber(int number) throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		DocumentType documentType = getDocumentType();
		Period period = registry.getPeriod();
		Document document = null;
		Session sess = null;

		try {
			sess = dataSource.openSession();
			document = dataSource.getDocumentDAO(sess).getByPeriodIdAndNumber(
					period.getId(), number);
		}
		finally {
			if (sess != null) sess.close();
		}

		if (document != null) {
			return -1;
		}

		if (documentType != null) {
			if (number < documentType.getNumberStart() || number > documentType.getNumberEnd()) {
				return -2;
			}
		}

		return 0;
	}

	public void loadLockedMonths() {
		Settings settings = registry.getSettings();
		String key = "locked/" + registry.getPeriod().getId();
		lockedMonths = settings.getProperty(key, "").split(",");
		Arrays.sort(lockedMonths);
		editable = !registry.getPeriod().isLocked();
	}

	public boolean isMonthEditable(Date date) {
		if (date == null) {
			return true;
		}

		return Arrays.binarySearch(lockedMonths,
				monthFormat.format(date)) < 0;
	}

	private void updateVatEntry(Entry entry, int accountId,
			BigDecimal vatAmount, boolean debit, int index) {

		/* Poistetaan ALV-vienti, jos ALV:n määrä on 0,00 tai
		 * vastatiliä ei ole määritetty. */
		if (vatAmount.compareTo(BigDecimal.ZERO) == 0 || accountId < 0) {
			removeVatEntry(entry, index);
		}
		else {
			Entry vatEntry = getVatEntry(entry, index);

			if (vatEntry == null) {
				vatEntry = addVatEntry(entry, index);
			}

			vatEntry.setAccountId(accountId);
			vatEntry.setDebit(debit);
			vatEntry.setAmount(vatAmount);
			vatEntry.setDescription("");
		}
	}

	private Entry getVatEntry(Entry entry, int index) {
		int vatRowNumber = getVatRowNumber(entry, index);

		for (Entry e : entries) {
			if (e.getRowNumber() == vatRowNumber) {
				return e;
			}
		}

		return null;
	}

	private Entry addVatEntry(Entry entry, int index) {
		Entry vatEntry = new Entry();
		vatEntry.setRowNumber(getVatRowNumber(entry, index));
		entries.add(vatEntry);
		return vatEntry;
	}

	private void removeVatEntry(Entry entry, int index) {
		int vatRowNumber = getVatRowNumber(entry, index);
		Iterator<Entry> iter = entries.iterator();
		Entry vatEntry;

		while (iter.hasNext()) {
			vatEntry = iter.next();

			if (vatEntry.getRowNumber() == vatRowNumber) {
				if (vatEntry.getId() > 0) deletedEntries.add(vatEntry);
				iter.remove();
			}
		}
	}

	private int getVatRowNumber(Entry entry, int index) {
		return entry.getRowNumber() + index * 100000;
	}

	private void fetchDocument() {
		BigDecimal amount, vatAmount;
		Entry vatEntry;

		document = recordSet.getDocument();
		entries = recordSet.getEntries();

		if (entries == null)
			entries = new ArrayList<Entry>();

		amounts.clear();
		vatAmounts.clear();

		for (Entry entry : entries) {
			if (entry.getRowNumber() >= 100000)
				break;

			amount = entry.getAmount();
			vatAmount = BigDecimal.ZERO;
			vatEntry = getVatEntry(entry, 1);

			if (vatEntry != null) {
				vatAmount = vatEntry.getAmount();
				amount = amount.add(vatAmount);
			}

			vatEntry = getVatEntry(entry, 2);

			if (vatEntry != null) {
				vatAmount = vatEntry.getAmount();
			}

			amounts.add(amount);
			vatAmounts.add(vatAmount);
		}
	}

	private DocumentType findDocumentType() {
		List<DocumentType> documentTypes = registry.getDocumentTypes();
		int documentTypeId = registry.getSettings().getDocumentTypeId();

		if (documentTypes.size() == 0) {
			documentTypeIndex = -1;
			return null;
		}
		else {
			documentTypeIndex = 0;
			int i = 0;

			for (DocumentType type : documentTypes) {
				if (type.getId() == documentTypeId) {
					documentTypeIndex = i;
					return type;
				}

				i++;
			}

			return documentTypes.get(0);
		}
	}

	private void setDefaultAccount(int index) {
		if (index == 0) {
			return;
		}

		Settings settings = registry.getSettings();
		String accountIdString = settings.getProperty("defaultAccount", "");
		int accountId;

		try {
			accountId = Integer.parseInt(accountIdString);
		}
		catch (NumberFormatException e) {
			return;
		}

		Account account = registry.getAccountById(accountId);

		if (account != null) {
			updateAccountId(index, accountId);
		}
	}
}
