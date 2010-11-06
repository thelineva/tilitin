package kirjanpito.models;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import kirjanpito.db.Account;
import kirjanpito.db.AccountDAO;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Session;
import kirjanpito.ui.Kirjanpito;
import kirjanpito.util.Registry;

public class VATChangeWorker extends SwingWorker<Integer, Void> {
	private Registry registry;
	private VATChangeModel model;
	private Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	
	public VATChangeWorker(Registry registry, VATChangeModel model) {
		this.registry = registry;
		this.model = model;
	}

	@Override
	protected Integer doInBackground() throws Exception {
		DataSource dataSource = registry.getDataSource();
		dataSource.backup();
		Session sess = null;
		double accountCount = registry.getAccounts().size();
		int changes = 0;
		int index = 0;
		
		try {
			sess = dataSource.openSession();
			AccountDAO dao = dataSource.getAccountDAO(sess);
			
			for (Account account : registry.getAccounts()) {
				if (model.updateAccount(account)) {
					dao.save(account);
					dao.save(model.getNewAccount());
					changes++;
				}
				
				if (isCancelled()) {
					sess.rollback();
					return null;
				}
				
				setProgress((int)(++index / accountCount * 100.0));
			}
			
			sess.commit();
			registry.fetchChartOfAccounts(sess);
		}
		catch (DataAccessException e) {
			logger.log(Level.SEVERE, "Virhe", e);
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			sess.close();
		}
		
		return changes;
	}
}
