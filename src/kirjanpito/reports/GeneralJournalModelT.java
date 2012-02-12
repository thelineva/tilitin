package kirjanpito.reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.Session;
import kirjanpito.util.CSVWriter;

/**
 * Malli päiväkirja tositelajeittain -tulosteelle.
 *
 * @author Tommi Helineva
 */
public class GeneralJournalModelT extends GeneralJournalModel {
	private List<DocumentType> documentTypes;

	public void run() throws DataAccessException {
		List<Document> documents;
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		final HashMap<Integer, Document> documentMap =
			new HashMap<Integer, Document>();

		final HashMap<Integer, DocumentType> documentTypeMap =
			new HashMap<Integer, DocumentType>();

		documentTypes = registry.getDocumentTypes();
		settings = registry.getSettings();
		rows = new ArrayList<GeneralJournalRow>();
		totalDebit = BigDecimal.ZERO;
		totalCredit = BigDecimal.ZERO;

		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(sess).getByPeriodId(period.getId(), 1);

			for (Document d : documents) {
				documentMap.put(d.getId(), d);
			}

			dataSource.getEntryDAO(sess).getByPeriodIdAndDate(
				period.getId(), startDate, endDate,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());
						Document document = documentMap.get(entry.getDocumentId());

						if (account == null || document == null) {
							return;
						}

						if (document.getDate().before(startDate) || document.getDate().after(endDate)) {
							return;
						}

						DocumentType type = getDocumentTypeByNumber(document.getNumber());

						if (type != null && !documentTypeMap.containsKey(account.getId())) {
							documentTypeMap.put(account.getId(), type);
						}

						if (entry.isDebit()) {
							totalDebit = totalDebit.add(entry.getAmount());
						}
						else {
							totalCredit = totalCredit.add(entry.getAmount());
						}

						lastDocumentNumber = Math.max(lastDocumentNumber, document.getNumber());
						rows.add(new GeneralJournalRow(1, document, type, account, entry));
					}
				});
		}
		finally {
			if (sess != null) sess.close();
		}

		/* Asetetaan tositelaji sellaisille riveille, joille ei ole vielä asetettu
		 * tositelajia (esim. alkusaldoviennit). */
		for (GeneralJournalRow row : rows) {
			if (row.documentType == null) {
				row.documentType = documentTypeMap.get(row.account.getId());

				if (row.documentType == null) {
					row.documentType = documentTypes.get(0);
				}
			}
		}

		if (orderBy == ORDER_BY_DATE) {
			Collections.sort(rows, new DocumentDateComparator());
		}
		else {
			Collections.sort(rows, new DocumentNumberComparator());
		}

		int prevDocumentType = -1;
		int prevDocument = -1;

		for (int i = 0; i < rows.size(); i++) {
			GeneralJournalRow row = rows.get(i);

			if (prevDocumentType != row.documentType.getId()) {
				if (prevDocumentType != -1) {
					rows.add(i++, new GeneralJournalRow(0, null, null, null, null));
				}

				rows.add(i++, new GeneralJournalRow(3, null, row.documentType, null, null));
				prevDocument = -1;
			}

			if (prevDocument != row.document.getId()) {
				rows.add(i++, new GeneralJournalRow(0, null, null, null, null));
				rows.add(i++, new GeneralJournalRow(2, row.document, null, null, null));
			}

			prevDocumentType = row.documentType.getId();
			prevDocument = row.document.getId();
		}

		if (totalAmountVisible) {
			rows.add(new GeneralJournalRow(0, null, null, null, null));
			rows.add(new GeneralJournalRow(4, null, null, null, null));
		}
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		writeCSV(writer, true);
	}

	private DocumentType getDocumentTypeByNumber(int number) {
		for (DocumentType type : documentTypes) {
			if (number >= type.getNumberStart() && number <= type.getNumberEnd()) {
				return type;
			}
		}

		return null;
	}

	private class DocumentNumberComparator implements Comparator<GeneralJournalRow> {
		@Override
		public int compare(GeneralJournalRow o1, GeneralJournalRow o2) {
			/* Vertaillaan
			 * 1. tositelajia
			 * 2. tositenumeroa
			 * 3. vientijärjestystä
			 */

			if (o1.documentType.getNumber() == o2.documentType.getNumber()) {
				if (o1.document.getNumber() == o2.document.getNumber()) {
					return o1.entry.getRowNumber() - o2.entry.getRowNumber();
				}
				else {
					return o1.document.getNumber() - o2.document.getNumber();
				}
			}
			else {
				return o1.documentType.getNumber() - o2.documentType.getNumber();
			}
		}
	}

	private class DocumentDateComparator implements Comparator<GeneralJournalRow> {
		@Override
		public int compare(GeneralJournalRow o1, GeneralJournalRow o2) {
			/* Vertaillaan
			 * 1. tositelajia
			 * 2. päivämäärää
			 * 3. tositenumeroa
			 * 4. vientijärjestystä
			 */

			if (o1.documentType.getNumber() == o2.documentType.getNumber()) {
				if (o1.document.getDate().equals(o2.document.getDate())) {
					if (o1.document.getNumber() == o2.document.getNumber()) {
						return o1.entry.getRowNumber() - o2.entry.getRowNumber();
					}
					else {
						return o1.document.getNumber() - o2.document.getNumber();
					}
				}
				else {
					return o1.document.getDate().compareTo(o2.document.getDate());
				}
			}
			else {
				return o1.documentType.getNumber() - o2.documentType.getNumber();
			}
		}
	}
}
