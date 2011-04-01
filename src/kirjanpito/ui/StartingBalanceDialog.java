package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import kirjanpito.db.DataAccessException;
import kirjanpito.models.StartingBalanceModel;
import kirjanpito.models.StartingBalanceTableModel;

/**
 * Alkusaldojen muokkausikkuna.
 * 
 * @author Tommi Helineva
 */
public class StartingBalanceDialog extends JDialog {
	private StartingBalanceModel model;
	private JMenuItem saveMenuItem;
	private JMenuItem copyMenuItem;
	private JTable accountTable;
	private JLabel assetsTotalLabel;
	private JLabel liabilitiesTotalLabel;
	private StartingBalanceTableModel tableModel;
	private TableRowSorter<StartingBalanceTableModel> rowSorter;
	private DecimalFormat formatter;
	
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME); 
	
	public StartingBalanceDialog(Frame owner, StartingBalanceModel model) {
		super(owner, "Alkusaldot", true);
		this.model = model;
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setLayout(new BorderLayout());
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				accountTable.setRowHeight(getFontMetrics(accountTable.getFont()).getHeight() + 4);
			}
			
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
		
		formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
		
		createMenuBar();
		createTable(getContentPane());
		createTotalRow(getContentPane());
		
		pack();
		setLocationRelativeTo(getOwner());
	}
	
	/**
	 * Luo ikkunan valikot.
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		
		menu = new JMenu("Alkusaldot");
		menu.setMnemonic('s');
		menuBar.add(menu);
		
		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
		saveMenuItem = SwingUtils.createMenuItem("Tallenna", "save-16x16.png", 'T',
				KeyStroke.getKeyStroke('S', shortcutKeyMask),
				saveListener);
		
		copyMenuItem = SwingUtils.createMenuItem("Kopioi edellisen tilikauden loppusaldot", null,
				'K', null, copyListener);
		copyMenuItem.setEnabled(model.isEditable());
		
		menu.add(saveMenuItem);
		menu.add(copyMenuItem);
		
		menu.add(SwingUtils.createMenuItem("Sulje", "close-16x16.png", 'L',
				KeyStroke.getKeyStroke('W', shortcutKeyMask),
				closeListener));
		
		setJMenuBar(menuBar);
		saveMenuItem.setEnabled(model.isEditable());
	}
	
	/**
	 * Luo tilikarttataulukon.
	 * 
	 * @param container taulukon säiliö
	 */
	private void createTable(Container container) {
		TableColumn column;
		
		tableModel = new StartingBalanceTableModel();
		tableModel.setModel(model);
		tableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent arg0) {
				updateTotalRow();
			}
		});
		rowSorter = new TableRowSorter<StartingBalanceTableModel>(tableModel);
		rowSorter.setSortable(2, false);
		
		accountTable = new JTable(tableModel);
		accountTable.setFillsViewportHeight(true);
		accountTable.setPreferredScrollableViewportSize(
				new Dimension(500, 350));
		accountTable.setRowSorter(rowSorter);
		
		column = accountTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(60);
		
		column = accountTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(400);
		
		column = accountTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(80);
		column.setCellRenderer(new CurrencyCellRenderer());
		column.setCellEditor(new CurrencyCellEditor());
		
		container.add(new JScrollPane(accountTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
	}
	
	/**
	 * Luo summarivin.
	 * 
	 * @param container paneeli, johon rivi lisätään
	 */
	private void createTotalRow(Container container) {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();
		c1.ipadx = 8;
		c1.anchor = GridBagConstraints.WEST;
		
		GridBagConstraints c2 = new GridBagConstraints();
		c2.ipadx = 8;
		c2.anchor = GridBagConstraints.WEST;
		c2.weightx = 1.0;
		
		JPanel panel = new JPanel(layout);
		panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 5, 2));
		container.add(panel, BorderLayout.SOUTH);
		
		assetsTotalLabel = new JLabel();
		liabilitiesTotalLabel = new JLabel();
		
		panel.add(new JLabel("Vastaavaa yht."), c1);
		panel.add(assetsTotalLabel, c2);
		panel.add(new JLabel("Vastattavaa yht."), c1);
		panel.add(liabilitiesTotalLabel, c2);
		
		updateTotalRow();
	}
	
	private void updateTotalRow() {
		assetsTotalLabel.setText(
				formatter.format(model.getAssetsTotal()));
		liabilitiesTotalLabel.setText(
				formatter.format(model.getLiabilitiesTotal()));
	}
	
	/**
	 * Sulkee ikkunan.
	 */
	public void close() {
		if (model.isChanged()) {
			int result = JOptionPane.showConfirmDialog(this,
					"Tallennetaanko muutokset?", Kirjanpito.APP_NAME,
					JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.YES_NO_CANCEL_OPTION);
			
			if (result == JOptionPane.YES_OPTION) {
				/* Ikkunaa ei suljeta, jos tallentaminen
				 * epäonnistuu. */
				if (!trySave()) return;
			}
			else if (result == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}
		
		dispose();
	}
	
	/**
	 * Tallentaa tilien alkusaldot.
	 */
	public void save() {
		trySave();
	}
	
	private void copyFromPreviousPeriod() {
		try {
			if (!model.copyFromPreviousPeriod()) {
				SwingUtils.showInformationMessage(this,
						"Edellisen tilikauden tietoja ei löytynyt.");
				return;
			}
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}
		
		tableModel.fireTableDataChanged();
	}
	
	private boolean trySave() {
		try {
			model.save();
		}
		catch (DataAccessException e) {
			String message = "Alkusaldojen tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return false;
		}
		
		return true;
	}
	
	/* Tallenna */
	private ActionListener saveListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			save();
		}
	};
	
	/* Kopioi edelliseltä tilikaudelta */
	private ActionListener copyListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			copyFromPreviousPeriod();
		}
	};
	
	/* Sulje */
	private ActionListener closeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			close();
		}
	};
}
