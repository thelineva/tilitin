package kirjanpito.models;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.Session;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.Registry;

public class StatisticsModel {
	private Registry registry;
	private Calendar calendar;
	private Date startDate;
	private Date endDate;
	private Account[] accounts;
	private String[] periodNames;
	private BigDecimal[][] amounts;

	public StatisticsModel(Registry registry) {
		this.registry = registry;
		this.calendar = Calendar.getInstance();
	}

	public boolean isEnabled() {
		return amounts != null;
	}

	public int getAccountCount() {
		return accounts.length;
	}

	public int getPeriodCount() {
		return periodNames.length;
	}

	public Account getAccount(int index) {
		return accounts[index];
	}

	public String getPeriodName(int index) {
		return periodNames[index];
	}

	public BigDecimal getAmount(int accountIndex, int periodIndex) {
		BigDecimal amount = amounts[accountIndex][periodIndex];
		return (amount.compareTo(BigDecimal.ZERO) == 0) ? null : amount;
	}

	public void clear() {
		accounts = null;
		periodNames = null;
		amounts = null;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void calculateWeeklyStatistics() throws DataAccessException {
		clear();
		ArrayList<Date> startDates = new ArrayList<Date>();
		ArrayList<Date> endDates = new ArrayList<Date>();
		Calendar cal = calendar;
		cal.setTime(startDate);
		cal.setLenient(true);

		/* Siirrytään edelliseen maanantaihin. */
		while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
			cal.add(Calendar.DAY_OF_MONTH, -1);
		}

		Date periodStartDate, periodEndDate;
		periodStartDate = cal.getTime();

		while (periodStartDate.before(endDate) && startDates.size() < 25) {
			cal.add(Calendar.DAY_OF_MONTH, 7);
			periodEndDate = cal.getTime();
			startDates.add(periodStartDate);
			endDates.add(periodEndDate);
			periodStartDate = periodEndDate;
		}

		calculateStatistics(startDates, endDates);
		periodNames = new String[startDates.size()];

		for (int i = 0; i < periodNames.length; i++) {
			cal.setTime(startDates.get(i));
			periodNames[i] = "Vko " + cal.get(Calendar.WEEK_OF_YEAR);
		}
	}

	public void calculateMonthlyStatistics() throws DataAccessException {
		clear();
		ArrayList<Date> startDates = new ArrayList<Date>();
		ArrayList<Date> endDates = new ArrayList<Date>();
		Calendar cal = calendar;
		cal.setTime(startDate);
		cal.setLenient(true);

		/* Siirrytään kuukauden ensimmäiseen päivään. */
		while (cal.get(Calendar.DAY_OF_MONTH) != 1) {
			cal.add(Calendar.DAY_OF_MONTH, -1);
		}

		Date periodStartDate, periodEndDate;
		periodStartDate = cal.getTime();

		while (periodStartDate.before(endDate)) {
			cal.add(Calendar.MONTH, 1);
			periodEndDate = cal.getTime();
			startDates.add(periodStartDate);
			endDates.add(periodEndDate);
			periodStartDate = periodEndDate;
		}

		calculateStatistics(startDates, endDates);
		DateFormatSymbols symbols = new DateFormatSymbols();
		String[] months = symbols.getShortMonths();
		periodNames = new String[startDates.size()];

		for (int i = 0; i < periodNames.length; i++) {
			cal.setTime(startDates.get(i));
			periodNames[i] = months[cal.get(Calendar.MONTH)];
		}
	}

	private void calculateStatistics(ArrayList<Date> startDates, ArrayList<Date> endDates)
			throws DataAccessException {

		DataSource dataSource = registry.getDataSource();
		List<Document> documents;
		Session sess = null;
		int periodId = registry.getPeriod().getId();
		final HashMap<Integer, Integer> documentMap = new HashMap<Integer, Integer>();
		final HashMap<Integer, ArrayList<BigDecimal>> map = new HashMap<Integer, ArrayList<BigDecimal>>();
		final int periodCount = startDates.size();
		Date date;

		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(sess).getByPeriodIdAndDate(
					periodId, startDate, endDate);

			for (Document document : documents) {
				date = document.getDate();

				for (int i = 0; i < periodCount; i++) {
					if ((date.after(startDates.get(i)) ||
							date.equals(startDates.get(i))) &&
							date.before(endDates.get(i))) {
						documentMap.put(document.getId(), i);
						break;
					}
				}
			}

			documents = null;
			dataSource.getEntryDAO(sess).getByPeriodIdAndDate(
					periodId, startDate, endDate, new DTOCallback<Entry>() {
				public void process(Entry entry) {
					int accountId;
					int type;
					boolean debit;
					BigDecimal amount;

					accountId = entry.getAccountId();
					type = registry.getAccountById(accountId).getType();

					if (type != Account.TYPE_EXPENSE && type != Account.TYPE_REVENUE)
						return;

					debit = entry.isDebit();
					amount = entry.getAmount();

					if ((type == Account.TYPE_EXPENSE && !debit) ||
							(type == Account.TYPE_REVENUE && debit))
					{
						amount = amount.negate();
					}

					Integer periodIndex = documentMap.get(entry.getDocumentId());

					if (periodIndex == null) {
						return;
					}

					ArrayList<BigDecimal> list = map.get(accountId);

					if (list == null) {
						list = createList(periodCount);
						list.set(periodIndex, amount);
						map.put(accountId, list);
					}
					else {
						list.set(periodIndex, list.get(periodIndex).add(amount));
					}
				}
			});
		}
		finally {
			if (sess != null) sess.close();
		}

		accounts = new Account[map.size()];
		amounts = new BigDecimal[accounts.length][periodCount];
		int index = 0;

		for (Account account : registry.getAccounts()) {
			ArrayList<BigDecimal> list = map.get(account.getId());

			if (list != null) {
				accounts[index] = account;
				list.toArray(amounts[index]);
				index++;
			}
		}
	}

	private ArrayList<BigDecimal> createList(int size) {
		ArrayList<BigDecimal> amounts = new ArrayList<BigDecimal>(size);

		for (int i = 0; i < size; i++) {
			amounts.add(BigDecimal.ZERO);
		}

		return amounts;
	}

	public void save(File file) throws IOException {
		CSVWriter writer = new CSVWriter(new FileWriter(file));
		writer.writeField("Nro");
		writer.writeField("Tili");

		for (String periodName : periodNames)
			writer.writeField(periodName);

		writer.writeLine();

		DecimalFormat formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);

		for (int i = 0; i < accounts.length; i++) {
			writer.writeField(accounts[i].getNumber());
			writer.writeField(accounts[i].getName());

			for (int j = 0; j < periodNames.length; j++) {
				writer.writeField(formatter.format(amounts[i][j]));
			}

			writer.writeLine();
		}

		writer.close();
	}
}
