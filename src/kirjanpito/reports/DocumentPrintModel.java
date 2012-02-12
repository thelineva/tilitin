package kirjanpito.reports;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ODFSpreadsheet;
import kirjanpito.util.Registry;

public class DocumentPrintModel implements PrintModel {
	private Registry registry;
	private Settings settings;
	private Document document;
	private List<Entry> entries;

	public DocumentPrintModel() {
		document = new Document();
	}

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		document.copy(this.document);
	}

	public void run() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		settings = registry.getSettings();

		try {
			sess = dataSource.openSession();
			entries = dataSource.getEntryDAO(sess
					).getByDocumentId(document.getId());
		}
		finally {
			if (sess != null) sess.close();
		}
	}

	public void writeCSV(CSVWriter writer) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		DecimalFormat numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);

		writer.writeField("Tosite");
		writer.writeLine();
		writer.writeField("Nimi");
		writer.writeField(settings.getName());
		writer.writeLine();
		writer.writeField("Y-tunnus");
		writer.writeField(settings.getBusinessId());
		writer.writeLine();
		writer.writeField("Tositenumero");
		writer.writeField(Integer.toString(document.getNumber()));
		writer.writeLine();
		writer.writeField("Päivämäärä");
		writer.writeField(dateFormat.format(document.getDate()));
		writer.writeLine();
		writer.writeLine();
		writer.writeField("");
		writer.writeField("Tili");
		writer.writeField("Debet");
		writer.writeField("Kredit");
		writer.writeField("Selite");
		writer.writeLine();

		for (Entry entry : entries) {
			Account account = registry.getAccountById(entry.getAccountId());
			writer.writeField(account.getNumber());
			writer.writeField(account.getName());

			if (entry.isDebit()) {
				writer.writeField(numberFormat.format(entry.getAmount()));
				writer.writeField("");
			}
			else {
				writer.writeField("");
				writer.writeField(numberFormat.format(entry.getAmount()));
			}

			writer.writeField(entry.getDescription());
			writer.writeLine();
		}
	}

	public void writeODS(ODFSpreadsheet s) {
		s.setTitle(String.format("Tosite %d", document.getNumber()));
		s.defineColumn("co1", "1.5cm");
		s.defineColumn("co2", "5cm");
		s.defineColumn("co3", "2.6cm");
		s.defineColumn("co4", "6cm");
		s.addTable("Tosite");
		s.addColumn("co1", "Default");
		s.addColumn("co2", "Default");
		s.addColumn("co3", "Default");
		s.addColumn("co3", "Default");
		s.addColumn("co4", "Default");
		s.addRow();
		s.setColSpan(2);
		s.writeTextCell("Tositenumero", "bold");
		s.setColSpan(1);
		s.writeEmptyCell();
		s.writeFloatCell(document.getNumber(), "num0AlignLeft");
		s.addRow();
		s.setColSpan(2);
		s.writeTextCell("Päivämäärä", "bold");
		s.setColSpan(1);
		s.writeEmptyCell();
		s.writeDateCell(document.getDate(), "dateAlignLeft");
		s.addRow();
		s.addRow();
		s.setColSpan(2);
		s.writeTextCell("Tili", "boldBorderBottom");
		s.setColSpan(1);
		s.writeTextCell("", "boldBorderBottom");
		s.writeTextCell("Debet", "boldAlignRightBorderBottom");
		s.writeTextCell("Kredit", "boldAlignRightBorderBottom");
		s.writeTextCell("Selite", "boldBorderBottom");

		for (Entry entry : entries) {
			s.addRow();
			Account account = registry.getAccountById(entry.getAccountId());
			s.writeTextCell(account.getNumber());
			s.writeTextCell(account.getName());

			if (entry.isDebit()) {
				s.writeFloatCell(entry.getAccountId(), "num2");
				s.writeTextCell("", "num2");
			}
			else {
				s.writeTextCell("", "num2");
				s.writeFloatCell(entry.getAccountId(), "num2");
			}

			s.writeTextCell(entry.getDescription());
		}
	}

	/**
	 * Palauttaa käyttäjän nimen.
	 *
	 * @return käyttäjän nimi
	 */
	public String getName() {
		return settings.getName();
	}

	/**
	 * Palauttaa Y-tunnuksen.
	 *
	 * @return y-tunnus
	 */
	public String getBusinessId() {
		return settings.getBusinessId();
	}

	/**
	 * Palauttaa vientien lukumäärän.
	 *
	 * @return vientien lukumäärä
	 */
	public int getEntryCount() {
		return entries.size();
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
	 * Palauttaa rivillä <code>index</code> olevan viennin tilin.
	 *
	 * @param index rivinumero
	 * @return tili
	 */
	public Account getAccount(int index) {
		return registry.getAccountById(entries.get(index).getAccountId());
	}
}
