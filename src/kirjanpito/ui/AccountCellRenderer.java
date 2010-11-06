package kirjanpito.ui;

import javax.swing.table.DefaultTableCellRenderer;

import kirjanpito.db.Account;
import kirjanpito.util.Registry;

/**
 * <code>TableCellRenderer</code>in toteuttava luokka, joka näyttää viennin
 * tilin numeron ja nimen.
 * 
 * @author Tommi Helineva
 */
public class AccountCellRenderer extends DefaultTableCellRenderer {
	private Registry registry;

	private static final long serialVersionUID = 1L;
	
	public AccountCellRenderer(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Asettaa tekstiksi tilin numeron ja nimen.
	 * 
	 * @param obj <code>null</code> tai tilinumero
	 */
	protected void setValue(Object obj) {
		if (obj == null) {
			setText(null);
		}
		else {
			Integer accountId = (Integer)obj;
			Account account = registry.getAccountById(accountId);
			
			if (account == null) {
				setText(null);
			}
			else {
				setText(account.getNumber() + " " + account.getName());
			}
		}
	}
}