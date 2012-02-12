package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import kirjanpito.db.Account;
import kirjanpito.db.DataAccessException;
import kirjanpito.models.EntryTemplateModel;
import kirjanpito.models.EntryTemplateTableModel;
import kirjanpito.util.Registry;

/**
 * Vientimallien muokkausikkuna.
 * 
 * @author Tommi Helineva
 */
public class EntryTemplateDialog extends JDialog implements AccountSelectionListener {
	private Registry registry;
	private EntryTemplateModel model;
	private JTable table;
	private EntryTemplateTableModel tableModel;
	private AccountCellRenderer accountCellRenderer;
	private AccountCellEditor accountCellEditor;
	private AccountSelectionDialog accountSelectionDialog;
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME); 
	private static final long serialVersionUID = 1L;
	
	public EntryTemplateDialog(Frame owner, Registry registry, EntryTemplateModel model) {
		super(owner, "Vientimallit", true);
		this.registry = registry;
		this.model = model;
	}

	/**
	 * Päivittää käyttäjän syöttämät tiedot <code>EntryTemplateModel</code>ille.
	 * 
	 * @return <code>true</code>, jos tietojen päivittäminen onnistui
	 */
	public boolean updateModel() {
		int count = model.getEntryTemplateCount();
		
		for (int i = 0; i < count; i++) {
			if (model.getEntryTemplate(i).getAccountId() < 1) {
				SwingUtils.showErrorMessage(this, "Valitse tili ennen tallentamista.");
				return false;
			}
			
			if (model.getEntryTemplate(i).getAmount() == null) {
				SwingUtils.showErrorMessage(this, "Syötä rahamäärä ennen tallentamista.");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
		
		createMenuBar();
		createToolBar();
		createTable();
		
		pack();
		setLocationRelativeTo(null);
		table.requestFocusInWindow();
	}
	
	/**
	 * Luo ikkunan valikot.
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		
		menu = new JMenu("Vientimallit");
		menu.setMnemonic('s');
		menuBar.add(menu);
		
		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
		menu.add(SwingUtils.createMenuItem("Lisää",
				"list-add-16x16.png", 'L',
				KeyStroke.getKeyStroke('N', shortcutKeyMask),
				addRowListener));
		
		menu.add(SwingUtils.createMenuItem("Poista",
				"list-remove-16x16.png", 'P',
				null, removeRowListener));
		
		menu.addSeparator();
		menu.add(SwingUtils.createMenuItem("Tallenna", "save-16x16.png", 'T',
				KeyStroke.getKeyStroke('S', shortcutKeyMask),
				saveListener));
		
		menu.add(SwingUtils.createMenuItem("Sulje", "close-16x16.png", 'S',
				KeyStroke.getKeyStroke('W', shortcutKeyMask),
				closeListener));
		
		setJMenuBar(menuBar);
	}
	
	private void createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		toolBar.add(SwingUtils.createToolButton("close-22x22.png",
				"Sulje", closeListener, true));

		toolBar.add(SwingUtils.createToolButton("save-22x22.png",
				"Tallenna", saveListener, true));

		toolBar.addSeparator();
		toolBar.add(SwingUtils.createToolButton("list-add-22x22.png",
				"Lisää", addRowListener, true));

		toolBar.add(SwingUtils.createToolButton("list-remove-22x22.png",
				"Poista", removeRowListener, true));

		add(toolBar, BorderLayout.NORTH);
	}

	/**
	 * Luo tilikarttataulukon.
	 * 
	 * @param container taulukon säiliö
	 */
	private void createTable() {
		tableModel = new EntryTemplateTableModel();
		tableModel.setModel(model);
		
		accountCellRenderer = new AccountCellRenderer(registry, tableModel);
		accountCellEditor = new AccountCellEditor(registry, tableModel,
				new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				String q = accountCellEditor.getTextField().getText();
				showAccountSelection(q);
			}
		});
		
		table = new JTable(tableModel);
		table.setFillsViewportHeight(true);
		table.setPreferredScrollableViewportSize(new Dimension(620, 250));
		table.setRowHeight(24);
		
		TableColumn column;
		int[] widths = new int[] {50, 120, 190, 80, 80, 190};
		
		TableCellRenderer[] renderers = new TableCellRenderer[] {
			null, null, accountCellRenderer,
			new CurrencyCellRenderer(), new CurrencyCellRenderer(),
			null };
		
		TableCellEditor[] editors = new TableCellEditor[] {
			null, null, accountCellEditor, new CurrencyCellEditor(),
			new CurrencyCellEditor(), null };
		
		for (int i = 0; i < widths.length; i++) {
			column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(widths[i]);
			
			if (renderers[i] != null) {
				column.setCellRenderer(renderers[i]);
			}
			
			if (editors[i] != null) {
				column.setCellEditor(editors[i]);
			}
		}
		
		/* Muutetaan enter-näppäimen toiminta. */
		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextCell");
		
		table.getActionMap().put("nextCell", nextCellAction);

		/* Poistetaan rivi, kun delete-näppäintä painetaan. */
		table.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeRow");

		table.getActionMap().put("removeRow", removeRowListener);

		add(new JScrollPane(table,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
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
			else {
				try {
					model.discardChanges();
				}
				catch (DataAccessException e) {
					String message = "Vientimallien hakeminen epäonnistui";
					logger.log(Level.SEVERE, message, e);
				}
			}
		}
		
		if (accountSelectionDialog != null) {
			accountSelectionDialog.dispose();
		}
		
		dispose();
	}
	
	/**
	 * Tallentaa vientimallit.
	 */
	public void save() {
		trySave();
	}
	
	private boolean trySave() {
		if (!updateModel()) {
			return false;
		}
		
		try {
			model.save();
		}
		catch (DataAccessException e) {
			String message = "Vientimallien tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Näyttää tilinvalintaikkunan ja hakee tilikartasta tilin
	 * hakusanalla <code>q</code>.
	 * 
	 * @param q hakusana
	 */
	public void showAccountSelection(String q) {
		if (accountSelectionDialog == null) {
			accountSelectionDialog = new AccountSelectionDialog(
					this, registry);
			
			accountSelectionDialog.setListener(this);
			accountSelectionDialog.create();
		}
		
		if (table.isEditing())
			table.getCellEditor().stopCellEditing();
		
		accountSelectionDialog.setSearchPhrase(q);
		accountSelectionDialog.setVisible(true);
	}
	
	/**
	 * Päivittää valittuun vientiin käyttäjän valitseman tilin.
	 */
	public void accountSelected() {
		Account account = accountSelectionDialog.getSelectedAccount();
		int index = table.getSelectedRow();
		model.updateAccountId(index, account.getId());
		accountSelectionDialog.setVisible(false);
	}
	
	/**
	 * Lisää vientimallin.
	 */
	public void addEntryTemplate() {
		int index = model.addEntryTemplate();
		tableModel.fireTableRowsInserted(index, index);
		table.requestFocusInWindow();
		table.changeSelection(index, 2, false, false);
	}
	
	/**
	 * Poistaa valitun vientimallin.
	 */
	public void removeEntryTemplate() {
		int index = table.getSelectedRow();
		
		if (index < 0) {
			return;
		}

		model.removeEntryTemplate(index);
		tableModel.fireTableRowsDeleted(index, index);
		table.requestFocusInWindow();

		if (index >= 1) {
			table.setRowSelectionInterval(index - 1, index - 1);
		}
		else if (tableModel.getRowCount() > 0) {
			table.setRowSelectionInterval(0, 0);
		}
	}
	
	/* Lisää */
	private AbstractAction addRowListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			addEntryTemplate();
		}
	};
	
	/* Poista */
	private AbstractAction removeRowListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			removeEntryTemplate();
		}
	};
	
	/* Tallenna */
	private ActionListener saveListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			save();
		}
	};
	
	/* Sulje */
	private ActionListener closeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			close();
		}
	};
	
	private AbstractAction nextCellAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int column = table.getSelectedColumn();  
			int row = table.getSelectedRow();
			boolean changed = false;
			
			if (table.isEditing())
				table.getCellEditor().stopCellEditing();
			
			if (column == 0 || column == 1) {
				column++;
				changed = true;
			}
			/* Tilisarakkeesta siirrytään debet- tai kreditsarakkeeseen. */
			else if (column == 2) {
				column = model.getEntryTemplate(row).isDebit() ? 3 : 4;
				changed = true;
			}
			/* Kredit- ja debetsarakkeesta siirrytään
			 * selitesarakkeeseen. */
			else if (column == 3 || column == 4) {
				BigDecimal amount = model.getEntryTemplate(row).getAmount();
				if (column == 3 && (amount == null ||
						BigDecimal.ZERO.compareTo(amount) == 0)) {
					column = 4;
				}
				else {
					column = 5;
				}
				
				changed = true;
			}
			
			if (changed) {
				table.changeSelection(row, column, false, false);
				table.editCellAt(row, column);
			}
		}
	};
}
