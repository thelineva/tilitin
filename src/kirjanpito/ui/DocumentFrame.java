package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import kirjanpito.db.Account;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryTemplate;
import kirjanpito.db.Period;
import kirjanpito.db.Settings;
import kirjanpito.models.COAModel;
import kirjanpito.models.CSVExportWorker;
import kirjanpito.models.DataSourceInitializationModel;
import kirjanpito.models.DataSourceInitializationWorker;
import kirjanpito.models.DocumentModel;
import kirjanpito.models.DocumentTypeModel;
import kirjanpito.models.EntryTableModel;
import kirjanpito.models.EntryTemplateModel;
import kirjanpito.models.PrintPreviewModel;
import kirjanpito.models.PropertiesModel;
import kirjanpito.models.ReportEditorModel;
import kirjanpito.models.StartingBalanceModel;
import kirjanpito.models.StatisticsModel;
import kirjanpito.models.TextFieldWithLockIcon;
import kirjanpito.reports.AccountStatementModel;
import kirjanpito.reports.AccountStatementPrint;
import kirjanpito.reports.AccountSummaryModel;
import kirjanpito.reports.AccountSummaryPrint;
import kirjanpito.reports.COAPrint;
import kirjanpito.reports.COAPrintModel;
import kirjanpito.reports.DocumentPrint;
import kirjanpito.reports.DocumentPrintModel;
import kirjanpito.reports.FinancialStatementModel;
import kirjanpito.reports.FinancialStatementPrint;
import kirjanpito.reports.GeneralJournalModel;
import kirjanpito.reports.GeneralJournalModelT;
import kirjanpito.reports.GeneralJournalPrint;
import kirjanpito.reports.GeneralLedgerModel;
import kirjanpito.reports.GeneralLedgerModelT;
import kirjanpito.reports.GeneralLedgerPrint;
import kirjanpito.reports.Print;
import kirjanpito.reports.PrintModel;
import kirjanpito.reports.VATReportModel;
import kirjanpito.reports.VATReportPrint;
import kirjanpito.ui.resources.Resources;
import kirjanpito.util.AppSettings;
import kirjanpito.util.Registry;
import kirjanpito.util.RegistryAdapter;

/**
 * Tositetietojen muokkausikkuna.
 *
 * @author Tommi Helineva
 */
public class DocumentFrame extends JFrame implements AccountSelectionListener {
	protected Registry registry;
	protected DocumentModel model;
	protected JMenu entryTemplateMenu;
	protected JMenu docTypeMenu;
	protected JMenu gotoMenu;
	protected JMenu reportsMenu;
	protected JMenu toolsMenu;
	private JMenuItem newDatabaseMenuItem;
	private JMenuItem openDatabaseMenuItem;
	private JMenuItem newDocMenuItem;
	private JMenuItem deleteDocMenuItem;
	private JMenuItem addEntryMenuItem;
	private JMenuItem removeEntryMenuItem;
	private JMenuItem pasteMenuItem;
	private JMenuItem coaMenuItem;
	private JMenuItem vatDocumentMenuItem;
	private JMenuItem editEntryTemplatesMenuItem;
	private JMenuItem createEntryTemplateMenuItem;
	private JMenuItem startingBalancesMenuItem;
	private JMenuItem propertiesMenuItem;
	private JMenuItem settingsMenuItem;
	private JCheckBoxMenuItem searchMenuItem;
	private JCheckBoxMenuItem[] docTypeMenuItems;
	private JMenuItem editDocTypesMenuItem;
	private JMenuItem setIgnoreFlagMenuItem;
	private JButton prevButton;
	private JButton nextButton;
	private JButton searchButton;
	private JButton findByNumberButton;
	private JButton newDocButton;
	private JButton addEntryButton;
	private JButton removeEntryButton;
	private TextFieldWithLockIcon numberTextField;
	private DateTextField dateTextField;
	private JLabel debitTotalLabel;
	private JLabel creditTotalLabel;
	private JLabel differenceLabel;
	private JLabel documentLabel;
	private JLabel periodLabel;
	private JLabel documentTypeLabel;
	private JTable entryTable;
	private TableColumn vatColumn;
	private EntryTableHeaderRenderer tableHeaderRenderer;
	private JPanel searchPanel;
	private JTextField searchPhraseTextField;
	private EntryTableModel tableModel;
	private AccountCellRenderer accountCellRenderer;
	private AccountCellEditor accountCellEditor;
	private DescriptionCellEditor descriptionCellEditor;
	private DecimalFormat formatter;
	private AccountSelectionDialog accountSelectionDialog;
	private PrintPreviewFrame printPreviewFrame;
	private boolean searchEnabled;
	private BigDecimal debitTotal;
	private BigDecimal creditTotal;

	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	private static final long serialVersionUID = 1L;

	public DocumentFrame(Registry registry, DocumentModel model) {
		super(Kirjanpito.APP_NAME);
		this.registry = registry;
		this.model = model;
		this.debitTotal = BigDecimal.ZERO;
		this.creditTotal = BigDecimal.ZERO;
		registry.addListener(registryListener);
	}

	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				entryTable.setRowHeight(getFontMetrics(entryTable.getFont()).getHeight() + 6);
			}

			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		try {
			List<Image> images = new ArrayList<Image>(3);
			images.add(ImageIO.read(Resources.load("tilitin-24x24.png")));
			images.add(ImageIO.read(Resources.load("tilitin-32x32.png")));
			images.add(ImageIO.read(Resources.load("tilitin-48x48.png")));
			setIconImages(images);
		}
		catch (IOException e) { }

		createMenuBar();
		createToolBar();
		createStatusBar();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		add(contentPanel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
		contentPanel.add(bottomPanel, BorderLayout.PAGE_END);

		createTextFieldPanel(contentPanel);
		createTable(contentPanel);
		createTotalRow(bottomPanel);
		createSearchBar(bottomPanel);

		formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
		formatter.setParseBigDecimal(true);

		AppSettings settings = AppSettings.getInstance();
		int width = settings.getInt("window.width", 0);
		int height = settings.getInt("window.height", 0);
		setMinimumSize(new Dimension(500, 300));

		if (width > 0 && height > 0) {
			setSize(width, height);
		}
		else {
			pack();
		}

		setLocationRelativeTo(null);
	}

	/**
	 * Luo ikkunan valikot.
	 */
	protected void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenuItem menuItem;
		JMenu menu;

		/* Luodaan Tiedosto-valikko. */
		menu = new JMenu("Tiedosto");
		menu.setMnemonic('T');
		menuBar.add(menu);

		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		newDatabaseMenuItem = SwingUtils.createMenuItem("Uusi…",
				null, 'U', null, newDatabaseListener);

		menu.add(newDatabaseMenuItem);

		openDatabaseMenuItem = SwingUtils.createMenuItem("Avaa…",
				null, 'A', KeyStroke.getKeyStroke('O', shortcutKeyMask),
				openDatabaseListener);

		menu.add(openDatabaseMenuItem);

		menu.add(SwingUtils.createMenuItem("Tietokanta-asetukset…", null, 'T', null,
				databaseSettingsListener));

		menu.addSeparator();
		menu.add(SwingUtils.createMenuItem("Lopeta", "quit-16x16.png", 'L',
				KeyStroke.getKeyStroke('Q', shortcutKeyMask),
				quitListener));

		/* Luodaan Muokkaa-valikko. */
		menu = new JMenu("Muokkaa");
		menu.setMnemonic('M');
		menuBar.add(menu);

		menu.add(SwingUtils.createMenuItem("Kopioi", null, 'K',
				KeyStroke.getKeyStroke(KeyEvent.VK_C,
						shortcutKeyMask), copyEntriesAction));

		pasteMenuItem = SwingUtils.createMenuItem("Liitä", null, 'L',
				KeyStroke.getKeyStroke(KeyEvent.VK_V,
						shortcutKeyMask), pasteEntriesAction);

		menu.add(pasteMenuItem);
		menu.addSeparator();

		newDocMenuItem = SwingUtils.createMenuItem("Uusi tosite", "document-new-16x16.png", 'U',
				KeyStroke.getKeyStroke('N', shortcutKeyMask),
				newDocListener);

		deleteDocMenuItem = SwingUtils.createMenuItem("Poista tosite", "delete-16x16.png", 'P',
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, shortcutKeyMask), deleteDocListener);

		menu.add(newDocMenuItem);
		menu.add(deleteDocMenuItem);
		menu.addSeparator();

		addEntryMenuItem = SwingUtils.createMenuItem("Lisää vienti",
				"list-add-16x16.png", 'L',
				KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), addEntryListener);

		removeEntryMenuItem = SwingUtils.createMenuItem("Poista vienti",
				"list-remove-16x16.png", 'o',
				null, removeEntryListener);

		entryTemplateMenu = new JMenu("Vientimallit");
		editEntryTemplatesMenuItem = SwingUtils.createMenuItem("Muokkaa", null, 'M',
				KeyStroke.getKeyStroke(KeyEvent.VK_M, shortcutKeyMask),
				editEntryTemplatesListener);

		createEntryTemplateMenuItem = SwingUtils.createMenuItem("Luo tositteesta", null, 'K',
				KeyStroke.getKeyStroke(KeyEvent.VK_K, shortcutKeyMask),
				createEntryTemplateListener);

		menu.add(addEntryMenuItem);
		menu.add(removeEntryMenuItem);
		menu.add(entryTemplateMenu);

		menu.addSeparator();

		coaMenuItem = SwingUtils.createMenuItem("Tilikartta…", null, 'T',
				KeyStroke.getKeyStroke(KeyEvent.VK_T,
						shortcutKeyMask), chartOfAccountsListener);

		startingBalancesMenuItem = SwingUtils.createMenuItem("Alkusaldot…", null, 's',
				null, startingBalancesListener);

		propertiesMenuItem = SwingUtils.createMenuItem("Perustiedot…", null, 'e',
				null, propertiesListener);

		settingsMenuItem = SwingUtils.createMenuItem("Kirjausasetukset…", null, 'K',
				null, settingsListener);

		menu.add(coaMenuItem);
		menu.add(startingBalancesMenuItem);
		menu.add(propertiesMenuItem);
		menu.add(settingsMenuItem);

		/* Luodaan Siirry-valikko. */
		menu = gotoMenu = new JMenu("Siirry");
		menu.setMnemonic('S');
		menuBar.add(menu);

		menu.add(SwingUtils.createMenuItem("Edellinen", "go-previous-16x16.png", 'E',
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
				prevDocListener));

		menu.add(SwingUtils.createMenuItem("Seuraava", "go-next-16x16.png", 'S',
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
				nextDocListener));

		menu.addSeparator();

		menu.add(SwingUtils.createMenuItem("Ensimmäinen", "go-first-16x16.png", 'n',
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
						shortcutKeyMask), firstDocListener));

		menu.add(SwingUtils.createMenuItem("Viimeinen", "go-last-16x16.png", 'V',
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,
						shortcutKeyMask), lastDocListener));

		menu.addSeparator();

		menu.add(SwingUtils.createMenuItem("Hae numerolla…",
				null, 'n', KeyStroke.getKeyStroke(KeyEvent.VK_G,
						shortcutKeyMask), findDocumentByNumberListener));

		searchMenuItem = new JCheckBoxMenuItem("Etsi…");
		searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
				shortcutKeyMask));
		searchMenuItem.addActionListener(searchListener);
		menu.add(searchMenuItem);

		/* Luodaan Tositelaji-valikko. */
		menu = docTypeMenu = new JMenu("Tositelaji");
		menu.setMnemonic('l');
		menuBar.add(menu);

		editDocTypesMenuItem = SwingUtils.createMenuItem("Muokkaa", null, 'M',
				KeyStroke.getKeyStroke(KeyEvent.VK_L, shortcutKeyMask),
				editDocTypesListener);

		/* Luodaan Tulosteet-valikko. */
		menu = reportsMenu = new JMenu("Tulosteet");
		menu.setMnemonic('u');
		menuBar.add(menu);

		menuItem = SwingUtils.createMenuItem("Tilien saldot", null, 's',
				KeyStroke.getKeyStroke(KeyEvent.VK_1, shortcutKeyMask), printListener);
		menuItem.setActionCommand("accountSummary");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Tosite", null, 'O',
				KeyStroke.getKeyStroke(KeyEvent.VK_2, shortcutKeyMask), printListener);
		menuItem.setActionCommand("document");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Tiliote", null, 'T',
				KeyStroke.getKeyStroke(KeyEvent.VK_3, shortcutKeyMask), printListener);
		menuItem.setActionCommand("accountStatement");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Tuloslaskelma", null, 'u',
				KeyStroke.getKeyStroke(KeyEvent.VK_4, shortcutKeyMask), printListener);
		menuItem.setActionCommand("incomeStatement");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Tuloslaskelma erittelyin", null, 'e',
				KeyStroke.getKeyStroke(KeyEvent.VK_5, shortcutKeyMask), printListener);
		menuItem.setActionCommand("incomeStatementDetailed");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Tase", null, 'a',
				KeyStroke.getKeyStroke(KeyEvent.VK_6, shortcutKeyMask), printListener);
		menuItem.setActionCommand("balanceSheet");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Tase erittelyin", null, 'e',
				KeyStroke.getKeyStroke(KeyEvent.VK_7, shortcutKeyMask), printListener);
		menuItem.setActionCommand("balanceSheetDetailed");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Päiväkirja", null, 'P',
				KeyStroke.getKeyStroke(KeyEvent.VK_8, shortcutKeyMask), printListener);
		menuItem.setActionCommand("generalJournal");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Pääkirja", null, 'k',
				KeyStroke.getKeyStroke(KeyEvent.VK_9, shortcutKeyMask), printListener);
		menuItem.setActionCommand("generalLedger");
		menu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("ALV-laskelma tileittäin", null, 'V',
				KeyStroke.getKeyStroke(KeyEvent.VK_0, shortcutKeyMask), printListener);
		menuItem.setActionCommand("vatReport");
		menu.add(menuItem);

		JMenu submenu = new JMenu("Tilikartta");
		submenu.setMnemonic('r');
		menu.add(submenu);

		menuItem = SwingUtils.createMenuItem("Vain käytössä olevat tilit", null, 'V', null, printListener);
		menuItem.setActionCommand("coa1");
		submenu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Vain suosikkitilit", null, 's', null, printListener);
		menuItem.setActionCommand("coa2");
		submenu.add(menuItem);

		menuItem = SwingUtils.createMenuItem("Kaikki tilit", null, 'k', null, printListener);
		menuItem.setActionCommand("coa0");
		submenu.add(menuItem);

		menu.addSeparator();
		menu.add(SwingUtils.createMenuItem("Muokkaa", null, 'M', null, editReportsListener));

		/* Luodaan Työkalut-valikko. */
		menu = toolsMenu = new JMenu("Työkalut");
		menu.setMnemonic('y');
		menuBar.add(menu);

		vatDocumentMenuItem = SwingUtils.createMenuItem("ALV-tilien päättäminen",
				null, 'p', KeyStroke.getKeyStroke(KeyEvent.VK_R,
						shortcutKeyMask), vatDocumentListener);

		menu.add(vatDocumentMenuItem);

		setIgnoreFlagMenuItem = SwingUtils.createMenuItem("Ohita vienti ALV-laskelmassa", null, 'O',
				KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutKeyMask),
				setIgnoreFlagToEntryAction);

		menu.add(setIgnoreFlagMenuItem);

		menu.add(SwingUtils.createMenuItem("Tilien saldojen vertailu", null, 'T',
				null, balanceComparisonListener));

		menu.add(SwingUtils.createMenuItem("Muuta tositenumeroita", null, 'n',
				null, numberShiftListener));

		menu.add(SwingUtils.createMenuItem("ALV-kantojen muutokset", null, 'm',
				null, vatChangeListener));

		menu.add(SwingUtils.createMenuItem("Vie tiedostoon",
				null, 'V', null, exportListener));

		/* Luodaan Ohje-valikko. */
		menu = new JMenu("Ohje");
		menu.setMnemonic('O');
		menuBar.add(menu);

		menu.add(SwingUtils.createMenuItem("Sisältö", null, 'S',
				KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
				helpListener));

		menu.add(SwingUtils.createMenuItem("Virheenjäljitystietoja", null, 'V',
				null, debugListener));

		menu.add(SwingUtils.createMenuItem("Tietoja ohjelmasta", null, 'T',
				null, aboutListener));

		setJMenuBar(menuBar);
	}

	/**
	 * Luo ikkunan työkalurivin.
	 */
	protected void createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		prevButton = SwingUtils.createToolButton("go-previous-22x22.png",
				"Edellinen tosite", prevDocListener, false);

		nextButton = SwingUtils.createToolButton("go-next-22x22.png",
				"Seuraava tosite", nextDocListener, false);

		toolBar.add(prevButton);
		toolBar.add(nextButton);
		toolBar.addSeparator();

		newDocButton = SwingUtils.createToolButton("document-new-22x22.png",
				"Uusi tosite", newDocListener, true);

		addEntryButton = SwingUtils.createToolButton("list-add-22x22.png",
				"Lisää vienti", addEntryListener, true);

		removeEntryButton = SwingUtils.createToolButton("list-remove-22x22.png",
				"Poista vienti", removeEntryListener, true);

		toolBar.add(newDocButton);
		toolBar.addSeparator();
		toolBar.add(addEntryButton);
		toolBar.add(removeEntryButton);
		toolBar.addSeparator();

		findByNumberButton = SwingUtils.createToolButton("jump-22x22.png",
				"Hae numerolla", findDocumentByNumberListener, true);

		searchButton = SwingUtils.createToolButton("find-22x22.png",
				"Etsi", searchListener, true);

		toolBar.add(findByNumberButton);
		toolBar.add(searchButton);

		add(toolBar, BorderLayout.PAGE_START);
	}

	/**
	 * Lisää <code>container</code>-paneeliin tositenumerokentän,
	 * päivämääräkentän ja tagikentän.
	 *
	 * @param container paneeli, johon komponentit lisätään
	 */
	protected void createTextFieldPanel(JPanel container) {
		GridBagConstraints c;
		JPanel panel = new JPanel();
		container.add(panel, BorderLayout.PAGE_START);
		panel.setLayout(new GridLayout(0, 2));

		JPanel left = new JPanel();
		left.setLayout(new GridBagLayout());
		panel.add(left);

		/* Lisätään paneeliin tositenumeronimiö ja -tekstikenttä. */
		JLabel numberLabel = new JLabel("Tositenumero");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(8, 8, 8, 4);
		left.add(numberLabel, c);

		numberTextField = new TextFieldWithLockIcon();
		numberTextField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (numberTextField.isEditable()) {
					model.setDocumentChanged();
				}
			}
		});

		numberTextField.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "transferFocus");

		numberTextField.getActionMap().put("transferFocus", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				numberTextField.transferFocus();
			}
		});

		c = new GridBagConstraints();
		c.insets = new Insets(8, 4, 8, 4);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		left.add(numberTextField, c);

		c.fill = GridBagConstraints.VERTICAL;
		c.weightx = 0.0;
		left.add(new JSeparator(SwingConstants.VERTICAL), c);

		JPanel right = new JPanel();
		right.setLayout(new GridBagLayout());
		panel.add(right);

		/* Lisätään paneeliin päivämääränimiö ja -tekstikenttä. */
		JLabel dateLabel = new JLabel("Päivämäärä");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(8, 8, 8, 4);
		right.add(dateLabel, c);

		dateTextField = new DateTextField();
		dateTextField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (dateTextField.isEditable() && Character.isDigit(e.getKeyChar())) {
					model.setDocumentChanged();
				}
			}
		});

		dateTextField.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "addEntry");

		dateTextField.getActionMap().put("addEntry", addEntryListener);

		dateTextField.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "firstEntry");

		dateTextField.getActionMap().put("firstEntry", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				dateTextField.transferFocus();

				/* Valitaan ensimmäinen rivi. */
				if (entryTable.getRowCount() > 0) {
					entryTable.changeSelection(0,
							Math.max(0, entryTable.getSelectedColumn()),
							false, false);
				}
			}
		});

		c = new GridBagConstraints();
		c.insets = new Insets(8, 4, 8, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		right.add(dateTextField, c);
	}

	/**
	 * Luo taulukon, joka näyttää tositteen viennit.
	 *
	 * @param container paneeli, johon taulukko lisätään
	 */
	protected void createTable(JPanel container) {
		tableModel = new EntryTableModel(model);
		tableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				updateTotalRow();
			}
		});

		accountCellRenderer = new AccountCellRenderer(registry, tableModel);
		accountCellEditor = new AccountCellEditor(registry, tableModel, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String q = accountCellEditor.getTextField().getText();
				showAccountSelection(q);
			}
		});

		descriptionCellEditor = new DescriptionCellEditor(model);

		entryTable = new JTable(tableModel);
		entryTable.setFillsViewportHeight(true);
		entryTable.setPreferredScrollableViewportSize(new Dimension(680, 250));
		entryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		entryTable.setSurrendersFocusOnKeystroke(true);
		entryTable.setColumnSelectionAllowed(true);

		tableHeaderRenderer = new EntryTableHeaderRenderer(
				entryTable.getTableHeader().getDefaultRenderer());
		entryTable.getTableHeader().setDefaultRenderer(tableHeaderRenderer);

		TableColumn column;
		int[] widths = new int[] {190, 80, 80, 80, 190};
		AppSettings settings = AppSettings.getInstance();
		int width;

		DescriptionCellEditor descriptionCellEditor = new DescriptionCellEditor(model);
		CurrencyCellRenderer currencyCellRenderer = new CurrencyCellRenderer();
		CurrencyCellEditor currencyCellEditor = new CurrencyCellEditor();
		currencyCellEditor.setActionListener(toggleDebitCreditAction);
		tableModel.setCurrencyCellEditor(currencyCellEditor);

		TableCellRenderer[] renderers = new TableCellRenderer[] {
			accountCellRenderer, currencyCellRenderer, currencyCellRenderer,
			currencyCellRenderer, null };

		TableCellEditor[] editors = new TableCellEditor[] {
			accountCellEditor, currencyCellEditor, currencyCellEditor,
			currencyCellEditor, descriptionCellEditor };

		for (int i = 0; i < widths.length; i++) {
			column = entryTable.getColumnModel().getColumn(i);
			width = settings.getInt("table.columns." + i, 0);

			if (width > 0) {
				column.setPreferredWidth(width);
			}
			else {
				column.setPreferredWidth(widths[i]);
			}

			if (renderers[i] != null) {
				column.setCellRenderer(renderers[i]);
			}

			if (editors[i] != null) {
				column.setCellEditor(editors[i]);
			}
		}

		container.add(new JScrollPane(entryTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/* Muutetaan enter-näppäimen toiminta. */
		entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextCell");

		entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, shortcutKeyMask), "nextCell");

		entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "prevCell");

		entryTable.getActionMap().put("prevCell", prevCellAction);
		entryTable.getActionMap().put("nextCell", nextCellAction);

		/* Muutetaan ylänuolen toiminta. */
		Object key = entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));

		AbstractAction previousRowAction = new PreviousRowAction(
				entryTable.getActionMap().get(key));

		entryTable.getActionMap().put(key, previousRowAction);

		/* Lisätään vienti, kun insert-näppäintä painetaan. */
		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insertRow");

		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "insertRow");

		entryTable.getActionMap().put("insertRow", addEntryListener);

		/* Vaihdetaan debet-vienti kredit-vienniksi ja toisin päin, kun
		 * §-näppäintä painetaan. */
		entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke('§'), "toggleDebitCredit");

		entryTable.getActionMap().put("toggleDebitCredit", toggleDebitCreditAction);

		/* Kun F12-näppäintä painetaan, aloitetaan selitteen muokkaaminen
		 * ja poistetaan teksti viimeiseen pilkkuun asti. */
		RemoveSuffixAction removeSuffixAction = new RemoveSuffixAction();

		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "removeSuffix");

		entryTable.getActionMap().put("removeSuffix", removeSuffixAction);

		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKeyMask), "copy");

		entryTable.getActionMap().put("copy", copyEntriesAction);

		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutKeyMask), "paste");

		entryTable.getActionMap().put("paste", pasteEntriesAction);

		descriptionCellEditor.getTextField().getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "removeSuffix");

		descriptionCellEditor.getTextField().getActionMap().put(
				"removeSuffix", removeSuffixAction);

		/* Siirrytään edelliseen tositteeseen, kun
		 * Page Up -näppäintä painetaan. */
		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "prevDocument");

		entryTable.getActionMap().put("prevDocument", prevDocListener);

		/* Siirrytään seuraavaan tositteeseen, kun
		 * Page Down -näppäintä painetaan. */
		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "nextDocument");

		entryTable.getActionMap().put("nextDocument", nextDocListener);

		/* Poistetaan vienti, kun delete-näppäintä painetaan. */
		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeRow");

		entryTable.getActionMap().put("removeRow", removeEntryListener);

		/* Merkitään vienti ohitettavaksi ALV-laskelmassa, kun painetaan Ctrl+H */
		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutKeyMask), "setIgnoreFlag");

		entryTable.getActionMap().put("setIgnoreFlag", setIgnoreFlagToEntryAction);
	}

	/**
	 * Luo summarivin.
	 *
	 * @param container paneeli, johon rivi lisätään
	 */
	protected void createTotalRow(JPanel container) {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 6);
		c.anchor = GridBagConstraints.WEST;

		JPanel panel = new JPanel(layout);
		panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 5, 2));
		container.add(panel);

		debitTotalLabel = new JLabel("0,00");
		Dimension minSize = debitTotalLabel.getMinimumSize();
		debitTotalLabel.setPreferredSize(new Dimension(80, minSize.height));
		creditTotalLabel = new JLabel("0,00");
		creditTotalLabel.setPreferredSize(new Dimension(80, minSize.height));
		differenceLabel = new JLabel("0,00");
		differenceLabel.setPreferredSize(new Dimension(80, minSize.height));

		panel.add(new JLabel("Debet yht."), c);
		panel.add(debitTotalLabel, c);
		panel.add(new JLabel("Kredit yht."), c);
		panel.add(creditTotalLabel, c);
		panel.add(new JLabel("Erotus"), c);
		c.weightx = 1.0;
		panel.add(differenceLabel, c);
	}

	/**
	 * Luo hakupalkin.
	 *
	 * @param container paneeli, johon hakupalkki lisätään
	 */
	protected void createSearchBar(JPanel container) {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 5, 8, 5);
		c.anchor = GridBagConstraints.WEST;

		JPanel panel = searchPanel = new JPanel(layout);
		panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		panel.setVisible(false);
		container.add(panel);

		JTextField textField = searchPhraseTextField = new JTextField();
		textField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					searchDocuments();
					e.consume();
				}
			}
		});
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(textField, c);

		JButton button = new JButton("Etsi", new ImageIcon(
				Resources.load("find-16x16.png")));

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchDocuments();
			}
		});
		button.setMnemonic('H');
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		panel.add(button, c);

		button = new JButton(new ImageIcon(Resources.load("close-16x16.png")));
		button.addActionListener(searchListener);
		panel.add(button, c);
	}

	/**
	 * Luo tilarivin, jossa näytetään valitun tositteen järjestysnumero
	 * ja tilikausi.
	 */
	protected void createStatusBar() {
		JPanel statusBarPanel = new JPanel(new BorderLayout());
		documentLabel = new JLabel();
		documentLabel.setBorder(new EtchedBorder());
		documentLabel.setPreferredSize(new Dimension(150, 0));
		periodLabel = new JLabel(" ");
		periodLabel.setBorder(new EtchedBorder());
		documentTypeLabel = new JLabel();
		documentTypeLabel.setBorder(new EtchedBorder());
		documentTypeLabel.setPreferredSize(new Dimension(200, 0));
		statusBarPanel.add(documentLabel, BorderLayout.WEST);
		statusBarPanel.add(periodLabel, BorderLayout.CENTER);
		statusBarPanel.add(documentTypeLabel, BorderLayout.EAST);
		add(statusBarPanel, BorderLayout.PAGE_END);
	}

	/**
	 * Lopettaa ohjelman suorituksen.
	 */
	public void quit() {
		if (registry.getDataSource() != null) {
			if (!saveDocumentIfChanged()) {
				return;
			}

			saveDocumentTypeIfChanged();
			model.closeDataSource();
		}

		if (printPreviewFrame != null) {
			printPreviewFrame.close();
			printPreviewFrame = null;
		}

		AppSettings settings = AppSettings.getInstance();

		/* Tallennetaan ikkunan koko. */
		settings.set("window.width", getWidth());
		settings.set("window.height", getHeight());

		/* Tallennetaan taulukon sarakkeiden leveydet. */
		for (int i = 0; i < 5; i++) {
			int columnIndex = mapColumnIndexToView(i);

			if (columnIndex >= 0) {
				settings.set("table.columns." + i, entryTable.getColumnModel(
						).getColumn(columnIndex).getWidth());
			}
		}

		settings.save();
		System.exit(0);
	}

	/**
	 * Luo uuden tositteen. Ennen tositteen luontia
	 * käyttäjän tekemät muutokset tallennetaan.
	 */
	public void createDocument() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		try {
			model.createDocument();
		}
		catch (DataAccessException e) {
			String message = "Uuden tositteen luominen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}

		updatePosition();
		updateDocument();
		updateTotalRow();
	}

	/**
	 * Poistaa valitun tositteen.
	 */
	public void deleteDocument() {
		int result = JOptionPane.showConfirmDialog(this,
				"Haluatko varmasti poistaa valitun tositteen?",
				Kirjanpito.APP_NAME, JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (result == JOptionPane.YES_OPTION) {
			try {
				model.deleteDocument();
			}
			catch (DataAccessException e) {
				String message = "Tositteen poistaminen epäonnistui";
				logger.log(Level.SEVERE, message, e);
				SwingUtils.showDataAccessErrorMessage(this, e, message);
			}

			updatePosition();
			updateDocument();
			updateTotalRow();
		}
	}

	/**
	 * Siirtyy toiseen tositteeseen. Ennen tietojen
	 * hakemista käyttäjän tekemät muutokset tallennetaan.
	 *
	 * @param index tositteen järjestysnumero
	 */
	public void goToDocument(int index) {
		if (!saveDocumentIfChanged()) {
			return;
		}

		try {
			model.goToDocument(index);
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}

		updatePosition();
		updateDocument();
		updateTotalRow();
	}

	/**
	 * Kysyy käyttäjältä tositenumeroa, ja siirtyy tähän tositteeseen.
	 */
	public void findDocumentByNumber() {
		boolean valid = false;
		int documentTypeIndex, index;
		int number = -1;

		while (!valid) {
			String s = JOptionPane.showInputDialog(this, "Tositenumero?",
				Kirjanpito.APP_NAME, JOptionPane.PLAIN_MESSAGE);

			if (s != null) {
				try {
					number = Integer.parseInt(s);
				}
				catch (NumberFormatException e) {
					number = -1;
				}

				valid = (number > 0);

				if (!valid) {
					JOptionPane.showMessageDialog(this,
							"Tositenumero saa sisältää vain numeroita.",
							Kirjanpito.APP_NAME, JOptionPane.ERROR_MESSAGE);
				}
			}
			else {
				return;
			}
		}

		documentTypeIndex = findDocumentTypeByNumber(number);

		try {
			index = model.findDocumentByNumber(documentTypeIndex, number);
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		if (index < 0) {
			SwingUtils.showInformationMessage(this,
					"Tositetta ei löytynyt numerolla " + number + ".");
			return;
		}

		boolean invalidDocuments = false;

		if (searchEnabled) {
			searchEnabled = false;
			invalidDocuments = true;
			updateSearchPanel();
		}

		if (model.getDocumentTypeIndex() != documentTypeIndex) {
			selectDocumentTypeMenuItem(documentTypeIndex);
			model.setDocumentTypeIndex(documentTypeIndex);
			invalidDocuments = true;
		}

		/* Tositetiedot on haettava tietokannasta, jos tositelaji on muuttunut
		 * tai haku on kytketty pois päältä. */
		if (invalidDocuments) {
			try {
				model.fetchDocuments(index);
			}
			catch (DataAccessException e) {
				String message = "Tositetietojen hakeminen epäonnistui";
				logger.log(Level.SEVERE, message, e);
				SwingUtils.showDataAccessErrorMessage(this, e, message);
				return;
			}

			updatePosition();
			updateDocument();
			updateTotalRow();
		}
		else {
			goToDocument(index);
		}
	}

	/**
	 * Kytkee tositteiden haun päälle tai pois päältä.
	 */
	public void toggleSearchPanel() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		searchEnabled = !searchEnabled;
		updateSearchPanel();

		if (!searchEnabled) {
			/* Kun haku kytketään pois päältä, haetaan valitun
			 * tositelajin kaikki tositteet.
			 */
			try {
				model.fetchDocuments(-1);
			}
			catch (DataAccessException e) {
				String message = "Tositteiden hakeminen epäonnistui";
				logger.log(Level.SEVERE, message, e);
				SwingUtils.showDataAccessErrorMessage(this, e, message);
				return;
			}

			updatePosition();
			updateDocument();
			updateTotalRow();
		}
	}

	/**
	 * Etsii tositteita käyttäjän antamalla hakusanalla.
	 */
	public void searchDocuments() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		int count;

		try {
			count = model.search(searchPhraseTextField.getText());
		}
		catch (DataAccessException e) {
			String message = "Tositteiden hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		if (count == 0) {
			SwingUtils.showInformationMessage(this,
					"Yhtään tositetta ei löytynyt.");
		}
		else {
			updatePosition();
			updateDocument();
			updateTotalRow();
		}
	}

	/**
	 * Avaa tilikarttaikkunan.
	 */
	public void showChartOfAccounts() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		closePrintPreview();
		COAModel coaModel = new COAModel(registry);
		COADialog dialog = new COADialog(this, registry, coaModel);
		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Tallentaa viennit CSV-tiedostoon.
	 */
	public void export() {
		AppSettings settings = AppSettings.getInstance();
		String path = settings.getString("csv-directory", ".");
		JFileChooser fc = new JFileChooser(path);
		fc.setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".csv");
			}

			public String getDescription() {
				return "CSV-tiedostot";
			}
		});

		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			settings.set("csv-directory",
					file.getParentFile().getAbsolutePath());

			CSVExportWorker worker = new CSVExportWorker(registry, file);
			TaskProgressDialog dialog = new TaskProgressDialog(
					this, "CSV-tiedostoon vienti", worker);
			dialog.create();
			dialog.setVisible(true);
			worker.execute();
		}
	}

	/**
	 * Päättää ALV-tilit.
	 */
	public void createVATDocument() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		boolean result;

		try {
			result = model.createVATDocument();
		}
		catch (DataAccessException e) {
			String message = "Uuden tositteen luominen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		updatePosition();
		updateDocument();
		updateTotalRow();

		if (!result) {
			SwingUtils.showErrorMessage(this, "Arvonlisäverovelkatiliä ei ole määritetty.");
		}
	}

	/**
	 * Näyttää vientimallien muokkausikkunan.
	 */
	public void editEntryTemplates() {
		EntryTemplateModel templateModel = new EntryTemplateModel(registry);

		EntryTemplateDialog dialog = new EntryTemplateDialog(
				this, registry, templateModel);

		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Luo vientimallin valitusta tositteesta.
	 */
	public void createEntryTemplateFromDocument() {
		int number;

		try {
			number = model.createEntryTemplateFromDocument();
		}
		catch (DataAccessException e) {
			String message = "Vientimallin luominen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		if (number < 0) {
			return;
		}

		String message = String.format("Vientimalli on luotu numerolle %d", number);

		if (number >= 1 && number < 10) {
			message += String.format(" (Alt+%s)", number % 10);
		}

		updateEntryTemplates();
		SwingUtils.showInformationMessage(this, message);
	}

	/**
	 * Lisää viennit mallin perusteella.
	 *
	 * @param number vientimallin numero
	 */
	public void addEntriesFromTemplate(int number) {
		int result = updateModel();

		if (result < 0) {
			return;
		}

		model.addEntriesFromTemplate(number);
		tableModel.fireTableDataChanged();
	}

	/**
	 * Näyttää alkusaldojen muokkausikkunan.
	 */
	public void showStartingBalances() {
		StartingBalanceModel balanceModel = new StartingBalanceModel(registry);

		try {
			balanceModel.initialize();
		}
		catch (DataAccessException e) {
			String message = "Alkusaldojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		StartingBalanceDialog dialog = new StartingBalanceDialog(
				this, balanceModel);

		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Näyttää perustiedot.
	 */
	public void showProperties() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		closePrintPreview();
		final PropertiesModel settingsModel = new PropertiesModel(registry);

		try {
			settingsModel.initialize();
		}
		catch (DataAccessException e) {
			String message = "Asetuksien hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		PropertiesDialog dialog = new PropertiesDialog(
				this, settingsModel);

		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Näyttää kirjausasetukset.
	 */
	public void showSettings() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		SettingsDialog dialog = new SettingsDialog(this, registry);
		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Näyttää tietokanta-asetukset.
	 */
	public void showDatabaseSettings() {
		DatabaseSettingsDialog dialog = new DatabaseSettingsDialog(
				this);

		AppSettings settings = AppSettings.getInstance();
		String url = settings.getString("database.url", null);
		String defaultUrl = model.buildDefaultJDBCURL();

		if (url == null)
			url = defaultUrl;

		dialog.create();
		dialog.setURL(url);
		dialog.setUsername(settings.getString("database.username", ""));
		dialog.setPassword(settings.getString("database.password", ""));
		dialog.setDefaultUrl(defaultUrl);
		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			settings.set("database.url", dialog.getURL());
			settings.set("database.username", dialog.getUsername());
			settings.set("database.password", dialog.getPassword());

			if (registry.getDataSource() != null) {
				model.closeDataSource();
				updatePeriod();
				updatePosition();
				updateDocument();
				updateTotalRow();
				updateEntryTemplates();
				updateDocumentTypes();
			}

			openDataSource();
		}

		dialog.dispose();
	}

	/**
	 * Lisää viennin tositteeseen.
	 */
	public void addEntry() {
		if (!model.isDocumentEditable()) {
			return;
		}

		stopEditing();
		int index = model.addEntry();
		tableModel.fireTableRowsInserted(index, index);
		updateTotalRow();
		entryTable.changeSelection(index, 0, false, false);
		entryTable.requestFocusInWindow();
	}

	/**
	 * Poistaa käyttäjän valitseman viennin.
	 */
	public void removeEntry() {
		if (!model.isDocumentEditable()) {
			return;
		}

		int[] rows = entryTable.getSelectedRows();

		if (rows.length == 0) {
			return;
		}

		stopEditing();
		Arrays.sort(rows);
		int index = -1;

		for (int i = rows.length - 1; i >= 0; i--) {
			index = rows[i];
			model.removeEntry(index);
			tableModel.fireTableRowsDeleted(index, index);
		}

		updateTotalRow();
		index = Math.min(index, tableModel.getRowCount() - 1);

		if (tableModel.getRowCount() > 0) {
			entryTable.setRowSelectionInterval(index, index);
			entryTable.requestFocusInWindow();
		}
		else if (entryTable.isFocusOwner()) {
			dateTextField.requestFocusInWindow();
		}
	}

	/**
	 * Kopioi valitut viennit leikepöydälle.
	 */
	public void copyEntries() {
		stopEditing();
		StringBuilder sb = new StringBuilder();
		int[] rows = entryTable.getSelectedRows();

		for (int i = 0; i < rows.length; i++) {
			Entry entry = model.getEntry(rows[i]);
			Account account = registry.getAccountById(entry.getAccountId());

			if (account == null) {
				sb.append('\t');
			}
			else {
				sb.append(account.getNumber());
				sb.append('\t');
				sb.append(account.getName());
			}

			sb.append('\t');

			if (entry.isDebit()) {
				sb.append(formatter.format(model.getVatIncludedAmount(rows[i])));
				sb.append('\t');
			}
			else {
				sb.append('\t');
				sb.append(formatter.format(model.getVatIncludedAmount(rows[i])));
			}

			sb.append('\t');
			sb.append(formatter.format(model.getVatAmount(i)));
			sb.append('\t');
			sb.append(entry.getDescription());
			sb.append(System.getProperty("line.separator"));
		}

		Toolkit.getDefaultToolkit().getSystemClipboard(
				).setContents(new StringSelection(sb.toString()), null);
	}

	/**
	 * Liittää leikepöydällä olevat viennit.
	 */
	public void pasteEntries() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		String text = null;

		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				text = t.getTransferData(DataFlavor.stringFlavor).toString();
			}
		}
		catch (UnsupportedFlavorException e) {
		}
		catch (IOException e) {
		}

		if (text == null) {
			return;
		}

		String[] lines = text.split("\n");
		stopEditing();

		for (String line : lines) {
			String[] cols = line.split("\t", 6);

			if (cols.length != 6) {
				continue;
			}

			int index = model.addEntry();
			Entry entry = model.getEntry(index);
			Account account = registry.getAccountByNumber(cols[0]);
			boolean vatEntries = true;

			if (account != null) {
				model.updateAccountId(index, account.getId());
			}

			if (!cols[4].isEmpty()) {
				/* Lasketaan ALV, jos ALV-sarakkeen rahamäärä on erisuuri kuin 0,00. */
				try {
					vatEntries = ((BigDecimal)formatter.parse(cols[4])).compareTo(BigDecimal.ZERO) != 0;
				}
				catch (ParseException e) {
				}
			}

			if (!cols[2].isEmpty()) {
				entry.setDebit(true);

				try {
					model.updateAmount(index, (BigDecimal)formatter.parse(cols[2]), vatEntries);
				}
				catch (ParseException e) {
				}
			}

			if (!cols[3].isEmpty()) {
				entry.setDebit(false);

				try {
					model.updateAmount(index, (BigDecimal)formatter.parse(cols[3]), vatEntries);
				}
				catch (ParseException e) {
				}
			}

			entry.setDescription(cols[5]);
			tableModel.fireTableRowsInserted(index, index);
			entryTable.changeSelection(index, 0, false, false);
		}

		updateTotalRow();
	}

	/**
	 * Valitsee tositelajin.
	 *
	 * @param index tositelajin järjestysnumero
	 */
	public void setDocumentType(int index) {
		selectDocumentTypeMenuItem(index);
		model.setDocumentTypeIndex(index);

		try {
			model.fetchDocuments(-1);
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}

		updatePosition();
		updateDocument();
		updateTotalRow();
	}

	/**
	 * Näyttää tositelajien muokkausikkunan.
	 */
	public void editDocumentTypes() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		DocumentTypeModel documentTypeModel = new DocumentTypeModel(registry);

		DocumentTypeDialog dialog = new DocumentTypeDialog(
				this, documentTypeModel);

		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Näyttää tilien saldot esikatseluikkunassa.
	 */
	public void showAccountSummary() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		AppSettings settings = AppSettings.getInstance();
		AccountSummaryOptionsDialog dialog = new AccountSummaryOptionsDialog(this);
		dialog.create();
		dialog.setPeriod(registry.getPeriod());
		dialog.setDocumentDate(model.getDocument().getDate());
		dialog.setDateSelectionMode(0);
		dialog.setPreviousPeriodVisible(settings.getBoolean("previous-period", false));
		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			boolean previousPeriodVisible = dialog.isPreviousPeriodVisible();
			settings.set("previous-period", previousPeriodVisible);
			int printedAccounts = dialog.getPrintedAccounts();
			AccountSummaryModel printModel = new AccountSummaryModel();
			printModel.setRegistry(registry);
			printModel.setPeriod(registry.getPeriod());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			printModel.setPreviousPeriodVisible(previousPeriodVisible);
			printModel.setPrintedAccounts(printedAccounts);
			showPrintPreview(printModel, new AccountSummaryPrint(printModel,
					printedAccounts != 1));
		}

		dialog.dispose();
	}

	/**
	 * Näyttää tositteen esikatseluikkunassa.
	 */
	public void showDocumentPrint() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		DocumentPrintModel printModel = new DocumentPrintModel();
		printModel.setRegistry(registry);
		printModel.setDocument(model.getDocument());
		showPrintPreview(printModel, new DocumentPrint(printModel));
	}

	/**
	 * Näyttää tiliotteen esikatseluikkunassa.
	 */
	public void showAccountStatement() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		int row = entryTable.getSelectedRow();
		Account account = null;

		if (row >= 0) {
			Entry entry = model.getEntry(row);
			int accountId = entry.getAccountId();

			if (accountId >= 0) {
				account = registry.getAccountById(accountId);
			}
		}

		if (account == null) {
			Settings settings = registry.getSettings();
			int accountId = -1;

			try {
				accountId = Integer.parseInt(settings.getProperty("defaultAccount", ""));
			}
			catch (NumberFormatException e) {
			}

			account = registry.getAccountById(accountId);
		}

		AppSettings settings = AppSettings.getInstance();
		AccountStatementOptionsDialog dialog = new AccountStatementOptionsDialog(this, registry);
		dialog.create();
		dialog.setPeriod(registry.getPeriod());
		dialog.setDocumentDate(model.getDocument().getDate());
		dialog.setDateSelectionMode(1);
		dialog.setOrderByDate(settings.getString("sort-entries", "number").equals("date"));
		dialog.selectAccount(account);
		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			AccountStatementModel printModel = new AccountStatementModel();
			printModel.setDataSource(registry.getDataSource());
			printModel.setPeriod(registry.getPeriod());
			printModel.setSettings(registry.getSettings());
			printModel.setAccount(dialog.getSelectedAccount());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			printModel.setOrderBy(dialog.isOrderByDate() ? AccountStatementModel.ORDER_BY_DATE :
				AccountStatementModel.ORDER_BY_NUMBER);
			settings.set("sort-entries", dialog.isOrderByDate() ? "date" : "number");
			showPrintPreview(printModel, new AccountStatementPrint(printModel));
		}

		dialog.dispose();
	}

	/**
	 * Näyttää tuloslaskelman esikatseluikkunassa.
	 */
	public void showIncomeStatement(boolean detailed) {
		if (!saveDocumentIfChanged()) {
			return;
		}

		FinancialStatementOptionsDialog dialog = new FinancialStatementOptionsDialog(
				registry, this, "Tuloslaskelma",
				FinancialStatementOptionsDialog.TYPE_INCOME_STATEMENT);

		try {
			dialog.fetchData();
		}
		catch (DataAccessException e) {
			String message = "Tietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		dialog.create();
		dialog.setVisible(true);

		if (dialog.getStartDates() != null) {
			FinancialStatementModel printModel = new FinancialStatementModel(
					detailed ? FinancialStatementModel.TYPE_INCOME_STATEMENT_DETAILED :
						FinancialStatementModel.TYPE_INCOME_STATEMENT);
			printModel.setDataSource(registry.getDataSource());
			printModel.setSettings(registry.getSettings());
			printModel.setAccounts(registry.getAccounts());
			printModel.setStartDates(dialog.getStartDates());
			printModel.setEndDates(dialog.getEndDates());
			showPrintPreview(printModel, new FinancialStatementPrint(printModel));
		}
	}

	/**
	 * Näyttää taseen esikatseluikkunassa.
	 */
	public void showBalanceSheet(boolean detailed) {
		if (!saveDocumentIfChanged()) {
			return;
		}

		FinancialStatementOptionsDialog dialog = new FinancialStatementOptionsDialog(
				registry, this, "Tase",
				FinancialStatementOptionsDialog.TYPE_BALANCE_SHEET);

		try {
			dialog.fetchData();
		}
		catch (DataAccessException e) {
			String message = "Tietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		dialog.create();
		dialog.setVisible(true);

		if (dialog.getStartDates() != null) {
			FinancialStatementModel printModel = new FinancialStatementModel(
					detailed ? FinancialStatementModel.TYPE_BALANCE_SHEET_DETAILED :
						FinancialStatementModel.TYPE_BALANCE_SHEET);
			printModel.setDataSource(registry.getDataSource());
			printModel.setSettings(registry.getSettings());
			printModel.setAccounts(registry.getAccounts());
			printModel.setStartDates(dialog.getStartDates());
			printModel.setEndDates(dialog.getEndDates());
			printModel.setPageBreakEnabled(dialog.isPageBreakEnabled());
			showPrintPreview(printModel, new FinancialStatementPrint(printModel));
		}
	}

	/**
	 * Näyttää päiväkirjan esikatseluikkunassa.
	 */
	public void showGeneralJournal() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		AppSettings settings = AppSettings.getInstance();
		GeneralLJOptionsDialog dialog = new GeneralLJOptionsDialog(this, "Päiväkirja");
		dialog.create();
		dialog.setPeriod(registry.getPeriod());
		dialog.setDocumentDate(model.getDocument().getDate());
		dialog.setDateSelectionMode(0);
		dialog.setOrderByDate(settings.getString("sort-entries", "number").equals("date"));
		dialog.setGroupByDocumentTypesEnabled(!registry.getDocumentTypes().isEmpty());
		dialog.setGroupByDocumentTypesSelected(settings.getBoolean("group-by-document-types", true));
		dialog.setTotalAmountVisible(settings.getBoolean("general-journal.total-amount-visible", true));
		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			GeneralJournalModel printModel = dialog.isGroupByDocumentTypesSelected() ?
					new GeneralJournalModelT() : new GeneralJournalModel();
			printModel.setRegistry(registry);
			printModel.setPeriod(registry.getPeriod());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			printModel.setOrderBy(dialog.isOrderByDate() ? GeneralJournalModel.ORDER_BY_DATE :
				GeneralJournalModel.ORDER_BY_NUMBER);
			printModel.setTotalAmountVisible(dialog.isTotalAmountVisible());
			settings.set("sort-entries", dialog.isOrderByDate() ? "date" : "number");
			settings.set("group-by-document-types", dialog.isGroupByDocumentTypesSelected());
			settings.set("general-journal.total-amount-visible", dialog.isTotalAmountVisible());
			showPrintPreview(printModel, new GeneralJournalPrint(printModel));
		}

		dialog.dispose();
	}

	/**
	 * Näyttää pääkirjan esikatseluikkunassa.
	 */
	public void showGeneralLedger() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		AppSettings settings = AppSettings.getInstance();
		GeneralLJOptionsDialog dialog = new GeneralLJOptionsDialog(this, "Pääkirja");
		dialog.create();
		dialog.setPeriod(registry.getPeriod());
		dialog.setDocumentDate(model.getDocument().getDate());
		dialog.setDateSelectionMode(0);
		dialog.setOrderByDate(settings.getString("sort-entries", "number").equals("date"));
		dialog.setGroupByDocumentTypesEnabled(!registry.getDocumentTypes().isEmpty());
		dialog.setGroupByDocumentTypesSelected(settings.getBoolean("group-by-document-types", true));
		dialog.setTotalAmountVisible(settings.getBoolean("general-ledger.total-amount-visible", true));
		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			GeneralLedgerModel printModel = dialog.isGroupByDocumentTypesSelected() ?
					new GeneralLedgerModelT() : new GeneralLedgerModel();
			printModel.setRegistry(registry);
			printModel.setPeriod(registry.getPeriod());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			printModel.setOrderBy(dialog.isOrderByDate() ? GeneralLedgerModel.ORDER_BY_DATE :
				GeneralLedgerModel.ORDER_BY_NUMBER);
			printModel.setTotalAmountVisible(dialog.isTotalAmountVisible());
			settings.set("sort-entries", dialog.isOrderByDate() ? "date" : "number");
			settings.set("group-by-document-types", dialog.isGroupByDocumentTypesSelected());
			settings.set("general-ledger.total-amount-visible", dialog.isTotalAmountVisible());
			showPrintPreview(printModel, new GeneralLedgerPrint(printModel));
		}

		dialog.dispose();
	}

	/**
	 * Näyttää ALV-laskelman esikatseluikkunassa.
	 */
	public void showVATReport() {
		if (!saveDocumentIfChanged()) {
			return;
		}

		PrintOptionsDialog dialog = new PrintOptionsDialog(this, "ALV-laskelma");
		dialog.create();
		dialog.setPeriod(registry.getPeriod());
		dialog.setDocumentDate(model.getDocument().getDate());
		dialog.setDateSelectionMode(1);
		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			VATReportModel printModel = new VATReportModel();
			printModel.setDataSource(registry.getDataSource());
			printModel.setPeriod(registry.getPeriod());
			printModel.setSettings(registry.getSettings());
			printModel.setAccounts(registry.getAccounts());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			showPrintPreview(printModel, new VATReportPrint(printModel));
		}

		dialog.dispose();
	}

	public void showChartOfAccountsPrint(int mode) {
		COAPrintModel printModel = new COAPrintModel();
		printModel.setRegistry(registry);
		printModel.setMode(mode);

		try {
			printModel.run();
		}
		catch (DataAccessException e) {
			String message = "Tulosteen luominen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		showPrintPreview(printModel, new COAPrint(printModel));
	}

	/**
	 * Näyttää tulosteiden muokkausikkunan.
	 */
	public void editReports() {
		ReportEditorModel editorModel = new ReportEditorModel(registry);
		ReportEditorDialog dialog = new ReportEditorDialog(
				this, editorModel);

		try {
			editorModel.load();
		}
		catch (DataAccessException e) {
			String message = "Tulostetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}

		dialog.create();
		dialog.setVisible(true);
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

		if (entryTable.isEditing())
			entryTable.getCellEditor().cancelCellEditing();

		accountSelectionDialog.setSearchPhrase(q);
		accountSelectionDialog.setVisible(true);
	}

	/**
	 * Päivittää valittuun vientiin käyttäjän valitseman tilin.
	 */
	public void accountSelected() {
		Account account = accountSelectionDialog.getSelectedAccount();
		int index = entryTable.getSelectedRow();
		model.updateAccountId(index, account.getId());
		moveToNextCell();
		accountSelectionDialog.setVisible(false);
	}

	/**
	 * Avaa tilien saldovertailuikkunan.
	 */
	public void showBalanceComparison() {
		StatisticsModel statsModel = new StatisticsModel(registry);

		BalanceComparisonDialog dialog = new BalanceComparisonDialog(this,
				registry, statsModel);

		dialog.create();
		dialog.setVisible(true);
	}

	public void showDocumentNumberShiftDialog() {
		DocumentType documentType = model.getDocumentType();
		int endNumber = Integer.MAX_VALUE;

		if (documentType != null) {
			endNumber = documentType.getNumberEnd();
		}

		DocumentNumberShiftDialog dialog = new DocumentNumberShiftDialog(this, registry);
		dialog.create();

		try {
			dialog.fetchDocuments(model.getDocument().getNumber(), endNumber);
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		dialog.setVisible(true);

		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			refreshModel(false);
		}
	}

	/**
	 * Avaa ALV-kantojen muutosikkunan.
	 */
	public void showVATChangeDialog() {
		VATChangeDialog dialog = new VATChangeDialog(this, registry);
		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Avaa ohjeet Web-selaimessa.
	 */
	public void showHelp() {
		try {
			Desktop.getDesktop().browse(
				new URI("http://helineva.net/tilitin/ohjeet/?v=" + Kirjanpito.APP_VERSION));
		}
		catch (Exception e) {
			SwingUtils.showErrorMessage(this, "Web-selaimen avaaminen epäonnistui. " +
					"Ohjeet löytyvät osoitteesta\n" +
					"http://helineva.net/tilitin/ohjeet/");
		}
	}

	/**
	 * Avaa lokitiedoston tekstieditorissa.
	 */
	public void showLogMessages() {
		File file = Kirjanpito.logFile;

		if (file.exists()) {
			try {
				Desktop.getDesktop().browse(new URI("file://" + file.getAbsolutePath().replace('\\', '/')));
			}
			catch (Exception e) {
				SwingUtils.showErrorMessage(this, String.format(
						"Lokitiedoston %s avaaminen epäonnistui.", file.getAbsolutePath()));
			}
		}
		else {
			SwingUtils.showInformationMessage(this, "Virheenjäljitystietoja ei löytynyt.");
		}
	}

	/**
	 * Näyttää tietoja ohjelmasta.
	 */
	public void showAboutDialog() {
		AboutDialog dialog = new AboutDialog(this);
		dialog.create();
		dialog.setVisible(true);
	}

	/**
	 * Avaa tietokantayhteyden ja hakee tarvittavat tiedot tietokannasta.
	 */
	public void openDataSource() {
		try {
			model.openDataSource();
			boolean initialized = model.initialize();

			/* Lisätään tietokantaan perustiedot, jos niitä ei vielä ole. */
			if (!initialized) {
				initializeDataSource();
			}
		}
		catch (DataAccessException e) {
			String message = "Tietokantayhteyden avaaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			setComponentsEnabled(false, false, false);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}

		updateTitle();
		updatePeriod();
		updatePosition();
		updateDocument();
		updateTotalRow();
		updateEntryTemplates();
		updateDocumentTypes();
		updateTableSettings();
	}

	public void openSqliteDataSource(File file) {
		AppSettings settings = AppSettings.getInstance();
		settings.set("database.url", String.format("jdbc:sqlite:%s",
				file.getAbsolutePath().replace(File.pathSeparatorChar, '/')));
		settings.set("database.username", "");
		settings.set("database.password", "");
		openDataSource();
	}

	protected void refreshModel(boolean positionChanged) {
		try {
			model.fetchDocuments(positionChanged ? -1 : model.getDocumentPosition());
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}

		updatePeriod();
		updatePosition();
		updateDocument();
		updateTotalRow();

		if (searchEnabled) {
			searchEnabled = false;
			updateSearchPanel();
		}
	}

	/**
	 * Päivittää ikkunan otsikkorivin.
	 */
	protected void updateTitle() {
		Settings settings = registry.getSettings();
		String name = (settings == null) ? null : settings.getName();
		String title;

		if (name == null || name.length() == 0) {
			title = Kirjanpito.APP_NAME;
		}
		else {
			title = name + " - " + Kirjanpito.APP_NAME;
		}

		setTitle(title);
	}

	/**
	 * Päivittää tilikauden tiedot tilariville.
	 */
	protected void updatePeriod() {
		Period period = registry.getPeriod();

		if (period == null) {
			periodLabel.setText("");
		}
		else {
			DateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
			String text = "Tilikausi " +
				dateFormat.format(period.getStartDate()) + " - " +
				dateFormat.format(period.getEndDate());
			periodLabel.setText(text);
		}
	}

	/**
	 * Päivittää tositteen järjestysnumeron, tositteiden
	 * lukumäärän ja tositelajin tilariville.
	 */
	protected void updatePosition() {
		int count = model.getDocumentCount();
		int countTotal = model.getDocumentCountTotal();

		if (count != countTotal) {
			documentLabel.setText(String.format("Tosite %d / %d (%d)",
					model.getDocumentPosition() + 1,
					model.getDocumentCount(),
					model.getDocumentCountTotal()));
		}
		else {
			documentLabel.setText(String.format("Tosite %d / %d",
					model.getDocumentPosition() + 1,
					model.getDocumentCount()));
		}

		DocumentType type;

		if (searchEnabled) {
			int index = findDocumentTypeByNumber(model.getDocument().getNumber());
			type = (index < 0) ? null : registry.getDocumentTypes().get(index);
		}
		else {
			type = model.getDocumentType();
		}

		if (type == null) {
			documentTypeLabel.setText("");
		}
		else {
			documentTypeLabel.setText(type.getName());
		}
	}

	/**
	 * Päivittää tositteen tiedot.
	 */
	protected void updateDocument() {
		Document document = model.getDocument();

		if (document == null) {
			numberTextField.setText("");
			dateTextField.setDate(null);
		}
		else {
			numberTextField.setText(Integer.toString(document.getNumber()));
			dateTextField.setDate(document.getDate());
			dateTextField.setBaseDate(document.getDate());

			/* Uuden tositteen päivämäärä on kopioitu edellisestä
			 * tositteesta. Valitaan päivämääräkentän teksti, jotta
			 * uusi päivämäärä voidaan kirjoittaa päälle. */
			if (document.getId() <= 0) {
				dateTextField.select(0, dateTextField.getText().length());
			}

			dateTextField.requestFocus();
		}

		boolean documentEditable = model.isDocumentEditable();
		tableModel.fireTableDataChanged();
		numberTextField.setLockIconVisible(document != null && !documentEditable);
		setComponentsEnabled(document != null, model.isPeriodEditable(), documentEditable);
	}

	/**
	 * Päivittää summarivin tiedot.
	 */
	protected void updateTotalRow() {
		debitTotal = BigDecimal.ZERO;
		creditTotal = BigDecimal.ZERO;
		int count = model.getEntryCount();
		Entry entry;

		for (int i = 0; i < count; i++) {
			entry = model.getEntry(i);

			if (entry.isDebit()) {
				debitTotal = debitTotal.add(model.getVatIncludedAmount(i));
			}
			else {
				creditTotal = creditTotal.add(model.getVatIncludedAmount(i));
			}
		}

		BigDecimal difference = creditTotal.subtract(debitTotal).abs();
		debitTotalLabel.setText(formatter.format(debitTotal));
		creditTotalLabel.setText(formatter.format(creditTotal));
		differenceLabel.setForeground(difference.compareTo(BigDecimal.ZERO) == 0 ?
				debitTotalLabel.getForeground() : Color.RED);
		differenceLabel.setText(formatter.format(difference));
	}

	/**
	 * Päivittää vientimallivalikon.
	 */
	protected void updateEntryTemplates() {
		List<EntryTemplate> templates = registry.getEntryTemplates();
		JMenuItem menuItem;
		int count = 0;

		entryTemplateMenu.removeAll();

		if (templates != null) {
			int prevNumber = -1;

			for (EntryTemplate template : templates) {
				if (template.getNumber() != prevNumber) {
					prevNumber = template.getNumber();
					menuItem = new JMenuItem(template.getName());
					menuItem.addActionListener(entryTemplateListener);

					/* 10 ensimmäiselle vientimallille näppäinoikotie. */
					if (template.getNumber() >= 1 && template.getNumber() <= 10) {
						menuItem.setAccelerator(KeyStroke.getKeyStroke(
								'0' + (template.getNumber() % 10),
								InputEvent.ALT_MASK));
					}

					entryTemplateMenu.add(menuItem);
					menuItem.setActionCommand(Integer.toString(template.getNumber()));
					count++;
				}
			}
		}

		if (count == 0) {
			menuItem = new JMenuItem("Ei vientimalleja");
			menuItem.setEnabled(false);
			entryTemplateMenu.add(menuItem);
		}

		entryTemplateMenu.addSeparator();
		entryTemplateMenu.add(createEntryTemplateMenuItem);
		entryTemplateMenu.add(editEntryTemplatesMenuItem);
	}

	/**
	 * Päivittää tositelajivalikon.
	 */
	protected void updateDocumentTypes() {
		char[] accelerators = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'};
		List<DocumentType> docTypes = registry.getDocumentTypes();
		JCheckBoxMenuItem menuItem;
		int selectedIndex = model.getDocumentTypeIndex();
		int index = 0;

		docTypeMenu.removeAll();

		if (docTypes != null) {
			docTypeMenuItems = new JCheckBoxMenuItem[docTypes.size()];

			for (DocumentType type : docTypes) {
				menuItem = new JCheckBoxMenuItem(type.getName());
				menuItem.addActionListener(docTypeListener);
				menuItem.setSelected(index == selectedIndex);

				/* 10 ensimmäiselle tositelajille näppäinoikotie. */
				if (type.getNumber() >= 1 && type.getNumber() <= 10) {
					menuItem.setAccelerator(KeyStroke.getKeyStroke(
							accelerators[type.getNumber() - 1],
							InputEvent.ALT_MASK));
				}

				docTypeMenu.add(menuItem);
				docTypeMenuItems[index] = menuItem;
				menuItem.setActionCommand(Integer.toString(index++));
			}
		}

		if (index == 0) {
			JMenuItem tmp = new JMenuItem("Ei tositelajeja");
			tmp.setEnabled(false);
			docTypeMenu.add(tmp);
		}

		docTypeMenu.addSeparator();
		docTypeMenu.add(editDocTypesMenuItem);
	}

	/**
	 * Päivittää tositelajivalinnan.
	 *
	 * @param index uuden tositelajin järjestysnumero
	 */
	protected void selectDocumentTypeMenuItem(int index) {
		int oldIndex = model.getDocumentTypeIndex();

		if (!saveDocumentIfChanged()) {
			docTypeMenuItems[index].setSelected(index == oldIndex);
			return;
		}

		if (oldIndex >= 0)
			docTypeMenuItems[oldIndex].setSelected(false);

		docTypeMenuItems[index].setSelected(true);
	}

	/**
	 * Näyttää tai piilottaa hakupaneelin.
	 */
	protected void updateSearchPanel() {
		searchPhraseTextField.setText("");
		searchPanel.setVisible(searchEnabled);
		searchMenuItem.setSelected(searchEnabled);

		if (searchEnabled) {
			searchPhraseTextField.requestFocusInWindow();
		}
	}

	/**
	 * Päivittää vientitaulukon asetukset.
	 */
	protected void updateTableSettings() {
		Settings settings = registry.getSettings();

		if (settings == null) {
			return;
		}

		TableColumnModel columnModel = entryTable.getColumnModel();
		boolean vatVisible = !settings.getProperty("vatVisible", "true").equals("false");

		if (vatVisible && vatColumn != null) {
			/* Näytetään ALV-sarake. */
			columnModel.addColumn(vatColumn);
			columnModel.moveColumn(columnModel.getColumnCount() - 1, 3);
			vatColumn = null;
		}
		else if (!vatVisible && vatColumn == null) {
			/* Piilotetaan ALV-sarake. */
			vatColumn = columnModel.getColumn(3);
			columnModel.removeColumn(vatColumn);
		}

		boolean vatEditable = settings.getProperty("vatLocked", "true").equals("false");
		tableModel.setVatEditable(vatEditable);

		boolean autoCompleteEnabled = !settings.getProperty("autoCompleteEnabled", "true").equals("false");
		model.setAutoCompleteEnabled(autoCompleteEnabled);
	}

	protected int mapColumnIndexToView(int col) {
		int[] indexes = {0, 1, 2, 3, 4};

		if (vatColumn != null) {
			indexes[3] = -1;
			indexes[4] = 3;
		}

		return indexes[col];
	}

	protected int mapColumnIndexToModel(int col) {
		int[] indexes = {0, 1, 2, 3, 4};

		if (vatColumn != null) {
			indexes[3] = 4;
			indexes[4] = -1;
		}

		if (col < 0 || col >= indexes.length) {
			return -1;
		}

		return indexes[col];
	}

	protected void initializeDataSource() {
		setComponentsEnabled(false, false, false);

		DataSourceInitializationModel initModel =
			new DataSourceInitializationModel();

		initModel.update();

		DataSourceInitializationDialog dialog =
			new DataSourceInitializationDialog(this,
					registry, initModel);

		dialog.create();
		dialog.setVisible(true);

		DataSourceInitializationWorker worker = dialog.getWorker();

		if (worker == null) {
			model.closeDataSource();
		}
		else {
			worker.addPropertyChangeListener(
					new InitializationWorkerListener(this, worker));
		}
	}

	protected void setComponentsEnabled(boolean read, boolean create, boolean edit) {
		coaMenuItem.setEnabled(read);
		startingBalancesMenuItem.setEnabled(read);
		propertiesMenuItem.setEnabled(read);
		settingsMenuItem.setEnabled(read);
		gotoMenu.setEnabled(read);
		docTypeMenu.setEnabled(read);
		reportsMenu.setEnabled(read);
		toolsMenu.setEnabled(read);
		prevButton.setEnabled(read);
		nextButton.setEnabled(read);
		findByNumberButton.setEnabled(read);
		searchButton.setEnabled(read);

		newDocMenuItem.setEnabled(create);
		newDocButton.setEnabled(create);
		vatDocumentMenuItem.setEnabled(create);

		deleteDocMenuItem.setEnabled(edit);
		addEntryMenuItem.setEnabled(edit);
		addEntryButton.setEnabled(edit);
		removeEntryMenuItem.setEnabled(edit);
		setIgnoreFlagMenuItem.setEnabled(edit);
		removeEntryButton.setEnabled(edit);
		entryTemplateMenu.setEnabled(edit);
		pasteMenuItem.setEnabled(edit);
		numberTextField.setEditable(edit);
		dateTextField.setEditable(edit);
	}

	/**
	 * Tallentaa tositteen tiedot, jos käyttäjä on tehnyt
	 * muutoksia niihin.
	 *
	 * @return <code>false</code>, jos tallentaminen epäonnistuu
	 */
	protected boolean saveDocumentIfChanged() {
		stopEditing();

		if (!model.isDocumentChanged() || !model.isDocumentEditable()) {
			return true;
		}

		if (logger.isLoggable(Level.FINE)) {
			Document document = model.getDocument();
			logger.fine(String.format("Tallennetaan tosite %d (ID %d)",
					document.getNumber(), document.getId()));
		}

		boolean numberChanged = false;

		try {
			int result = updateModel();

			if (result < 0) {
				return false;
			}

			if (registry.getSettings().getProperty("debitCreditRemark", "false").equals("true")) {
				if (debitTotal.compareTo(creditTotal) != 0) {
					SwingUtils.showInformationMessage(this, "Debet- ja kredit-vientien summat eroavat toisistaan.");
				}
			}

			model.saveDocument();
			numberChanged = (result == 1);
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return false;
		}

		if (numberChanged) {
			refreshModel(false);
		}
		else {
			updatePosition();
		}

		return true;
	}

	/**
	 * Päivittää käyttäjän syöttämät tiedot <code>DocumentModel</code>ille.
	 *
	 * @return -1, jos tiedot ovat virheellisiä; 0, jos tietojen päivittäminen onnistui;
	 * 1, jos päivittäminen onnistui ja tositenumero on muuttunut
	 */
	protected int updateModel() {
		Document document = model.getDocument();
		int result = 0;
		stopEditing();

		try {
			int number = Integer.parseInt(numberTextField.getText());

			if (number != document.getNumber() && JOptionPane.showConfirmDialog(
					this, "Haluatko varmasti muuttaa tositenumeroa?",
					Kirjanpito.APP_NAME, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
					!= JOptionPane.YES_OPTION) {
				numberTextField.setText(Integer.toString(document.getNumber()));
				number = document.getNumber();
			}

			/* Tarkistetaan tositenumeron oikeellisuus, jos käyttäjä on muuttanut sitä. */
			if (number != document.getNumber()) {
				int r;

				try {
					r = model.validateDocumentNumber(number);
				}
				catch (DataAccessException e) {
					String message = "Tositetietojen hakeminen epäonnistui";
					logger.log(Level.SEVERE, message, e);
					SwingUtils.showDataAccessErrorMessage(this, e, message);
					return -2;
				}

				if (r == -1) {
					SwingUtils.showErrorMessage(this, String.format("Tositenumero %d on jo käytössä.", number));
					return -1;
				}
				else if (r == -2) {
					DocumentType documentType = model.getDocumentType();
					SwingUtils.showErrorMessage(this, String.format("Tositenumero %d ei kuulu tositelajin \"%s\" numerovälille (%d-%d).",
							number, documentType.getName(), documentType.getNumberStart(), documentType.getNumberEnd()));
					return -1;
				}

				document.setNumber(number);
				result = 1;
			}
		}
		catch (NumberFormatException e) {
			SwingUtils.showErrorMessage(this, "Virheellinen tositenumero.");
			numberTextField.requestFocusInWindow();
			return -1;
		}

		try {
			document.setDate(dateTextField.getDate());
		}
		catch (ParseException e) {
			SwingUtils.showErrorMessage(this, "Virheellinen päivämäärä.");
			dateTextField.requestFocusInWindow();
			return -1;
		}

		if (document.getDate() == null) {
			SwingUtils.showErrorMessage(this, "Syötä tositteen päivämäärä ennen tallentamista.");
			dateTextField.requestFocusInWindow();
			return -1;
		}

		if (!model.isMonthEditable(document.getDate())) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");
			SwingUtils.showErrorMessage(this, String.format("Kuukausi %s on lukittu.",
					dateFormat.format(document.getDate())));
			dateTextField.requestFocusInWindow();
			return -1;
		}

		Period period = registry.getPeriod();

		if (document.getDate().before(period.getStartDate()) ||
				document.getDate().after(period.getEndDate())) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
			SwingUtils.showErrorMessage(this, String.format("Päivämäärä ei kuulu nykyiselle tilikaudelle\n%s - %s.",
					dateFormat.format(period.getStartDate()),
					dateFormat.format(period.getEndDate())));
			dateTextField.requestFocusInWindow();
			return -1;
		}

		removeEmptyEntry();
		int count = model.getEntryCount();

		for (int i = 0; i < count; i++) {
			if (model.getEntry(i).getAccountId() < 1) {
				SwingUtils.showErrorMessage(this, "Valitse tili ennen tallentamista.");
				entryTable.changeSelection(i, 0, false, false);
				entryTable.requestFocusInWindow();
				return -1;
			}

			if (model.getEntry(i).getAmount() == null) {
				SwingUtils.showErrorMessage(this, "Syötä viennin rahamäärä ennen tallentamista.");
				return -1;
			}
		}

		return result;
	}

	/**
	 * Poistaa viimeisen viennin, jos selite on sama kuin edellisessä viennissä
	 * ja lisäksi rahamäärä on nolla tai tiliä ei ole valittu.
	 */
	protected void removeEmptyEntry() {
		int count = model.getEntryCount();

		if (count > 0) {
			String prevDescription = "";

			if (count - 2 >= 0) {
				prevDescription = model.getEntry(
						count - 2).getDescription();
			}

			Entry lastEntry = model.getEntry(count - 1);

			if ((lastEntry.getAccountId() <= 0 ||
					BigDecimal.ZERO.compareTo(lastEntry.getAmount()) == 0) &&
					lastEntry.getDescription().equals(prevDescription)) {

				model.removeEntry(count - 1);
				tableModel.fireTableRowsDeleted(count - 1, count - 1);
				count--;
			}
		}
	}

	protected void saveDocumentTypeIfChanged() {
		try {
			model.saveDocumentType();
		}
		catch (DataAccessException e) {
			String message = "Asetuksien tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
		}
	}

	/**
	 * Etsii tositelajin, johon tositenumero <code>number</code> kuuluu.
	 *
	 * @param number tositenumero
	 * @return tositelajin järjestysnumero tai -1, jos tositelajia ei löytynyt
	 */
	protected int findDocumentTypeByNumber(int number) {
		int index = 0;

		for (DocumentType type : registry.getDocumentTypes()) {
			if (number >= type.getNumberStart() && number <= type.getNumberEnd()) {
				return index;
			}

			index++;
		}

		return -1;
	}

	/**
	 * Näyttää tulosteiden esikatseluikkunan.
	 *
	 * @param printModel tulosteen malli
	 * @param print tuloste
	 */
	protected void showPrintPreview(PrintModel printModel, Print print) {
		try {
			printModel.run();
		}
		catch (DataAccessException e) {
			String message = "Tulosteen luominen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		print.setSettings(registry.getSettings());
		PrintPreviewModel previewModel;

		if (printPreviewFrame == null) {
			previewModel = new PrintPreviewModel();
			printPreviewFrame = new PrintPreviewFrame(this, previewModel);
			printPreviewFrame.setIconImage(getIconImage());
			printPreviewFrame.create();
		}
		else {
			previewModel = printPreviewFrame.getModel();
		}

		previewModel.setPrintModel(printModel);
		previewModel.setPrint(print);
		printPreviewFrame.updatePrint();
		printPreviewFrame.setVisible(true);
	}

	/**
	 * Sulkee tulosteiden esikatseluikkunan, jos se on auki.
	 */
	private void closePrintPreview() {
		if (printPreviewFrame != null) {
			printPreviewFrame.setVisible(false);
		}
	}

	/**
	 * Lopettaa vientien muokkaamisen.
	 */
	protected void stopEditing() {
		if (entryTable.isEditing())
			entryTable.getCellEditor().stopCellEditing();
	}

	/**
	 * Valitsee taulukon seuraavan solun.
	 */
	protected void moveToNextCell() {
		nextCellAction.actionPerformed(null);
	}

	private RegistryAdapter registryListener = new RegistryAdapter() {
		public void settingsChanged() {
			model.loadLockedMonths();
			updateTableSettings();
			updateDocument();
			updateTitle();
		}

		public void entryTemplatesChanged() {
			updateEntryTemplates();
		}

		public void documentTypesChanged() {
			updateDocumentTypes();
			refreshModel(true);
		}

		public void periodChanged() {
			updatePeriod();
			refreshModel(true);
		}
	};

	private class EntryTableHeaderRenderer extends DefaultTableCellRenderer {
		private TableCellRenderer defaultRenderer;
		private final int[] alignments = {
				JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.LEFT};
		private static final long serialVersionUID = 1L;

		public EntryTableHeaderRenderer(TableCellRenderer defaultRenderer) {
			this.defaultRenderer = defaultRenderer;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			Component comp = defaultRenderer.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);

			if (comp instanceof JLabel) {
				/* Muutetaan tekstin tasaus. */
				((JLabel)comp).setHorizontalAlignment(alignments[mapColumnIndexToModel(column)]);
			}

			return comp;
		}
	};

	/* Uusi tietokanta */
	private ActionListener newDatabaseListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileChooser = new JFileChooser(model.getDatabaseDir());
			fileChooser.setFileFilter(sqliteFileFilter);
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setDialogTitle("Uusi tietokanta");
			File file = null;

			while (true) {
				fileChooser.showSaveDialog(DocumentFrame.this);
				file = fileChooser.getSelectedFile();

				if (file == null) {
					return;
				}

				if (!file.getName().toLowerCase().endsWith(".sqlite")) {
					file = new File(file.getAbsolutePath() + ".sqlite");
				}

				if (file.exists()) {
					SwingUtils.showErrorMessage(DocumentFrame.this, String.format(
							"Tiedosto %s on jo olemassa. Valitse toinen nimi.", file.getAbsolutePath()));
				}
				else {
					break;
				}
			}

			openSqliteDataSource(file);
		}
	};

	/* Avaa tietokanta */
	private ActionListener openDatabaseListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileChooser = new JFileChooser(model.getDatabaseDir());
			fileChooser.setFileFilter(sqliteFileFilter);
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setDialogTitle("Avaa tietokanta");
			fileChooser.showOpenDialog(DocumentFrame.this);
			File file = fileChooser.getSelectedFile();

			if (file == null) {
				return;
			}

			openSqliteDataSource(file);
		}
	};

	FileFilter sqliteFileFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(".sqlite");
		}

		@Override
		public String getDescription() {
			return "Tilitin-tietokannat (.sqlite)";
		}
	};

	/* Tietokanta-asetukset */
	private ActionListener databaseSettingsListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showDatabaseSettings();
		}
	};

	/* Lopeta */
	private ActionListener quitListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			quit();
		}
	};

	/* Edellinen tosite */
	private AbstractAction prevDocListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			goToDocument(DocumentModel.FETCH_PREVIOUS);
		}
	};

	/* Seuraava tosite */
	private AbstractAction nextDocListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			goToDocument(DocumentModel.FETCH_NEXT);
		}
	};

	/* Ensimmäinen tosite */
	private ActionListener firstDocListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			goToDocument(DocumentModel.FETCH_FIRST);
		}
	};

	/* Viimeinen tosite */
	private ActionListener lastDocListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			goToDocument(DocumentModel.FETCH_LAST);
		}
	};

	/* Hae numerolla */
	private ActionListener findDocumentByNumberListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			findDocumentByNumber();
		}
	};

	/* Etsi */
	private ActionListener searchListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			toggleSearchPanel();
		}
	};

	/* Uusi tosite */
	private ActionListener newDocListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			createDocument();
		}
	};

	/* Poista tosite */
	private ActionListener deleteDocListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			deleteDocument();
		}
	};

	/* Muokkaa vientimalleja */
	private ActionListener editEntryTemplatesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			editEntryTemplates();
		}
	};

	/* Luo vientimalli tositteesta */
	private ActionListener createEntryTemplateListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			createEntryTemplateFromDocument();
		}
	};

	/* Vientimalli */
	private ActionListener entryTemplateListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();

			if (command != null) {
				addEntriesFromTemplate(Integer.parseInt(command));
			}
		}
	};

	/* Vie */
	private ActionListener exportListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			export();
		}
	};

	/* Tilikartta */
	private ActionListener chartOfAccountsListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showChartOfAccounts();
		}
	};

	/* Alkusaldot */
	private ActionListener startingBalancesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showStartingBalances();
		}
	};

	/* Perustiedot */
	private ActionListener propertiesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showProperties();
		}
	};

	/* Kirjausasetukset */
	private ActionListener settingsListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showSettings();
		}
	};

	/* Lisää vienti */
	private AbstractAction addEntryListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (!entryTable.isEditing()) {
				addEntry();
			}
		}
	};

	/* Poista vienti */
	private AbstractAction removeEntryListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (!entryTable.isEditing()) {
				removeEntry();

				if (entryTable.getRowCount() == 0)
					dateTextField.requestFocusInWindow();
			}
		}
	};

	/* Muokkaa tositelajeja */
	private ActionListener editDocTypesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			editDocumentTypes();
		}
	};

	/* Kopioi */
	private AbstractAction copyEntriesAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			copyEntries();
		}
	};

	/* Liitä */
	private AbstractAction pasteEntriesAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			pasteEntries();
		}
	};

	/* Tositelaji */
	private ActionListener docTypeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();

			if (command != null) {
				setDocumentType(Integer.parseInt(command));
			}
		}
	};

	/* Tilien saldot */
	private ActionListener printListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();

			if (cmd.equals("accountSummary")) {
				showAccountSummary();
			}
			else if (cmd.equals("document")) {
				showDocumentPrint();
			}
			else if (cmd.equals("accountStatement")) {
				showAccountStatement();
			}
			else if (cmd.equals("incomeStatement")) {
				showIncomeStatement(false);
			}
			else if (cmd.equals("incomeStatementDetailed")) {
				showIncomeStatement(true);
			}
			else if (cmd.equals("balanceSheet")) {
				showBalanceSheet(false);
			}
			else if (cmd.equals("balanceSheetDetailed")) {
				showBalanceSheet(true);
			}
			else if (cmd.equals("generalJournal")) {
				showGeneralJournal();
			}
			else if (cmd.equals("generalLedger")) {
				showGeneralLedger();
			}
			else if (cmd.equals("vatReport")) {
				showVATReport();
			}
			else if (cmd.equals("coa0")) {
				showChartOfAccountsPrint(0);
			}
			else if (cmd.equals("coa1")) {
				showChartOfAccountsPrint(1);
			}
			else if (cmd.equals("coa2")) {
				showChartOfAccountsPrint(2);
			}
		}
	};

	/* Muokkaa tulosteita */
	private ActionListener editReportsListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			editReports();
		}
	};

	/* Tilien saldojen vertailu */
	private ActionListener balanceComparisonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showBalanceComparison();
		}
	};

	/* ALV-tilien päättäminen */
	private ActionListener vatDocumentListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			createVATDocument();
		}
	};

	/* Ohita vienti ALV-laskelmassa */
	private AbstractAction setIgnoreFlagToEntryAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int[] rows = entryTable.getSelectedRows();

			if (rows.length == 0) {
				return;
			}

			boolean ignore = !model.getEntry(rows[0]).getFlag(0);

			for (int index : rows) {
				Entry entry = model.getEntry(index);
				Account account = registry.getAccountById(entry.getAccountId());

				if (account == null) {
					continue;
				}

				if (account.getVatCode() == 2 || account.getVatCode() == 3) {
					entry.setFlag(0, ignore);
				}
				else {
					entry.setFlag(0, false);
				}

				model.setDocumentChanged();
				tableModel.fireTableRowsUpdated(index, index);
			}
		}
	};

	/* Muuta tositenumeroita */
	private ActionListener numberShiftListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showDocumentNumberShiftDialog();
		}
	};

	/* ALV-kantojen muutokset */
	private ActionListener vatChangeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showVATChangeDialog();
		}
	};

	/* Ohje */
	private ActionListener helpListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showHelp();
		}
	};

	/* Virheenjäljitystietoja */
	private ActionListener debugListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showLogMessages();
		}
	};

	/* Tietoja ohjelmasta */
	private ActionListener aboutListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showAboutDialog();
		}
	};

	private AbstractAction prevCellAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int column = mapColumnIndexToModel(entryTable.getSelectedColumn());
			int row = entryTable.getSelectedRow();
			boolean changed = false;

			if (entryTable.isEditing())
				entryTable.getCellEditor().stopCellEditing();

			if (row < 0) {
				addEntry();
				return;
			}

			/* Tilisarakkeesta siirrytään edellisen viennin selitteeseen. */
			if (column == 0) {
				if (row > 0) {
					row--;
					column = 4;
					changed = true;
				}
				else {
					dateTextField.requestFocusInWindow();
					return;
				}
			}
			/* Jos kreditsarakkeessa on 0,00, siirrytään debetsarakkeeseen.
			 * Muussa tapauksessa kredit- ja debetsarakkeesta siirrytään
			 * selitesarakkeeseen. */
			else if (column == 1 || column == 2) {
				BigDecimal amount = model.getEntry(row).getAmount();

				if (amount.compareTo(BigDecimal.ZERO) == 0 && column == 2) {
					column = 1;
				}
				else {
					column = 0;
				}

				changed = true;
			}
			else {
				Entry entry = model.getEntry(row);
				column = entry.isDebit() ? 1 : 2;
				changed = true;
			}

			if (changed) {
				column = mapColumnIndexToView(column);
				entryTable.changeSelection(row, column, false, false);
				entryTable.editCellAt(row, column);
			}
		}
	};

	private AbstractAction nextCellAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int column = mapColumnIndexToModel(entryTable.getSelectedColumn());
			int row = entryTable.getSelectedRow();
			boolean changed = false;

			if (entryTable.isEditing())
				entryTable.getCellEditor().stopCellEditing();

			if (row < 0) {
				addEntry();
				return;
			}

			/* Tilisarakkeesta siirrytään debet- tai kreditsarakkeeseen. */
			if (column == 0) {
				column = model.getEntry(row).isDebit() ? 1 : 2;
				changed = true;
			}
			/* Jos debetsarakkeessa on 0,00, siirrytään kreditsarakkeeseen.
			 * Muussa tapauksessa kredit- ja debetsarakkeesta siirrytään
			 * selitesarakkeeseen. */
			else if (column == 1 || column == 2) {
				BigDecimal amount = model.getEntry(row).getAmount();

				if (amount.compareTo(BigDecimal.ZERO) == 0 && column == 1) {
					column = 2;
				}
				else {
					column = 4;
				}

				changed = true;
			}
			else if (column == 3) {
				column = 4;
				changed = true;
			}
			else {
				int lastRow = entryTable.getRowCount() - 1;

				/* Selitesarakkeesta siirrytään seuraavan rivin
				 * tilisarakkeeseen. */
				if (row < lastRow) {
					column = 0;
					row++;
					changed = true;
				}
				else if (row >= 0) {
					String prevDescription = "";

					if (row > 0) {
						prevDescription = model.getEntry(
								row - 1).getDescription();
					}

					Entry entry = model.getEntry(row);

					/* Siirrytään uuteen tositteeseen, jos vientejä on jo vähintään kaksi
					 * ja selite on sama kuin edellisessä viennissä ja lisäksi
					 * tiliä ei ole valittu tai rahamäärä on nolla. */
					if (row == lastRow && row >= 1 && entry.getDescription().equals(prevDescription) &&
							(entry.getAccountId() < 0 || BigDecimal.ZERO.compareTo(entry.getAmount()) == 0)) {
						createDocument();
					}
					else {
						addEntry();
					}
				}
			}

			if (changed) {
				column = mapColumnIndexToView(column);
				entryTable.changeSelection(row, column, false, false);
				entryTable.editCellAt(row, column);
			}
		}
	};

	private AbstractAction toggleDebitCreditAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (entryTable.getSelectedRowCount() != 1 || entryTable.getSelectedColumnCount() != 1) {
				return;
			}

			int column = entryTable.getSelectedColumn();

			if (column != 1 && column != 2) {
				return;
			}

			boolean editing = entryTable.isEditing();
			int index = entryTable.getSelectedRow();

			if (editing) {
				entryTable.getCellEditor().stopCellEditing();
			}

			boolean addVatEntries = model.getVatAmount(index).compareTo(BigDecimal.ZERO) != 0;
			Entry entry = model.getEntry(index);
			entry.setDebit(!entry.isDebit());
			model.updateAmount(index, model.getVatIncludedAmount(index), addVatEntries);
			model.setDocumentChanged();
			tableModel.fireTableRowsUpdated(index, index);

			if (editing) {
				entryTable.editCellAt(index, entry.isDebit() ? 1 : 2);
			}
		}
	};

	private class PreviousRowAction extends AbstractAction {
		private Action defaultAction;

		private static final long serialVersionUID = 1L;

		public PreviousRowAction(Action defaultAction) {
			this.defaultAction = defaultAction;
		}

		public void actionPerformed(ActionEvent e) {
			int row = entryTable.getSelectedRow();
			defaultAction.actionPerformed(e);

			if (row <= 0) {
				entryTable.transferFocusBackward();
			}
		}
	};

	private class RemoveSuffixAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (entryTable.isEditing()) {
				if (entryTable.getSelectedColumn() != 4) return;
			}
			else {
				int row = entryTable.getSelectedRow();
				if (row < 0) return;
				entryTable.editCellAt(row, 4);
			}

			descriptionCellEditor.removeSuffix();
		}
	};

	private class InitializationWorkerListener implements PropertyChangeListener {
		private Window owner;
		private DataSourceInitializationWorker worker;

		public InitializationWorkerListener(Window owner,
				DataSourceInitializationWorker worker) {

			this.owner = owner;
			this.worker = worker;
		}

		public void propertyChange(PropertyChangeEvent ev) {
			if (ev.getPropertyName().equals("state") &&
					worker.getState() == StateValue.DONE) {

				try {
					worker.get();
				}
				catch (CancellationException e) {
					return;
				}
				catch (Exception e) {
					e.printStackTrace();
					logger.log(Level.SEVERE, "Tietokannan luonti epäonnistui",
							e.getCause());

					SwingUtils.showErrorMessage(owner, "Tietokannan luonti epäonnistui. " +
							e.getCause().getMessage());
					return;
				}

				try {
					model.initialize();
				}
				catch (DataAccessException e) {
					String message = "Tietokantayhteyden avaaminen epäonnistui";
					logger.log(Level.SEVERE, message, e);
					SwingUtils.showDataAccessErrorMessage(owner, e, message);
					return;
				}

				updatePeriod();
				updatePosition();
				updateDocument();
				updateTotalRow();
				updateEntryTemplates();
				updateDocumentTypes();
				updateTableSettings();
			}
		}
	}
}
