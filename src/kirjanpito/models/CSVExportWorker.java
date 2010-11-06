package kirjanpito.models;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import javax.swing.SwingWorker;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.Registry;

/**
 * <code>SwingWorker</code>, joka tallentaa tilikauden
 * viennit CSV-tiedostoon.
 */
public class CSVExportWorker extends SwingWorker<Void, Void> {
	private Registry registry;
	private File file;
	private SimpleDateFormat dateFormat;
	private DecimalFormat numberFormat;
	private IOException exception;
	
	public CSVExportWorker(Registry registry, File file) {
		this.registry = registry;
		this.file = file;
		dateFormat = new SimpleDateFormat("d.M.yyyy");
		numberFormat = new DecimalFormat();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
	}
	
	protected Void doInBackground() throws Exception {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			export(sess);
			sess.commit();
		}
		catch (Exception e) {
			e.printStackTrace();
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			sess.close();
		}
		
		return null;
	}
	
	private void export(Session sess) throws IOException,
		DataAccessException {
		
		DataSource dataSource = registry.getDataSource();
		Period period = registry.getPeriod();
		
		List<Document> documents = dataSource.getDocumentDAO(
				sess).getByPeriodId(period.getId(), 1);
		
		final HashMap<Integer, Document> documentMap =
			new HashMap<Integer, Document>();
		
		for (Document document : documents) {
			documentMap.put(document.getId(), document);
		}
		
		documents = null;
		
		final CSVWriter writer = new CSVWriter(new FileWriter(file));
		writer.writeField("Tosite");
		writer.writeField("Päivämäärä");
		writer.writeField("Nro");
		writer.writeField("Tili");
		writer.writeField("Debet");
		writer.writeField("Kredit");
		writer.writeField("Selite");
		writer.writeLine();
		
		dataSource.getEntryDAO(sess).getByPeriodId(period.getId(),
			EntryDAO.ORDER_BY_DOCUMENT_NUMBER, new DTOCallback<Entry>() {
			public void process(Entry entry) {
				Document document = documentMap.get(entry.getDocumentId());
				if (document == null) return;
				Account account = registry.getAccountById(entry.getAccountId());
				
				try {
					writer.writeField(Integer.toString(document.getNumber()));
					writer.writeField(dateFormat.format(document.getDate()));
					writer.writeField(account.getNumber());
					writer.writeField(account.getName());
					writer.writeField(entry.isDebit() ?
							numberFormat.format(entry.getAmount()) : "");
					writer.writeField(!entry.isDebit() ?
							numberFormat.format(entry.getAmount()) : "");
					writer.writeField(entry.getDescription());
					writer.writeLine();
				}
				catch (IOException e) {
					exception = e;
				}
			}
		});
		
		writer.close();
		
		if (exception != null)
			throw exception;
		
		setProgress(100);
	}
}
