package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.Registry;

public class SettingsDialog extends JDialog {
	private Registry registry;
	private JCheckBox vatVisibleCheckBox;
	private JCheckBox vatLockedCheckBox;
	private JCheckBox autoCompleteEnabledCheckBox;
	private JCheckBox debitCreditRemarkCheckBox;
	private JButton okButton;
	private JButton cancelButton;
	private JTable monthTable;
	private SimpleDateFormat dateFormat;
	private Date[] months;
	private boolean[] monthsLocked;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);

	public SettingsDialog(Frame owner, Registry registry) {
		super(owner, "Kirjausasetukset", true);
		this.registry = registry;
		this.dateFormat = new SimpleDateFormat("MMMM yyyy");
	}

	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setLayout(new BorderLayout());
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		buildTableData();
		createContentPanel();
		createButtonPanel();
		loadSettings();
		pack();
		setLocationRelativeTo(getOwner());
	}

	private void createContentPanel() {
		GridBagConstraints c;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.CENTER);

		vatVisibleCheckBox = new JCheckBox("Näytä ALV-sarake");
		vatVisibleCheckBox.setMnemonic('n');
		vatLockedCheckBox = new JCheckBox("Lukitse ALV-sarake");
		vatLockedCheckBox.setMnemonic('l');
		autoCompleteEnabledCheckBox = new JCheckBox("Ota käyttöön vientiselitteen täydennys");
		autoCompleteEnabledCheckBox.setMnemonic('s');
		debitCreditRemarkCheckBox = new JCheckBox("Huomauta, jos debet ja kredit eroavat toisistaan");
		debitCreditRemarkCheckBox.setMnemonic('H');

		c = new GridBagConstraints();
		c.insets = new Insets(0, 5, 5, 0);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		panel.add(vatVisibleCheckBox, c);
		c.gridy = 1;
		panel.add(vatLockedCheckBox, c);
		c.gridy = 2;
		panel.add(autoCompleteEnabledCheckBox, c);
		c.gridy = 3;
		panel.add(debitCreditRemarkCheckBox, c);

		JPanel monthPanel = new JPanel(new BorderLayout());
		monthPanel.setBorder(BorderFactory.createTitledBorder("Lukitse kuukaudet"));
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 4;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(15, 0, 0, 0);
		panel.add(monthPanel, c);

		monthTable = new JTable(tableModel);
		monthTable.setTableHeader(null);
		monthTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		monthTable.setFillsViewportHeight(true);
		monthTable.setPreferredScrollableViewportSize(
				new Dimension(300, 300));
		monthTable.setRowHeight(24);
		monthTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		monthTable.setShowGrid(false);

		TableColumn column;
		column = monthTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(40);
		column.setMaxWidth(40);

		column = monthTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(220);

		monthPanel.add(new JScrollPane(monthTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridBagLayout());
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		c = new GridBagConstraints();

		JButton lockAllMonthsButton = new JButton("Lukitse kaikki");
		lockAllMonthsButton.setMnemonic('k');
		lockAllMonthsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < monthsLocked.length; i++) {
					monthsLocked[i] = true;
				}

				tableModel.fireTableDataChanged();
			}
		});

		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		c.ipadx = 10;
		c.ipady = 4;
		buttonPanel.add(lockAllMonthsButton, c);

		monthPanel.add(buttonPanel, BorderLayout.SOUTH);
	}

	private void createButtonPanel() {
		GridBagConstraints c;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.SOUTH);

		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.setPreferredSize(new Dimension(100, 30));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				acceptChanges();
			}
		});

		cancelButton = new JButton("Peruuta");
		cancelButton.setMnemonic('P');
		cancelButton.setPreferredSize(new Dimension(100, 30));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(0, 0, 0, 5);
		panel.add(cancelButton, c);

		c = new GridBagConstraints();
		panel.add(okButton, c);
		
		rootPane.setDefaultButton(okButton);
	}

	private void buildTableData() {
		Period period = registry.getPeriod();
		ArrayList<Date> monthList = new ArrayList<Date>();
		Calendar cal = Calendar.getInstance();
		Date date = period.getStartDate();
		cal.setTime(date);
		cal.setLenient(true);

		while (date.equals(period.getEndDate()) || date.before(period.getEndDate())) {
			monthList.add(date);
			cal.add(Calendar.MONTH, 1);
			date = cal.getTime();
		}

		months = new Date[monthList.size()];
		monthsLocked = new boolean[monthList.size()];
		monthList.toArray(months);
	}

	private void loadSettings() {
		Settings settings = registry.getSettings();
		vatVisibleCheckBox.setSelected(!settings.getProperty(
				"vatVisible", "true").equals("false"));

		vatLockedCheckBox.setSelected(!settings.getProperty(
				"vatLocked", "true").equals("false"));

		autoCompleteEnabledCheckBox.setSelected(!settings.getProperty(
				"autoCompleteEnabled", "true").equals("false"));

		debitCreditRemarkCheckBox.setSelected(settings.getProperty(
				"debitCreditRemark", "false").equals("true"));

		String key = "locked/" + registry.getPeriod().getId();
		String[] lockedMonths = settings.getProperty(key, "").split(",");
		Arrays.sort(lockedMonths);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");

		for (int i = 0; i < months.length; i++) {
			if (Arrays.binarySearch(lockedMonths, dateFormat.format(months[i])) >= 0) {
				monthsLocked[i] = true;
				tableModel.fireTableRowsUpdated(i, i);
			}
		}
	}

	private void acceptChanges() {
		try {
			saveSettings();
		}
		catch (DataAccessException e) {
			String message = "Asetusten tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}
		
		dispose();
	}
	
	private void saveSettings() throws DataAccessException {
		Settings settings = registry.getSettings();
		settings.setProperty("vatVisible", vatVisibleCheckBox.isSelected() ? "" : "false");
		settings.setProperty("vatLocked", vatLockedCheckBox.isSelected() ? "" : "false");
		settings.setProperty("autoCompleteEnabled", autoCompleteEnabledCheckBox.isSelected() ? "" : "false");
		settings.setProperty("debitCreditRemark", debitCreditRemarkCheckBox.isSelected() ? "true" : "");
		
		String key = "locked/" + registry.getPeriod().getId();
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		int count = 0;

		for (int i = 0; i < months.length; i++) {
			if (monthsLocked[i]) {
				if (sb.length() > 0) {
					sb.append(',');
				}

				sb.append(dateFormat.format(months[i]));
				count++;
			}
		}

		settings.setProperty(key, sb.toString());

		/* Merkitään tilikausi lukituksi, jos kaikki tilikauteen
		 * kuuluvat kuukaudet on lukittu. */
		Period period = registry.getPeriod();
		period.setLocked(count == months.length);

		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		try {
			sess = dataSource.openSession();
			dataSource.getSettingsDAO(sess).save(settings);
			dataSource.getPeriodDAO(sess).save(period);
			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}
		
		registry.fireSettingsChanged();
	}

	private AbstractTableModel tableModel = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;
		private final String[] COLUMN_CAPTIONS = {"Lukittu", "Kuukausi"};
		private final Class<?>[] COLUMN_CLASSES = {Boolean.class, String.class};

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			return months.length;
		}

		@Override
		public String getColumnName(int col) {
			return COLUMN_CAPTIONS[col];
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return COLUMN_CLASSES[col];
		}

		@Override
		public Object getValueAt(int row, int col) {
			if (col == 0) {
				return monthsLocked[row];
			}
			else if (col == 1) {
				return dateFormat.format(months[row]);
			}

			return null;
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if (col == 0) {
				monthsLocked[row] = (Boolean)value;
				fireTableRowsUpdated(row, row);
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col == 0;
		}
	};
}