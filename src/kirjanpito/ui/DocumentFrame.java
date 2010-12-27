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
import kirjanpito.models.ReportEditorModel;
import kirjanpito.models.SettingsModel;
import kirjanpito.models.StartingBalanceModel;
import kirjanpito.models.StatisticsModel;
import kirjanpito.reports.AccountStatementModel;
import kirjanpito.reports.AccountStatementPrint;
import kirjanpito.reports.AccountSummaryModel;
import kirjanpito.reports.AccountSummaryPrint;
import kirjanpito.reports.DocumentPrint;
import kirjanpito.reports.DocumentPrintModel;
import kirjanpito.reports.FinancialStatementModel;
import kirjanpito.reports.FinancialStatementPrint;
import kirjanpito.reports.GeneralJournalModel;
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
	private JMenuItem newDocMenuItem;
	private JMenuItem deleteDocMenuItem;
	private JMenuItem addEntryMenuItem;
	private JMenuItem removeEntryMenuItem;
	private JMenuItem coaMenuItem;
	private JMenuItem vatDocumentMenuItem;
	private JMenuItem exportMenuItem;
	private JMenuItem editEntryTemplatesMenuItem;
	private JMenuItem startingBalancesMenuItem;
	private JMenuItem settingsMenuItem;
	private JCheckBoxMenuItem searchMenuItem;
	private JCheckBoxMenuItem[] docTypeMenuItems;
	private JMenuItem editDocTypesMenuItem;
	private JMenuItem generalLedgerTMenuItem;
	private JCheckBoxMenuItem autoCompleteMenuItem;
	private JButton prevButton;
	private JButton nextButton;
	private JButton newDocButton;
	private JButton addEntryButton;
	private JButton removeEntryButton;
	private JTextField numberTextField;
	private DateTextField dateTextField;
	private JLabel debitTotalLabel;
	private JLabel kreditTotalLabel;
	private JLabel differenceLabel;
	private JLabel documentLabel;
	private JLabel periodLabel;
	private JLabel documentTypeLabel;
	private JTable entryTable;
	private JPanel searchPanel;
	private JTextField searchPhraseTextField;
	private EntryTableModel tableModel;
	private AccountCellRenderer accountCellRenderer;
	private AccountCellEditor accountCellEditor;
	private DescriptionCellEditor descriptionCellEditor;
	private DecimalFormat formatter;
	private AccountSelectionDialog accountSelectionDialog;
	private boolean searchEnabled;
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	private static final long serialVersionUID = 1L;
	
	public DocumentFrame(Registry registry, DocumentModel model) {
		super(Kirjanpito.APP_NAME);
		this.registry = registry;
		this.model = model;
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
		
		/* Luodaan Tosite-valikko. */
		menu = new JMenu("Tosite");
		menu.setMnemonic('T');
		menuBar.add(menu);
		
		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
		newDocMenuItem = SwingUtils.createMenuItem("Uusi", "document-new-16x16.png", 'U',
				KeyStroke.getKeyStroke('N', shortcutKeyMask),
				newDocListener);
		
		deleteDocMenuItem = SwingUtils.createMenuItem("Poista", "delete-16x16.png", 'P',
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
		
		vatDocumentMenuItem = SwingUtils.createMenuItem("ALV-tilien päättäminen",
				null, 'p', KeyStroke.getKeyStroke(KeyEvent.VK_R,
						shortcutKeyMask), vatDocumentListener);
		
		menu.add(addEntryMenuItem);
		menu.add(removeEntryMenuItem);
		menu.add(entryTemplateMenu);
		menu.add(vatDocumentMenuItem);
		menu.addSeparator();
		
		exportMenuItem = SwingUtils.createMenuItem("Vie…",
				null, 'V', null, exportListener);
		
		coaMenuItem = SwingUtils.createMenuItem("Tilikartta…", null, 'T',
				KeyStroke.getKeyStroke(KeyEvent.VK_T,
						shortcutKeyMask), chartOfAccountsListener);
		
		startingBalancesMenuItem = SwingUtils.createMenuItem("Alkusaldot…", null, 's',
				null, startingBalancesListener);
		
		settingsMenuItem = SwingUtils.createMenuItem("Perustiedot…", null, 'e',
				null, settingsListener);
		
		menu.add(exportMenuItem);
		menu.add(coaMenuItem);
		menu.add(startingBalancesMenuItem);
		menu.add(settingsMenuItem);
		
		menu.add(SwingUtils.createMenuItem("Tietokanta-asetukset…", null, 'a', null,
				databaseSettingsListener));
		
		menu.addSeparator();
		
		menu.add(SwingUtils.createMenuItem("Lopeta", "quit-16x16.png", 'L',
				KeyStroke.getKeyStroke('Q', shortcutKeyMask),
				quitListener));
		
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
		
		menuItem = SwingUtils.createMenuItem("Tilien saldot", null, 's', null, printListener);
		menuItem.setActionCommand("accountSummary");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Tosite", null, 'O', null, printListener);
		menuItem.setActionCommand("document");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Tiliote", null, 'T', null, printListener);
		menuItem.setActionCommand("accountStatement");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Tuloslaskelma", null, 'u', null, printListener);
		menuItem.setActionCommand("incomeStatement");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Tuloslaskelma erittelyin", null, 'e', null, printListener);
		menuItem.setActionCommand("incomeStatementDetailed");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Tase", null, 'a', null, printListener);
		menuItem.setActionCommand("balanceSheet");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Tase erittelyin", null, 'e', null, printListener);
		menuItem.setActionCommand("balanceSheetDetailed");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Päiväkirja", null, 'P', null, printListener);
		menuItem.setActionCommand("generalJournal");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("Pääkirja", null, 'k', null, printListener);
		menuItem.setActionCommand("generalLedger");
		menu.add(menuItem);
		
		generalLedgerTMenuItem = menuItem = SwingUtils.createMenuItem("Pääkirja tositelajeittain", null, 'i', null, printListener);
		menuItem.setActionCommand("generalLedgerT");
		menu.add(menuItem);
		
		menuItem = SwingUtils.createMenuItem("ALV-laskelma tileittäin", null, 'V', null, printListener);
		menuItem.setActionCommand("vatReport");
		menu.add(menuItem);
		
		menu.addSeparator();
		menu.add(SwingUtils.createMenuItem("Muokkaa", null, 'M', null, editReportsListener));
		
		/* Luodaan Työkalut-valikko. */
		menu = toolsMenu = new JMenu("Työkalut");
		menu.setMnemonic('y');
		menuBar.add(menu);
		
		menu.add(SwingUtils.createMenuItem("Tilien saldojen vertailu", null, 'T',
				null, balanceComparisonListener));
		
		autoCompleteMenuItem = new JCheckBoxMenuItem("Vientiselitteen täydennys");
		autoCompleteMenuItem.setMnemonic('s');
		autoCompleteMenuItem.addActionListener(autoCompleteListener);
		menu.add(autoCompleteMenuItem);
		
		menu.add(SwingUtils.createMenuItem("ALV-kantojen muutokset", null, 'T',
				null, vatChangeListener));
		
		/* Luodaan Ohje-valikko. */
		menu = new JMenu("Ohje");
		menu.setMnemonic('O');
		menuBar.add(menu);
		
		menu.add(SwingUtils.createMenuItem("Sisältö", "help-16x16.png", 'S',
				KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
				helpListener));
		
		menu.add(SwingUtils.createMenuItem("Tietoja", "about-16x16.png", 'T',
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
		
		numberTextField = new JTextField();
		numberTextField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				dateTextField.requestFocusInWindow();
				e.consume();
			}
			
			public void keyReleased(KeyEvent e) {
				model.setDocumentChanged();
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
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					addEntry();
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					dateTextField.transferFocus();
					
					/* Valitaan ensimmäinen rivi. */
					if (entryTable.getSelectedRow() < 0 ||
							entryTable.getRowCount() > 0) {
						
						entryTable.changeSelection(0,
								Math.max(0, entryTable.getSelectedColumn()),
								false, false);
					}
					
					e.consume();
				}
			}

			public void keyReleased(KeyEvent e) {
				if (Character.isDigit(e.getKeyChar())) {
					model.setDocumentChanged();
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
		
		accountCellRenderer = new AccountCellRenderer(registry);
		accountCellEditor = new AccountCellEditor(registry, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String q = accountCellEditor.getTextField().getText();
				showAccountSelection(q);
			}
		});
		
		descriptionCellEditor = new DescriptionCellEditor(model);
		
		entryTable = new JTable(tableModel);
		entryTable.setFillsViewportHeight(true);
		entryTable.setPreferredScrollableViewportSize(new Dimension(680, 250));
		entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		entryTable.setSurrendersFocusOnKeystroke(true);
		entryTable.getTableHeader().setDefaultRenderer(new EntryTableHeaderRenderer(
				entryTable.getTableHeader().getDefaultRenderer()));
		
		TableColumn column;
		int[] widths = new int[] {190, 80, 80, 80, 190};
		AppSettings settings = AppSettings.getInstance();
		int width;
		
		DescriptionCellEditor descriptionCellEditor = new DescriptionCellEditor(model);
		CurrencyCellRenderer currencyCellRenderer = new CurrencyCellRenderer();
		CurrencyCellEditor currencyCellEditor = new CurrencyCellEditor();
		tableModel.setCurrencyCellEditor(currencyCellEditor);
		
		TableCellRenderer[] renderers = new TableCellRenderer[] {
			accountCellRenderer, currencyCellRenderer, currencyCellRenderer,
			currencyCellRenderer, null };
		
		TableCellEditor[] editors = new TableCellEditor[] {
			accountCellEditor, currencyCellEditor, currencyCellEditor,
			null, descriptionCellEditor };
		
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
		
		setAutoCompleteEnabled(settings.getBoolean("table.auto-complete-enabled", false));
		
		container.add(new JScrollPane(entryTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		
		/* Muutetaan enter-näppäimen toiminta. */
		entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextCell");
		
		entryTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "nextCell");
		
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
		
		/* Kun F12-näppäintä painetaan, aloitetaan selitteen muokkaaminen
		 * ja poistetaan teksti viimeiseen pilkkuun asti. */
		RemoveSuffixAction removeSuffixAction = new RemoveSuffixAction();
		
		entryTable.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "removeSuffix");
		
		entryTable.getActionMap().put("removeSuffix", removeSuffixAction);
		
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
		kreditTotalLabel = new JLabel("0,00");
		kreditTotalLabel.setPreferredSize(new Dimension(80, minSize.height));
		differenceLabel = new JLabel("0,00");
		differenceLabel.setPreferredSize(new Dimension(80, minSize.height));
		
		panel.add(new JLabel("Debet yht."), c);
		panel.add(debitTotalLabel, c);
		panel.add(new JLabel("Kredit yht."), c);
		panel.add(kreditTotalLabel, c);
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
		
		AppSettings settings = AppSettings.getInstance();
		
		/* Tallennetaan ikkunan koko. */
		settings.set("window.width", getWidth());
		settings.set("window.height", getHeight());
		
		/* Tallennetaan taulukon sarakkeiden leveydet. */
		for (int i = 0; i < 5; i++) {
			settings.set("table.columns." + i, entryTable.getColumnModel(
					).getColumn(i).getWidth());
		}
		
		settings.set("table.auto-complete-enabled", model.isAutoCompleteEnabled());
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
				model.fetchDocuments();
			}
			catch (DataAccessException e) {
				String message = "Tositetietojen hakeminen epäonnistui";
				logger.log(Level.SEVERE, message, e);
				SwingUtils.showDataAccessErrorMessage(this, e, message);
				return;
			}
		}
		
		goToDocument(index);
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
				model.fetchDocuments();
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
	public void showSettings() {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		final SettingsModel settingsModel = new SettingsModel(registry);
		
		try {
			settingsModel.initialize();
		}
		catch (DataAccessException e) {
			String message = "Asetuksien hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}
		
		SettingsDialog dialog = new SettingsDialog(
				this, settingsModel);
		
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
		int index = entryTable.getSelectedRow();
		if (index < 0) return;
		stopEditing();
		model.removeEntry(index);
		tableModel.fireTableRowsDeleted(index, index);
		updateTotalRow();
		
		/* Valitaan edellinen vienti. */
		if (index >= 1) {
			entryTable.setRowSelectionInterval(index - 1, index - 1);
		}
		else if (tableModel.getRowCount() > 0) {
			entryTable.setRowSelectionInterval(0, 0);
		}
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
			model.fetchDocuments();
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
		dialog.setPeriod(registry.getPeriod());
		dialog.create();
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
		
		AccountStatementOptionsDialog dialog = new AccountStatementOptionsDialog(this, registry);
		dialog.setPeriod(registry.getPeriod());
		dialog.create();
		dialog.setVisible(true);
		
		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			AccountStatementModel printModel = new AccountStatementModel();
			printModel.setDataSource(registry.getDataSource());
			printModel.setPeriod(registry.getPeriod());
			printModel.setSettings(registry.getSettings());
			printModel.setAccount(dialog.getSelectedAccount());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			showPrintPreview(printModel, new AccountStatementPrint(printModel));
		}
	}
	
	/**
	 * Näyttää tuloslaskelman esikatseluikkunassa.
	 */
	public void showIncomeStatement(boolean detailed) {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		AppSettings settings = AppSettings.getInstance();
		FinancialStatementOptionsDialog dialog =
			new FinancialStatementOptionsDialog(this, "Tuloslaskelma");
		dialog.setPeriod(registry.getPeriod());
		dialog.create();
		dialog.setPreviousPeriodVisible(settings.getBoolean("previous-period", false));
		dialog.showTab(0);
		dialog.setVisible(true);
		
		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			boolean previousPeriodVisible = dialog.isPreviousPeriodVisible();
			settings.set("previous-period", previousPeriodVisible);
			FinancialStatementModel printModel = new FinancialStatementModel();
			printModel.setDataSource(registry.getDataSource());
			printModel.setPeriod(registry.getPeriod());
			printModel.setSettings(registry.getSettings());
			printModel.setAccounts(registry.getAccounts());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			printModel.setTitle("Tuloslaskelma");
			printModel.setReportId(detailed ? "income-statement-detailed" : "income-statement");
			printModel.setPreviousPeriodVisible(previousPeriodVisible);
			showPrintPreview(printModel, new FinancialStatementPrint(
					printModel, detailed ? "incomeStatementDetailed" : "incomeStatement", true));
		}
	}
	
	/**
	 * Näyttää taseen esikatseluikkunassa.
	 */
	public void showBalanceSheet(boolean detailed) {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		AppSettings settings = AppSettings.getInstance();
		FinancialStatementOptionsDialog dialog =
			new FinancialStatementOptionsDialog(this, "Tase");
		dialog.setPeriod(registry.getPeriod());
		dialog.create();
		dialog.setPreviousPeriodVisible(settings.getBoolean("previous-period", false));
		dialog.showTab(0);
		dialog.setVisible(true);
		
		if (dialog.getResult() == JOptionPane.OK_OPTION) {
			boolean previousPeriodVisible = dialog.isPreviousPeriodVisible();
			settings.set("previous-period", previousPeriodVisible);
			FinancialStatementModel printModel = new FinancialStatementModel();
			printModel.setDataSource(registry.getDataSource());
			printModel.setPeriod(registry.getPeriod());
			printModel.setSettings(registry.getSettings());
			printModel.setAccounts(registry.getAccounts());
			printModel.setStartDate(dialog.getStartDate());
			printModel.setEndDate(dialog.getEndDate());
			printModel.setTitle("Tase");
			printModel.setReportId(detailed ? "balance-sheet-detailed" : "balance-sheet");
			printModel.setPreviousPeriodVisible(previousPeriodVisible);
			showPrintPreview(printModel, new FinancialStatementPrint(
					printModel, detailed ? "balanceSheetDetailed" : "balanceSheet", false));
		}
	}
	
	/**
	 * Näyttää päiväkirjan esikatseluikkunassa.
	 */
	public void showGeneralJournal() {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		GeneralJournalModel printModel = new GeneralJournalModel();
		printModel.setRegistry(registry);
		printModel.setPeriod(registry.getPeriod());
		showPrintPreview(printModel, new GeneralJournalPrint(printModel));
	}
	
	/**
	 * Näyttää pääkirjan esikatseluikkunassa.
	 */
	public void showGeneralLedger() {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		GeneralLedgerModel printModel = new GeneralLedgerModel();
		printModel.setRegistry(registry);
		printModel.setPeriod(registry.getPeriod());
		showPrintPreview(printModel, new GeneralLedgerPrint(printModel));
	}
	
	/**
	 * Näyttää pääkirjan tositelajeittain esikatseluikkunassa.
	 */
	public void showGeneralLedgerT() {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		GeneralLedgerModel printModel = new GeneralLedgerModelT();
		printModel.setRegistry(registry);
		printModel.setPeriod(registry.getPeriod());
		showPrintPreview(printModel, new GeneralLedgerPrint(printModel));
	}
	
	/**
	 * Näyttää ALV-laskelman esikatseluikkunassa.
	 */
	public void showVATReport() {
		if (!saveDocumentIfChanged()) {
			return;
		}
		
		PrintOptionsDialog dialog =
			new PrintOptionsDialog(this, "ALV-laskelma");
		
		dialog.setPeriod(registry.getPeriod());
		dialog.create();
		dialog.showTab(1);
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
	
	/**
	 * Avaa ALV-kannan muutosikkunan.
	 */
	public void showVATChangeDialog() {
		VATChangeDialog dialog = new VATChangeDialog(this, registry);
		dialog.create();
		dialog.setVisible(true);
	}
	
	public void setAutoCompleteEnabled(boolean enabled) {
		model.setAutoCompleteEnabled(enabled);
		autoCompleteMenuItem.setSelected(enabled);
	}
	
	/**
	 * Avaa ohjeet Web-selaimessa.
	 */
	public void showHelp() {
		try {
			Desktop.getDesktop().browse(
				new URI("http://helineva.net/tilitin/ohjeet/"));
		}
		catch (Exception e) {
			SwingUtils.showErrorMessage(this, "Web-selaimen avaaminen epäonnistui. " +
					"Ohjeet löytyvät osoitteesta\n" +
					"http://helineva.net/tilitin/ohjeet/");
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
			setComponentsEnabled(false, false);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
		}
		
		updatePeriod();
		updatePosition();
		updateDocument();
		updateTotalRow();
		updateEntryTemplates();
		updateDocumentTypes();
	}
	
	protected void refreshModel() {
		try {
			model.refresh();
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
			
			/* Poistetaan muokkaustoiminnot käytöstä, jos
			 * tilikausi on lukittu. */
			setComponentsEnabled(true, !period.isLocked());
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
			
			/* Uuden tositteen päivämäärä on kopioitu edellisestä
			 * tositteesta. Valitaan päivämääräkentän teksti, jotta
			 * uusi päivämäärä voidaan kirjoittaa päälle. */
			if (document.getId() <= 0) {
				dateTextField.select(0, dateTextField.getText().length());
			}
			
			dateTextField.requestFocus();
		}
		
		tableModel.fireTableDataChanged();
	}
	
	/**
	 * Päivittää summarivin tiedot.
	 */
	protected void updateTotalRow() {
		BigDecimal debitTotal = BigDecimal.ZERO;
		BigDecimal kreditTotal = BigDecimal.ZERO;
		int count = model.getEntryCount();
		Entry entry;
		
		for (int i = 0; i < count; i++) {
			entry = model.getEntry(i);
			
			if (entry.isDebit()) {
				debitTotal = debitTotal.add(model.getVatIncludedAmount(i));	
			}
			else {
				kreditTotal = kreditTotal.add(model.getVatIncludedAmount(i));
			}
		}
		
		BigDecimal difference = kreditTotal.subtract(debitTotal).abs();
		debitTotalLabel.setText(formatter.format(debitTotal));
		kreditTotalLabel.setText(formatter.format(kreditTotal));
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
		
		/* Pääkirja tositelajeittain voidaan tulostaa vain, jos
		 * tositelajit ovat käytössä. */
		generalLedgerTMenuItem.setEnabled(index != 0);
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
	
	protected void initializeDataSource() {
		setComponentsEnabled(false, false);
		
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
	
	protected void setComponentsEnabled(boolean read, boolean write) {
		coaMenuItem.setEnabled(read);
		startingBalancesMenuItem.setEnabled(read);
		settingsMenuItem.setEnabled(read);
		exportMenuItem.setEnabled(read);
		gotoMenu.setEnabled(read);
		docTypeMenu.setEnabled(read);
		reportsMenu.setEnabled(read);
		toolsMenu.setEnabled(read);
		prevButton.setEnabled(read);
		nextButton.setEnabled(read);
		
		newDocMenuItem.setEnabled(write);
		deleteDocMenuItem.setEnabled(write);
		addEntryMenuItem.setEnabled(write);
		removeEntryMenuItem.setEnabled(write);
		vatDocumentMenuItem.setEnabled(write);
		entryTemplateMenu.setEnabled(write);
		newDocButton.setEnabled(write);
		addEntryButton.setEnabled(write);
		removeEntryButton.setEnabled(write);
		dateTextField.setEditable(write);
	}
	
	/**
	 * Tallentaa tositteen tiedot, jos käyttäjä on tehnyt
	 * muutoksia niihin.
	 * 
	 * @return <code>false</code>, jos tallentaminen epäonnistuu
	 */
	protected boolean saveDocumentIfChanged() {
		stopEditing();
		
		if (!model.isDocumentChanged()) {
			return true;
		}
		
		if (logger.isLoggable(Level.FINE)) {
			Document document = model.getDocument();
			logger.fine(String.format("Tallennetaan tosite %d (ID %d)",
					document.getNumber(), document.getId()));
		}
		
		try {
			int result = updateModel();
			
			if (result < 0) {
				return false;
			}
			
			model.saveDocument();
			
			if (result == 1) { // Tositenumeroa muutettu
				/* Kaikki tositetiedot on haettava tietokannasta uudelleen, koska
				 * tositteiden järjestys on muuttunut. */
				model.refresh();
			}
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return false;
		}
		
		updatePosition();
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
			
			if (count - 1 > 0) {
				prevDescription = model.getEntry(
						count - 1).getDescription();
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
	 * Näyttää tulosteen esikatseluikkunan.
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
		PrintPreviewModel previewModel = new PrintPreviewModel();
		previewModel.setPrintModel(printModel);
		previewModel.setPrint(print);
		PrintPreviewDialog frame = new PrintPreviewDialog(this, previewModel);
		frame.create();
		frame.setVisible(true);
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
			updateTitle();
		}
		
		public void entryTemplatesChanged() {
			updateEntryTemplates();
		}
		
		public void documentTypesChanged() {
			updateDocumentTypes();
			refreshModel();
		}
		
		public void periodChanged() {
			updatePeriod();
			refreshModel();
		}
	};
	
	private static class EntryTableHeaderRenderer extends DefaultTableCellRenderer {
		private TableCellRenderer defaultRenderer;
		private static final long serialVersionUID = 1L;
		private static final int[] alignments = {
			JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.LEFT};
		
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
				((JLabel)comp).setHorizontalAlignment(alignments[column]);
			}
			
			return comp;
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
	
	/* Muokkaa vientimalleja */
	private ActionListener editEntryTemplatesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			editEntryTemplates();
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
	
	/* ALV-tilien päättäminen */
	private ActionListener vatDocumentListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			createVATDocument();
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
			else if (cmd.equals("generalLedgerT")) {
				showGeneralLedgerT();
			}
			else if (cmd.equals("vatReport")) {
				showVATReport();
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
	
	/* Vientiselitteen täydennys */
	private ActionListener autoCompleteListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			setAutoCompleteEnabled(!model.isAutoCompleteEnabled());
		}
	};
	
	/* ALV-kannan muutokset */
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
	
	/* Tietoja */
	private ActionListener aboutListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showAboutDialog();
		}
	};
	
	private AbstractAction nextCellAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int column = entryTable.getSelectedColumn();  
			int row = entryTable.getSelectedRow();
			boolean changed = false;
			
			if (entryTable.isEditing())
				entryTable.getCellEditor().stopCellEditing();
			
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
				entryTable.changeSelection(row, column, false, false);
				entryTable.editCellAt(row, column);
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
			}
		}
	}
}
