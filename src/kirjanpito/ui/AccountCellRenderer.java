package kirjanpito.ui;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import kirjanpito.db.Account;
import kirjanpito.ui.resources.Resources;
import kirjanpito.util.Registry;

/**
 * <code>TableCellRenderer</code>in toteuttava luokka, joka näyttää viennin
 * tilin numeron ja nimen.
 * 
 * @author Tommi Helineva
 */
public class AccountCellRenderer extends DefaultTableCellRenderer {
	private Registry registry;
	private TableModel tableModel;
	private Image icon;
	private boolean iconVisible;

	private static final long serialVersionUID = 1L;
	
	public AccountCellRenderer(Registry registry, TableModel tableModel) {
		this.registry = registry;
		this.tableModel = tableModel;
		icon = new ImageIcon(Resources.load("bullet-12x12.png")).getImage();
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
			Integer row = (Integer)obj;
			Integer accountId = (Integer)tableModel.getValueAt(row, -1);
			iconVisible = Boolean.TRUE.equals(tableModel.getValueAt(row, -2));
			Account account = registry.getAccountById(accountId);
			
			if (account == null) {
				setText(null);
			}
			else {
				setText(account.getNumber() + " " + account.getName());
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (iconVisible) {
			int x = getWidth() - 20;
			int y = (getHeight() - 12) / 2;
			g.drawImage(icon, x, y, null);
		}
	}
}