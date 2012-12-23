package kirjanpito.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.jar.JarFile;

import javax.swing.SwingWorker;

import kirjanpito.db.Account;
import kirjanpito.db.AccountDAO;
import kirjanpito.db.COAHeading;
import kirjanpito.db.COAHeadingDAO;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Period;
import kirjanpito.db.ReportStructure;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;

/**
 * <code>SwingWorker</code>, joka lisää tyhjään tietokantaan
 * pakolliset perustiedot.
 */
public class DataSourceInitializationWorker extends SwingWorker<Void, Void> {
	private DataSource dataSource;
	private Session sess;
	private File archiveFile;
	private JarFile jar;
	private boolean initialized;

	public DataSourceInitializationWorker(DataSource dataSource,
			File archiveFile) {

		this.dataSource = dataSource;
		this.archiveFile = archiveFile;
	}

	/**
	 * Ilmoittaa, onko perustietojen lisääminen onnistunut.
	 *
	 * @return <code>true</code>, jos perustiedot on lisätty
	 */
	public boolean isInitialized() {
		return initialized;
	}

	protected Void doInBackground() throws Exception {
		try {
			sess = dataSource.openSession();
			jar = new JarFile(archiveFile);
			init();
			createCOA();
			copyReportStructure("balance-sheet");
			copyReportStructure("balance-sheet-detailed");
			copyReportStructure("income-statement");
			copyReportStructure("income-statement-detailed");

			if (isCancelled()) {
				sess.rollback();
			}
			else {
				sess.commit();
			}
		}
		catch (Exception e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			sess.close();
		}

		return null;
	}

	private void init() throws DataAccessException {
		/* Luodaan tilikausi, jonka alkamispäivä on
		 * nykyisen vuoden ensimmäinen päivä ja päättymispäivä
		 * vuoden viimeinen päivä. */
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);

		Period period = new Period();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, 0);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		period.setStartDate(cal.getTime());
		cal.set(Calendar.MONTH, 11);
		cal.set(Calendar.DAY_OF_MONTH, 31);
		period.setEndDate(cal.getTime());
		dataSource.getPeriodDAO(sess).save(period);

		/* Asetetaan luotu tilikausi nykyiseksi tilikaudeksi. */
		Settings settings = dataSource.getSettingsDAO(sess).get();
		settings.setCurrentPeriodId(period.getId());
		settings.setName("");
		settings.setBusinessId("");
		settings.setDocumentTypeId(-1);
		dataSource.getSettingsDAO(sess).save(settings);

		/* Luodaan 0-tosite, johon tallennetaan taseen tilien alkusaldot. */
		Document document = new Document();
		document.setDate(period.getStartDate());
		document.setNumber(0);
		document.setPeriodId(period.getId());
		dataSource.getDocumentDAO(sess).save(document);
	}

	private void createCOA() throws IOException, DataAccessException {
		/* Luodaan tilikartta. */
		BufferedReader reader = new BufferedReader(new InputStreamReader(
						jar.getInputStream(jar.getEntry("chart-of-accounts.txt")),
						Charset.forName("UTF-8")));

		String line;
		String[] fields;
		AccountDAO accountDAO = dataSource.getAccountDAO(sess);
		COAHeadingDAO headingDAO = dataSource.getCOAHeadingDAO(sess);
		HashMap<String, Account> accounts = new HashMap<String, Account>();
		Account account;
		COAHeading heading;
		int index;
		double count;
		boolean containsVatAccounts = false;
		final BigDecimal[] vatRateMapping = {
			BigDecimal.ZERO,
			new BigDecimal("22"),
			new BigDecimal("17"),
			new BigDecimal("8"),
			new BigDecimal("12"),
			new BigDecimal("9"),
			new BigDecimal("13"),
			new BigDecimal("23")
		};

		line = reader.readLine();
		count = Integer.parseInt(line);
		index = 0;

		/* Luetaan tilit ja otsikot CSV-tiedostosta. */
		while ((line = reader.readLine()) != null && !isCancelled()) {
			fields = line.split(";");

			if (fields.length < 2)
				continue;

			/* Tiliriveissä ensimmäinen kenttä on "A" */
			if (fields[0].equals("A")) {
				/* 2. tilinumero, 3. nimi, 4. tyyppi */
				account = new Account();
				account.setNumber(fields[1]);
				account.setName(fields[2]);
				account.setType(Integer.parseInt(fields[3]));
				account.setVatAccount1Id(-1);
				account.setVatAccount2Id(-1);
				accountDAO.save(account);
				accounts.put(account.getNumber(), account);
			}
			/* Otsikkoriveissä ensimmäinen kenttä on "H" */
			else if (fields[0].equals("H")) {
				/* 2. numero, 3. teksti, 4. otsikkotaso */
				heading = new COAHeading();
				heading.setNumber(fields[1]);
				heading.setText(fields[2]);
				heading.setLevel(Integer.parseInt(fields[3]));
				headingDAO.save(heading);
			}
			/* ALV-riveissä ensimmäinen kenttä on "V" */
			else if (fields[0].equals("V")) {
				account = accounts.get(fields[1]);
				account.setVatCode(Integer.parseInt(fields[2]));

				if (fields[3].endsWith("%")) {
					account.setVatRate(new BigDecimal(
							fields[3].substring(0, fields[3].length() - 1)));
				}
				else {
					account.setVatRate(vatRateMapping[Integer.parseInt(fields[3])]);
				}

				if (fields.length > 4) {
					account.setVatAccount1Id(accounts.get(fields[4]).getId());
				}
				else {
					account.setVatAccount1Id(-1);
				}

				if (fields.length > 5) {
					account.setVatAccount2Id(accounts.get(fields[5]).getId());
				}
				else {
					account.setVatAccount2Id(-1);
				}

				if (account.getVatCode() == 2 || account.getVatCode() == 3) {
					containsVatAccounts = true;
				}

				accountDAO.save(account);
			}

			setProgress((int)(index / count * 100));
			index++;
		}

		reader.close();

		/* Piilotetaan ALV-sarake, jos tilikarttamalli ei sisällä ALV-tilejä. */
		if (!containsVatAccounts) {
			Settings settings = dataSource.getSettingsDAO(sess).get();
			settings.setProperty("vatVisible", "false");
			dataSource.getSettingsDAO(sess).save(settings);
		}
	}

	private void copyReportStructure(String name) throws IOException, DataAccessException {
		String filename = name + ".txt";

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				jar.getInputStream(jar.getEntry(filename)),
				Charset.forName("UTF-8")));

		StringBuilder sb = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}

		ReportStructure s = new ReportStructure();
		s.setId(name);
		s.setData(sb.toString());
		dataSource.getReportStructureDAO(sess).save(s);
	}
}
