package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;
import kirjanpito.db.DataAccessException;
import kirjanpito.models.COAModel;
import kirjanpito.models.EditableCOATableModel;
import kirjanpito.ui.resources.Resources;
import kirjanpito.util.CSVReader;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.Registry;

/**
 * Tilikartan muokkausikkuna.
 *
 * @author Tommi Helineva
 */
public class COADialog extends JDialog {
	private Registry registry;
	private COAModel model;
	private JPanel topPanel;
	private JPopupMenu accountPopupMenu;
	private JPopupMenu headingPopupMenu;
	private JCheckBoxMenuItem[] levelMenuItems;
	private JCheckBoxMenuItem[] typeMenuItems;
	private JCheckBoxMenuItem[] codeMenuItems;
	private JMenuItem vatRateMenuItem;
	private JMenuItem vatAccountMenuItem;
	private JButton saveButton;
	private JTable accountTable;
	private JTextField searchTextField;
	private JMenuItem removeMenuItem;
	private JMenuItem saveMenuItem;
	private JToggleButton hideNonFavouriteAccountsButton;
	private JCheckBoxMenuItem hideNonFavouriteAccountsMenuItem;
	private JCheckBoxMenuItem defaultAccountMenuItem;
	private JCheckBoxMenuItem favouriteAccountMenuItem;
	private COATableCellRenderer cellRenderer;
	private EditableCOATableModel tableModel;

	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	private static final long serialVersionUID = 1L;

	public COADialog(Frame owner, Registry registry, COAModel model) {
		super(owner, "Tilikartta", true);
		this.registry = registry;
		this.model = model;
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
		createPopupMenus();
		createSearchPanel();
		createToolBar();
		createTable();
		pack();
		setLocationRelativeTo(getOwner());

		searchTextField.requestFocusInWindow();
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "toggleFavourite");

		rootPane.getActionMap().put("toggleFavourite", toggleFavAccountAction);
	}

	/**
	 * Luo ikkunan valikot.
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;

		menu = new JMenu("Tilikartta");
		menu.setMnemonic('T');
		menuBar.add(menu);

		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		menu.add(SwingUtils.createMenuItem("Lisää tili", "list-add-16x16.png", 'L',
				KeyStroke.getKeyStroke('N', shortcutKeyMask),
				addAccountListener));

		menu.add(SwingUtils.createMenuItem("Lisää otsikko", "list-add-16x16.png", 'O',
				null, addHeadingListener));

		removeMenuItem = SwingUtils.createMenuItem("Poista tili", "list-remove-16x16.png", 'P',
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,
						shortcutKeyMask), removeListener);

		removeMenuItem.setEnabled(false);
		menu.add(removeMenuItem);

		saveMenuItem = SwingUtils.createMenuItem("Tallenna", "save-16x16.png", 'T',
				KeyStroke.getKeyStroke(KeyEvent.VK_S,
						shortcutKeyMask), saveListener);

		saveMenuItem.setEnabled(false);
		menu.add(saveMenuItem);

		hideNonFavouriteAccountsMenuItem = new JCheckBoxMenuItem("Näytä vain suosikkitilit");
		hideNonFavouriteAccountsMenuItem.setMnemonic('s');
		hideNonFavouriteAccountsMenuItem.addActionListener(hideNonFavAccountsListener);
		hideNonFavouriteAccountsMenuItem.setState(model.isNonFavouriteAccountsHidden());
		hideNonFavouriteAccountsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
		menu.add(hideNonFavouriteAccountsMenuItem);

		menu.add(SwingUtils.createMenuItem("Sulje", "close-16x16.png", 'L',
				KeyStroke.getKeyStroke('W', shortcutKeyMask),
				closeListener));

		setJMenuBar(menuBar);
	}

	private void createPopupMenus() {
		/* Luodaan valikko tileille. */
		accountPopupMenu = new JPopupMenu();

		String[] types = new String[] {"Vastaavaa", "Vastattavaa",
				"Oma pääoma", "Tulot", "Menot", "Edellisten tilikausien voitto"};

		typeMenuItems = new JCheckBoxMenuItem[types.length];
		int index = 0;

		for (String type : types) {
			typeMenuItems[index] = new JCheckBoxMenuItem(type);
			typeMenuItems[index].addActionListener(accountTypeListener);
			accountPopupMenu.add(typeMenuItems[index]);
			index++;
		}

		JMenu vatMenu = new JMenu("ALV-koodi");
		accountPopupMenu.addSeparator();
		accountPopupMenu.add(vatMenu);

		vatRateMenuItem = SwingUtils.createMenuItem("Aseta ALV-prosentti", null,
				'r', null, vatPercentListener);

		accountPopupMenu.add(vatRateMenuItem);

		vatAccountMenuItem = SwingUtils.createMenuItem("Valitse ALV-vastatili", null, 'V',
				null, vatAccountListener);

		accountPopupMenu.add(vatAccountMenuItem);

		defaultAccountMenuItem = new JCheckBoxMenuItem("Oletusvastatili");
		defaultAccountMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = accountTable.getSelectedRow();
				if (index < 0) return;
				ChartOfAccounts coa = model.getChartOfAccounts();
				Account account = coa.getAccount(index);

				if (model.getDefaultAccount() == account) {
					model.setDefaultAccount(null);
				}
				else {
					model.setDefaultAccount(coa.getAccount(index));
				}

				setSaveEnabled(true);
			}
		});

		favouriteAccountMenuItem = new JCheckBoxMenuItem("Suosikkitili");
		favouriteAccountMenuItem.setMnemonic('S');
		favouriteAccountMenuItem.addActionListener(toggleFavAccountAction);

		accountPopupMenu.addSeparator();
		accountPopupMenu.add(defaultAccountMenuItem);
		accountPopupMenu.add(favouriteAccountMenuItem);

		String[] codes = new String[] {"---", "Arvonlisäverovelka",
				"Suoritettava ALV", "Vähennettävä ALV",
				"Verollinen myynti", "Verollinen osto",
				"Veroton myynti", "Veroton osto",
				"Yhteisömyynti", "Yhteisöosto",
				"Rakentamispalvelun myynti", "Rakentamispalvelun osto"
		};

		codeMenuItems = new JCheckBoxMenuItem[codes.length];
		index = 0;

		for (String code : codes) {
			codeMenuItems[index] = new JCheckBoxMenuItem(code);
			codeMenuItems[index].addActionListener(accountVatCodeListener);
			vatMenu.add(codeMenuItems[index]);
			index++;
		}

		accountPopupMenu.addSeparator();

		accountPopupMenu.add(SwingUtils.createMenuItem("Lisää tili", "list-add-16x16.png", 'L',
				null, addAccountListener));

		accountPopupMenu.add(SwingUtils.createMenuItem("Lisää otsikko", "list-add-16x16.png", 'O',
				null, addHeadingListener));

		accountPopupMenu.add(SwingUtils.createMenuItem("Poista tili", "list-remove-16x16.png", 'P',
				null, removeListener));

		/* Luodaan valikko otsikkoriveille. */
		headingPopupMenu = new JPopupMenu();

		levelMenuItems = new JCheckBoxMenuItem[6];

		for (int i = 0; i < levelMenuItems.length; i++) {
			levelMenuItems[i] = new JCheckBoxMenuItem("Taso " + (i + 1));
			levelMenuItems[i].addActionListener(levelListener);
			headingPopupMenu.add(levelMenuItems[i]);
		}

		headingPopupMenu.addSeparator();

		headingPopupMenu.add(SwingUtils.createMenuItem("Siirrä ylös", "go-up-16x16.png", 'y',
				null, moveHeadingUpListener));

		headingPopupMenu.add(SwingUtils.createMenuItem("Siirrä alas", "go-down-16x16.png", 'a',
				null, moveHeadingDownListener));

		headingPopupMenu.add(SwingUtils.createMenuItem("Lisää tili", "list-add-16x16.png", 'L',
				null, addAccountListener));

		headingPopupMenu.add(SwingUtils.createMenuItem("Lisää otsikko", "list-add-16x16.png", 'O',
				null, addHeadingListener));

		headingPopupMenu.add(SwingUtils.createMenuItem("Poista otsikko", "list-remove-16x16.png", 'P',
				null, removeListener));
	}

	private void createSearchPanel() {
		topPanel = new JPanel();
		BorderLayout layout = new BorderLayout();
		layout.setHgap(8);
		layout.setVgap(8);
		topPanel.setLayout(layout);
		topPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
		searchTextField = new JTextField();
		searchTextField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				/* Valitaan taulukosta seuraava rivi, kun hakukentässä
				 * painetaan alanuolinäppäintä. */
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					int index = accountTable.getSelectedRow();

					if (index < accountTable.getRowCount() - 1)
						setSelectedRow(index + 1);
				}
				/* Valitaan taulukosta edellinen rivi, kun hakukentässä
				 * painetaan ylänuolinäppäintä. */
				else if (e.getKeyCode() == KeyEvent.VK_UP) {
					int index = accountTable.getSelectedRow();

					if (index > 0)
						setSelectedRow(index - 1);
				}
			}
		});

		JLabel label = new JLabel("Haku");
		label.setDisplayedMnemonic('H');
		label.setLabelFor(searchTextField);

		searchTextField.getDocument().addDocumentListener(
				searchTextFieldListener);
		topPanel.add(label, BorderLayout.LINE_START);
		topPanel.add(searchTextField, BorderLayout.CENTER);
		add(topPanel, BorderLayout.NORTH);
	}

	private void createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		toolBar.add(SwingUtils.createToolButton("close-22x22.png",
				"Sulje", closeListener, true));

		saveButton = SwingUtils.createToolButton("save-22x22.png",
				"Tallenna", saveListener, true);

		saveButton.setEnabled(false);
		toolBar.add(saveButton);
		toolBar.addSeparator();

		toolBar.add(SwingUtils.createToolButton("list-add-22x22.png",
				"Lisää tili", addAccountListener, true));

		toolBar.add(SwingUtils.createToolButton("list-add-22x22.png",
				"Lisää otsikko", addHeadingListener, true));

		toolBar.add(SwingUtils.createToolButton("list-remove-22x22.png",
				"Poista", removeListener, true));

		hideNonFavouriteAccountsButton = new JToggleButton(
				"Vain suosikkitilit", new ImageIcon(Resources.load("favourite-22x22.png")));
		hideNonFavouriteAccountsButton.addActionListener(hideNonFavAccountsListener);
		hideNonFavouriteAccountsButton.setSelected(model.isNonFavouriteAccountsHidden());

		toolBar.addSeparator();
		toolBar.add(hideNonFavouriteAccountsButton);

		topPanel.add(toolBar, BorderLayout.NORTH);
	}

	/**
	 * Luo tilikarttataulukon.
	 *
	 * @param container taulukon säiliö
	 */
	private void createTable() {
		TableColumn column;

		tableModel = new EditableCOATableModel(this, model);
		tableModel.setChartOfAccounts(model.getChartOfAccounts());
		tableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				setSaveEnabled(true);
			}
		});

		accountTable = new JTable(tableModel);
		accountTable.setFillsViewportHeight(true);
		accountTable.setPreferredScrollableViewportSize(
				new Dimension(600, 380));
		accountTable.addMouseListener(mouseListener);
		accountTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		accountTable.getSelectionModel().addListSelectionListener(
				selectionListener);

		accountTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(525, 0), "showMenu");

		accountTable.getActionMap().put("showMenu", showMenuAction);

		accountTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "toggleFavourite");

		accountTable.getActionMap().put("toggleFavourite", toggleFavAccountAction);

		accountTable.setDropMode(DropMode.INSERT_ROWS);
		accountTable.setTransferHandler(transferHandler);
		accountTable.setDragEnabled(true);
		accountTable.setRowHeight(Math.max(16, getFontMetrics(accountTable.getFont()).getHeight()) + 4);
		accountTable.getColumnModel().getColumn(0).setMaxWidth(getFontMetrics(accountTable.getFont())
				.stringWidth(tableModel.getColumnName(0)) + 30);

		cellRenderer = new COATableCellRenderer();
		cellRenderer.setChartOfAccounts(model.getChartOfAccounts());
		cellRenderer.setHighlightFavouriteAccounts(!model.isNonFavouriteAccountsHidden());

		column = accountTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(80);

		column = accountTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(420);
		column.setCellRenderer(cellRenderer);

		add(new JScrollPane(accountTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
	}

	private void showPopupMenu(Component comp, int x, int y) {
		int index = accountTable.getSelectedRow();

		if (index < 0)
			return;

		ChartOfAccounts coa = model.getChartOfAccounts();
		int type = coa.getType(index);

		if (type == ChartOfAccounts.TYPE_ACCOUNT) {
			Account account = coa.getAccount(index);

			/* Valitaan tilin tyyppi. */
			int accountType = account.getType();

			for (int i = 0; i < typeMenuItems.length; i++) {
				typeMenuItems[i].setState(accountType == i);
			}

			/* Valitaan ALV-koodi. */
			int accountVatCode = account.getVatCode();

			for (int i = 0; i < codeMenuItems.length; i++) {
				codeMenuItems[i].setState(accountVatCode == i);
			}

			boolean vatRateEnabled = (accountVatCode == 4 ||
					accountVatCode == 5 || accountVatCode == 9 || accountVatCode == 11);

			vatRateMenuItem.setEnabled(vatRateEnabled);
			vatAccountMenuItem.setEnabled(vatRateEnabled);
			defaultAccountMenuItem.setState(account == model.getDefaultAccount());
			favouriteAccountMenuItem.setState((account.getFlags() & 0x01) != 0);
			accountPopupMenu.show(comp, x, y);
		}
		else {
			int level = coa.getHeading(index).getLevel();

			/* Asetetaan rasti oikeaan tasovaihtoehtoon. */
			for (int i = 0; i < levelMenuItems.length; i++) {
				levelMenuItems[i].setState(i == level);
			}

			headingPopupMenu.show(comp, x, y);
		}
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
					registry.fetchChartOfAccounts();
				}
				catch (DataAccessException e) {
					String message = "Tilikarttatietojen hakeminen epäonnistui";
					logger.log(Level.SEVERE, message, e);
					SwingUtils.showDataAccessErrorMessage(this, e, message);
				}
			}
		}

		dispose();
	}

	/**
	 * Lisää väliotsikon tilikarttaan.
	 */
	public void addHeading() {
		int index = accountTable.getSelectedRow();

		if (index < 0) {
			SwingUtils.showInformationMessage(this,
				"Valitse ensin tili, jonka yläpuolelle uusi otsikko lisätään.");
			return;
		}

		index = model.addHeading(index);
		tableModel.fireTableRowsInserted(index, index);
		accountTable.changeSelection(index, 1, false, false);
		accountTable.requestFocus();
		accountTable.editCellAt(index, 1);
	}

	/**
	 * Lisää tilin tilikarttaan.
	 */
	public void addAccount() {
		int index = accountTable.getSelectedRow();

		if (index < 0) {
			SwingUtils.showInformationMessage(this,
				"Valitse ensin tili, jonka alapuolelle uusi tili lisätään.");
			return;
		}

		index = model.addAccount(index);
		tableModel.fireTableRowsInserted(index, index);
		accountTable.changeSelection(index, 1, false, false);
		accountTable.requestFocus();
		accountTable.editCellAt(index, 1);
	}

	/**
	 * Poistaa käyttäjän valitseman rivin tilikartasta.
	 */
	public void removeRow() {
		int index = accountTable.getSelectedRow();

		if (index < 0) {
			return;
		}

		ChartOfAccounts coa = model.getChartOfAccounts();

		if (coa.getType(index) == ChartOfAccounts.TYPE_ACCOUNT) {
			Account account = coa.getAccount(index);
			int canRemove;

			try {
				canRemove = model.canRemoveAccount(account);
			}
			catch (DataAccessException e) {
				String message = "Tietojen hakeminen epäonnistui";
				logger.log(Level.SEVERE, message, e);
				SwingUtils.showDataAccessErrorMessage(this, e, message);
				return;
			}

			if (canRemove == 1) {
				JOptionPane.showMessageDialog(this,
						"Tiliä ei voi poistaa, koska jokin vienti kohdistuu valittuun tiliin.",
						Kirjanpito.APP_NAME, JOptionPane.OK_OPTION);
				return;
			}
			else if (canRemove == 2) {
				JOptionPane.showMessageDialog(this,
						"Tiliä ei voi poistaa, koska tili on määritelty ALV-vastatiliksi.",
						Kirjanpito.APP_NAME, JOptionPane.OK_OPTION);
				return;
			}
		}

		model.removeRow(index);

		if (model.isNonFavouriteAccountsHidden()) {
			tableModel.fireTableDataChanged();
		}
		else {
			tableModel.fireTableRowsDeleted(index, index);
		}
	}

	/**
	 * Siirtää otsikkoa ylös tai alas.
	 *
	 * @param down alas
	 */
	public void moveHeading(boolean down) {
		int index = accountTable.getSelectedRow();

		if (index < 0) {
			return;
		}

		if (model.isNonFavouriteAccountsHidden()) {
			hideNonFavAccountsListener.actionPerformed(null);
		}

		index = model.moveHeading(index, down);

		if (index >= 0) {
			tableModel.fireTableDataChanged();
			setSelectedRow(index);
		}
	}

	/**
	 * Etsii tilien nimistä ja otsikkoteksteistä käyttäjän
	 * syöttämää merkkijonoa. Ensimmäinen löytynyt rivi valitaan
	 * taulukosta.
	 */
	public void search() {
		int index = model.getChartOfAccounts().search(
				searchTextField.getText());

		if (index >= 0) {
			setSelectedRow(index);
		}
	}

	/**
	 * Päivittää valitun otsikon otsikkotason.
	 *
	 * @param level otsikkotaso
	 */
	public void updateHeadingLevel(int level) {
		ChartOfAccounts coa = model.getChartOfAccounts();
		int index = accountTable.getSelectedRow();

		if (index < 0 || coa.getType(index) !=
				ChartOfAccounts.TYPE_HEADING) {
			return;
		}

		COAHeading heading = coa.getHeading(index);

		if (heading.getLevel() != level) {
			heading.setLevel(level);
			model.updateRow(index, true);
			tableModel.fireTableDataChanged();
			setSaveEnabled(true);
		}
	}

	/**
	 * Päivittää valitun tilin tyypin.
	 *
	 * @param type tilin tyyppi
	 */
	public void updateAccountType(int type) {
		ChartOfAccounts coa = model.getChartOfAccounts();
		int index = accountTable.getSelectedRow();

		if (index < 0 || coa.getType(index) !=
				ChartOfAccounts.TYPE_ACCOUNT) {
			return;
		}

		Account account = coa.getAccount(index);

		if (account.getType() != type) {
			account.setType(type);
			model.updateRow(index, false);
			tableModel.fireTableCellUpdated(index, 1);
			setSaveEnabled(true);
		}
	}

	/**
	 * Päivittää valitun tilin ALV-koodin.
	 *
	 * @param code alv-koodi
	 */
	public void updateVatCode(int code) {
		ChartOfAccounts coa = model.getChartOfAccounts();
		int index = accountTable.getSelectedRow();

		if (index < 0 || coa.getType(index) !=
				ChartOfAccounts.TYPE_ACCOUNT) {
			return;
		}

		Account account = coa.getAccount(index);

		if (account.getVatCode() == code) {
			return;
		}

		account.setVatCode(code);

		/* ALV-prosenttia käytetään vain, jos koodi on
		 * verollinen myynti tai verolliset ostot. */
		if (code < 4 || code > 5) {
			account.setVatRate(BigDecimal.ZERO);
		}
		else {
			account.setVatRate(new BigDecimal("24.00"));
		}

		account.setVatAccount1Id(-1);
		account.setVatAccount2Id(-1);

		if (code >= 4) {
			/* Kopioidaan ALV-vastatilit toiselta tililtä, jolla
			 * on sama ALV-koodi. */
			for (Account a : registry.getAccounts()) {
				if (a == account) {
					continue;
				}

				if (a.getVatCode() == code) {
					account.setVatAccount1Id(a.getVatAccount1Id());
					account.setVatAccount2Id(a.getVatAccount2Id());
					break;
				}
			}

			if (account.getVatAccount1Id() < 0) {
				SwingUtils.showInformationMessage(this, "Valitse ALV-vastatili.");
			}
		}

		model.updateRow(index, false);
		setSaveEnabled(true);
	}

	/**
	 * Päivittää valitun tilin ALV-prosentin.
	 *
	 */
	public void updateVatRate() {
		ChartOfAccounts coa = model.getChartOfAccounts();
		int index = accountTable.getSelectedRow();

		if (index < 0 || coa.getType(index) !=
				ChartOfAccounts.TYPE_ACCOUNT) {
			return;
		}

		Account account = coa.getAccount(index);
		DecimalFormat formatter = new DecimalFormat();
		formatter.setParseBigDecimal(true);
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
		BigDecimal percentage = null;

		while (percentage == null) {
			Object result = JOptionPane.showInputDialog(this,
					"Syötä ALV-prosentti.", "ALV-prosentti",
					JOptionPane.QUESTION_MESSAGE, null, null,
					formatter.format(account.getVatRate()));

			if (result == null) {
				return;
			}

			try {
				percentage = (BigDecimal)formatter.parse(result.toString());
			}
			catch (ParseException e) {
				continue;
			}

			if (percentage.compareTo(BigDecimal.ZERO) < 0 ||
					percentage.compareTo(new BigDecimal("100")) > 0) {
				SwingUtils.showInformationMessage(this,
						"ALV-prosentin on oltava vähintään 0 ja korkeintaan 100.");
				percentage = null;
			}
		}

		if (percentage.compareTo(account.getVatRate()) != 0) {
			account.setVatRate(percentage);
			model.updateRow(index, false);
			tableModel.fireTableCellUpdated(index, 1);
			setSaveEnabled(true);

			if (account.getVatAccount1Id() < 0) {
				SwingUtils.showInformationMessage(this, "Valitse ALV-vastatili.");
			}
		}
	}

	/**
	 * Päivittää valitun tilin ALV-vastatilin.
	 */
	public void updateVatAccount() {
		ChartOfAccounts coa = model.getChartOfAccounts();
		int index = accountTable.getSelectedRow();

		if (index < 0 || coa.getType(index) !=
				ChartOfAccounts.TYPE_ACCOUNT) {
			return;
		}

		ArrayList<Account> vatAccounts = new ArrayList<Account>();
		vatAccounts.add(null);

		for (Account account : registry.getAccounts()) {
			if (account.getVatCode() == 2 || account.getVatCode() == 3) {
				vatAccounts.add(account);
			}
		}

		Account account = coa.getAccount(index);
		String[] vatAccountTexts = new String[vatAccounts.size()];
		vatAccountTexts[0] = "---";
		int vatAccount1Index = 0;
		int vatAccount2Index = 0;

		for (int i = 1; i < vatAccountTexts.length; i++) {
			Account vatAccount = vatAccounts.get(i);
			vatAccountTexts[i] = vatAccount.getNumber() + " " + vatAccount.getName();

			if (vatAccount.getId() == account.getVatAccount1Id()) {
				vatAccount1Index = i;
			}

			if (vatAccount.getId() == account.getVatAccount2Id()) {
				vatAccount2Index = i;
			}
		}

		int vatAccount1Id;
		int vatAccount2Id = -1;
		Object result;

		result = JOptionPane.showInputDialog(this,
				"Valitse ALV-vastatili.",
				"ALV-vastatili", JOptionPane.QUESTION_MESSAGE, null,
				vatAccountTexts, vatAccountTexts[vatAccount1Index]);

		vatAccount1Id = findVatAccount(result, vatAccountTexts, vatAccounts);

		if (vatAccount1Id == -2) {
			return;
		}

		if (account.getVatCode() == 9 || account.getVatCode() == 11) {
			result = JOptionPane.showInputDialog(this,
					"Valitse ALV-vastatili (2).",
					"ALV-vastatili (2)", JOptionPane.QUESTION_MESSAGE, null,
					vatAccountTexts, vatAccountTexts[vatAccount2Index]);

			vatAccount2Id = findVatAccount(result, vatAccountTexts, vatAccounts);

			if (vatAccount2Id == -2) {
				return;
			}
		}

		account.setVatAccount1Id(vatAccount1Id);
		account.setVatAccount2Id(vatAccount2Id);
		model.updateRow(coa.indexOfAccount(account), false);
		setSaveEnabled(true);
	}

	private int findVatAccount(Object result, String[] texts, ArrayList<Account> vatAccounts) {
		if (result == null) {
			return -2;
		}

		if (result == texts[0]) {
			return -1;
		}

		for (int i = 1; i < texts.length; i++) {
			if (result == texts[i]) {
				return vatAccounts.get(i).getId();
			}
		}

		return -1;
	}

	/**
	 * Tallentaa tilikarttaan tehdyt muutokset.
	 */
	public void save() {
		trySave();
	}

	/**
	 * Tallentaa tilikarttaan tehdyt muutokset.
	 *
	 * @return <code>true</code>, jos tallentaminen onnistuu;
	 * <code>false</code>, jos tallentaminen epäonnistuu
	 */
	private boolean trySave() {
		try {
			model.save();
		}
		catch (DataAccessException e) {
			String message = "Tilikartan tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return false;
		}

		setSaveEnabled(false);
		return true;
	}

	/**
	 * Asettaa valitun rivin.
	 *
	 * @param position rivinumero
	 */
	private void setSelectedRow(int position) {
		accountTable.getSelectionModel().setSelectionInterval(
				position, position);

		/* Vieritetään taulukkoa niin, että valittu rivi on näkyvissä. */
		Rectangle rect = accountTable.getCellRect(position, 0, true);
		accountTable.scrollRectToVisible(rect);
	}

	private void setSaveEnabled(boolean enabled) {
		saveMenuItem.setEnabled(enabled);
		saveButton.setEnabled(enabled);
	}

	/* Lisää tili */
	private ActionListener addAccountListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			addAccount();
		}
	};

	/* Lisää otsikko */
	private ActionListener addHeadingListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			addHeading();
		}
	};

	/* Poista tili / Poista otsikko */
	private ActionListener removeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			removeRow();
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

	/* Taso X */
	private ActionListener levelListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int level = -1;

			for (int i = 0; i < levelMenuItems.length; i++) {
				if (e.getSource() == levelMenuItems[i]) {
					level = i;
					break;
				}
			}

			updateHeadingLevel(level);
		}
	};

	/* Siirrä ylös */
	private ActionListener moveHeadingUpListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			moveHeading(false);
		}
	};

	/* Siirrä alas */
	private ActionListener moveHeadingDownListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			moveHeading(true);
		}
	};

	/* Tilityyppivaihtoehto */
	private ActionListener accountTypeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int type = -1;

			/* Tarkistetaan, mitä vaihtoehtoa valikosta on klikattu. */
			for (int i = 0; i < typeMenuItems.length; i++) {
				if (typeMenuItems[i] == e.getSource()) {
					type = i;
					break;
				}
			}

			updateAccountType(type);
		}
	};

	/* ALV-koodivaihtoehto */
	private ActionListener accountVatCodeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int code = -1;

			/* Tarkistetaan, mitä vaihtoehtoa valikosta on klikattu. */
			for (int i = 0; i < codeMenuItems.length; i++) {
				if (codeMenuItems[i] == e.getSource()) {
					code = i;
					break;
				}
			}

			updateVatCode(code);
		}
	};

	/* ALV-prosentti */
	private ActionListener vatPercentListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateVatRate();
		}
	};

	/* ALV-vastatili */
	private ActionListener vatAccountListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateVatAccount();
		}
	};

	private AbstractAction showMenuAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int index = accountTable.getSelectedRow();

			if (index < 0)
				return;

			Rectangle rect = accountTable.getCellRect(index, 1, false);
			showPopupMenu(accountTable, rect.x + 15, rect.y + 15);
		}
	};

	private ActionListener hideNonFavAccountsListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			boolean enabled = !model.isNonFavouriteAccountsHidden();
			hideNonFavouriteAccountsButton.setSelected(enabled);
			hideNonFavouriteAccountsMenuItem.setSelected(enabled);
			model.setNonFavouriteAccountsHidden(enabled);
			cellRenderer.setHighlightFavouriteAccounts(!enabled);
			tableModel.fireTableDataChanged();
			search();
		}
	};

	private AbstractAction toggleFavAccountAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int index = accountTable.getSelectedRow();
			if (index < 0) return;
			ChartOfAccounts coa = model.getChartOfAccounts();

			if (coa.getType(index) != ChartOfAccounts.TYPE_ACCOUNT) {
				return;
			}

			Account account = coa.getAccount(index);
			int flags = account.getFlags();

			if ((flags & 0x01) != 0) {
				account.setFlags(flags & ~0x01);
			}
			else {
				account.setFlags(flags | 0x01);
			}

			if (model.isNonFavouriteAccountsHidden()) {
				model.updateRow(index, true);
				tableModel.fireTableDataChanged();
			}
			else {
				model.updateRow(index, false);
				tableModel.fireTableRowsUpdated(index, index);
			}

			setSaveEnabled(true);
		}
	};

	/* Näytetään taulukon päällä popup-valikko, kun hiiren
	 * oikeaa painiketta klikataan. */
	private MouseListener mouseListener = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (!e.isPopupTrigger())
				return;

			showPopupMenu(e.getComponent(), e.getX(), e.getY());
		}
	};

	/* Päivitetään valikkotekstit, kun taulukon valinta muuttuu */
	private ListSelectionListener selectionListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			int index = e.getFirstIndex();

			if (index >= tableModel.getRowCount())
				index = -1;

			removeMenuItem.setEnabled(index >= 0);

			if (index < 0)
				return;

			int type = model.getChartOfAccounts().getType(index);

			if (type == ChartOfAccounts.TYPE_ACCOUNT) {
				removeMenuItem.setText("Poista tili");
			}
			else {
				removeMenuItem.setText("Poista otsikko");
			}
		}
	};

	private DocumentListener searchTextFieldListener = new DocumentListener() {
		public void changedUpdate(DocumentEvent e) { }

		public void insertUpdate(DocumentEvent e) {
			search();
		}

		public void removeUpdate(DocumentEvent e) {
			search();
		}
	};

	private TransferHandler transferHandler = new TransferHandler() {
		private static final long serialVersionUID = 1L;
		private boolean dragging;

		public int getSourceActions(JComponent c) {
			return COPY;
		}

		public Transferable createTransferable(JComponent c) {
			StringWriter writer = new StringWriter();
			CSVWriter csv = new CSVWriter(writer);
			ChartOfAccounts coa = model.getChartOfAccounts();
			ListSelectionModel selectionModel = accountTable.getSelectionModel();
			DecimalFormat formatter = new DecimalFormat();
			formatter.setMinimumFractionDigits(0);
			formatter.setMaximumFractionDigits(2);
			int len = accountTable.getRowCount();

			for (int i = 0; i < len; i++) {
				if (selectionModel.isSelectedIndex(i)) {
					if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT) {
						Account account = coa.getAccount(i);

						try {
							csv.writeField("A");
							csv.writeField(account.getNumber());
							csv.writeField(account.getName());
							csv.writeField(Integer.toString(account.getType()));
							csv.writeField(Integer.toString(account.getVatCode()));
							csv.writeField(formatter.format(account.getVatRate()));
							csv.writeField(accountIdToString(account.getVatAccount1Id()));
							csv.writeField(accountIdToString(account.getVatAccount2Id()));
							csv.writeLine();
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					else {
						COAHeading heading = coa.getHeading(i);

						try {
							csv.writeField("H");
							csv.writeField(heading.getNumber());
							csv.writeField(heading.getText());
							csv.writeField(Integer.toString(heading.getLevel()));
							csv.writeLine();
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			dragging = true;
			return new StringSelection(writer.toString());
		}

		public boolean canImport(TransferSupport support) {
			if (dragging) {
				return false;
			}

			if (!support.isDrop()) {
				return false;
			}

			if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return false;
			}

			return true;
		}


		protected void exportDone(JComponent c, Transferable data, int action) {
			super.exportDone(c, data, action);
			dragging = false;
		}

		public boolean importData(TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}

			String data;

			try {
				data = (String) support.getTransferable().getTransferData(
						DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException e) {
				return false;
			}
			catch (IOException e) {
				return false;
			}

			StringReader reader = new StringReader(data);
			CSVReader csv = new CSVReader(reader);
			ChartOfAccounts coa = model.getChartOfAccounts();
			String[] fields = null;
			DecimalFormat formatter = new DecimalFormat();
			formatter.setParseBigDecimal(true);

			try {
				while ((fields = csv.readLine()) != null) {
					if (fields[0].equals("A")) {
						if (fields.length != 8) throw new NumberFormatException();
						Account account = null;
						boolean positionChanged = false;
						int index = -1;

						for (int i = 0; i < coa.getSize(); i++) {
							if (coa.getType(i) == ChartOfAccounts.TYPE_ACCOUNT &&
									fields[1].equals(coa.getAccount(i).getNumber())) {
								account = coa.getAccount(i);
								index = i;
							}
						}

						if (account == null) {
							index = model.addAccount(0);
							account = coa.getAccount(index);
							account.setNumber(fields[1]);
							positionChanged = true;
						}

						account.setName(fields[2]);
						account.setType(Integer.parseInt(fields[3]));

						if (account.getType() < Account.TYPE_ASSET ||
								account.getType() > Account.TYPE_EXPENSE) {
							throw new NumberFormatException();
						}

						account.setVatCode(Integer.parseInt(fields[4]));

						if (account.getVatCode() < 0 ||
								account.getVatCode() >= codeMenuItems.length) {
							throw new NumberFormatException();
						}

						account.setVatRate((BigDecimal)formatter.parse(fields[5]));
						account.setVatAccount1Id(findAccount(fields[6]));
						account.setVatAccount2Id(findAccount(fields[7]));
						model.updateRow(index, positionChanged);
					}
					else if (fields[0].equals("H")) {
						if (fields.length != 4) throw new NumberFormatException();
						COAHeading heading = null;
						boolean positionChanged = false;
						int level = Integer.parseInt(fields[3]);
						int index = -1;

						for (int i = 0; i < coa.getSize(); i++) {
							if (coa.getType(i) == ChartOfAccounts.TYPE_HEADING &&
									fields[1].equals(coa.getHeading(i).getNumber()) &&
									coa.getHeading(i).getLevel() == level) {
								heading = coa.getHeading(i);
								index = i;
							}
						}

						if (heading == null) {
							index = model.addHeading(0);
							heading = coa.getHeading(index);
							heading.setNumber(fields[1]);
							positionChanged = true;
						}

						heading.setText(fields[2]);
						heading.setLevel(level);
						model.updateRow(index, positionChanged);
					}
					else {
						throw new NumberFormatException();
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (ParseException e) {
				logger.log(Level.WARNING, "Virheellinen rivi: " + Arrays.toString(fields));
			}
			catch (NumberFormatException e) {
				logger.log(Level.WARNING, "Virheellinen rivi: " + Arrays.toString(fields));
			}

			tableModel.fireTableDataChanged();
			return true;
		}

		private String accountIdToString(int id) {
			if (id < 0) return "";
			Account account = model.getAccountById(id);
			return (account == null) ? "" : account.getNumber();
		}

		private int findAccount(String number) {
			Account account = model.getAccountByNumber(number);
			return (account == null) ? -1 : account.getId();
		}
	};
}
