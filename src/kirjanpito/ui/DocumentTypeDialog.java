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
import javax.swing.table.TableColumn;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DocumentType;
import kirjanpito.models.DocumentTypeModel;
import kirjanpito.models.DocumentTypeTableModel;

public class DocumentTypeDialog extends JDialog {
	private DocumentTypeModel model;
	private JTable table;
	private DocumentTypeTableModel tableModel;
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME); 
	private static final long serialVersionUID = 1L;
	
	public DocumentTypeDialog(Frame owner, DocumentTypeModel model) {
		super(owner, "Tositelajit", true);
		this.model = model;
	}

	/**
	 * Päivittää käyttäjän syöttämät tiedot <code>DocumentTypeModel</code>ille.
	 * 
	 * @return <code>true</code>, jos tietojen päivittäminen onnistui
	 */
	public boolean updateModel() {
		int count = model.getDocumentTypeCount();
		DocumentType type;
		
		for (int i = 0; i < count; i++) {
			type = model.getDocumentType(i);
			
			if (type.getNumber() < 1) {
				SwingUtils.showErrorMessage(this, "Virheellinen numero.");
				return false;
			}
			
			if (type.getName().length() == 0) {
				SwingUtils.showErrorMessage(this, "Syötä tositelajin nimi ennen tallentamista.");
				return false;
			}
			
			if (type.getNumberStart() < 1) {
				SwingUtils.showErrorMessage(this, "Virheellinen tositenumerovälin alku.");
				return false;
			}
			
			if (type.getNumberEnd() < 1) {
				SwingUtils.showErrorMessage(this, "Virheellinen tositenumerovälin loppu.");
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
		
		menu = new JMenu("Tositelajit");
		menu.setMnemonic('T');
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
		menu.add(SwingUtils.createMenuItem("Tallenna", "save-16x16.png", 'a',
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
		tableModel = new DocumentTypeTableModel();
		tableModel.setModel(model);
		
		table = new JTable(tableModel);
		table.setFillsViewportHeight(true);
		table.setPreferredScrollableViewportSize(new Dimension(400, 250));
		table.setRowHeight(24);
		
		TableColumn column;
		int[] widths = new int[] {80, 140, 80, 80};
		
		for (int i = 0; i < widths.length; i++) {
			column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(widths[i]);
		}
		
		/* Muutetaan enter-näppäimen toiminta. */
		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextCell");
		
		table.getActionMap().put("nextCell", nextCellAction);
		
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
					String message = "Tositelajien hakeminen epäonnistui";
					logger.log(Level.SEVERE, message, e);
				}
			}
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
	 * Lisää vientimallin.
	 */
	public void addEntryTemplate() {
		int index = model.addDocumentType();
		tableModel.fireTableRowsInserted(index, index);
		table.requestFocusInWindow();
		table.changeSelection(index, 1, false, false);
	}
	
	/**
	 * Poistaa valitun vientimallin.
	 */
	public void removeEntryTemplate() {
		int index = table.getSelectedRow();
		
		if (index < 0) {
			return;
		}

		model.removeDocumentType(index);
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
	private ActionListener addRowListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			addEntryTemplate();
		}
	};
	
	/* Poista */
	private ActionListener removeRowListener = new ActionListener() {
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
			
			if (table.isEditing())
				table.getCellEditor().stopCellEditing();
			
			if (column < 4) {
				table.changeSelection(row, column + 1, false, false);
				table.editCellAt(row, column + 1);
			}
		}
	};
}