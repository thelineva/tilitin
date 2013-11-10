package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Period;
import kirjanpito.db.Session;
import kirjanpito.db.Settings;
import kirjanpito.util.Registry;

public class FinancialStatementOptionsDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private Registry registry;
	private int type;
	private JButton okButton;
	private JButton cancelButton;
	private JButton resetButton;
	private JSpinner[] startDateSpinners;
	private DateTextField[] startDateFields;
	private JSpinner[] endDateSpinners;
	private DateTextField[] endDateFields;
	private Date[] startDates;
	private Date[] endDates;
	private List<Period> periods;
	private JCheckBox pageBreakCheckBox;
	private CustomFocusTraversalPolicy focusPolicy;

	public static final int TYPE_INCOME_STATEMENT = 1;
	public static final int TYPE_BALANCE_SHEET = 2;
	private static final int NUM_COLUMNS = 3;
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);

	public FinancialStatementOptionsDialog(Registry registry,
			Frame owner, String title, int type) {
		super(owner, title, true);
		this.registry = registry;
		this.type = type;
		this.focusPolicy = new CustomFocusTraversalPolicy();
	}

	public Date[] getStartDates() {
		return startDates;
	}

	public Date[] getEndDates() {
		return endDates;
	}

	public boolean isPageBreakEnabled() {
		return pageBreakCheckBox.isSelected();
	}

	public void setPageBreakEnabled(boolean enabled) {
		pageBreakCheckBox.setSelected(enabled);
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 250);
	}

	public void fetchData() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			periods = dataSource.getPeriodDAO(sess).getAll();
			Period currentPeriod = registry.getPeriod();

			for (int i = periods.size() - 1; i >= 0; i--) {
				if (periods.get(i).getStartDate().after(currentPeriod.getStartDate())) {
					periods.remove(i);
				}
			}
		}
		finally {
			if (sess != null) sess.close();
		}
	}

	public void create() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setMinimumSize(getFrameMinimumSize());
		createContentPanel();
		createButtonPanel();
		pack();
		setLocationRelativeTo(getOwner());
		setFocusTraversalPolicy(focusPolicy);

		Settings settings = registry.getSettings();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String prefix = (type == TYPE_INCOME_STATEMENT) ? "income-statement." : "balance-sheet.";
		boolean empty = true;
		String key;

		for (int i = 0; i < NUM_COLUMNS; i++) {
			if (type == TYPE_INCOME_STATEMENT) {
				key = prefix + i + ".start-date";

				try {
					startDateFields[i].setDate(dateFormat.parse(
							settings.getProperty(key, "")));
					empty = false;
				}
				catch (ParseException e) {
				}
			}

			key = prefix + i + ".end-date";

			try {
				endDateFields[i].setDate(dateFormat.parse(
						settings.getProperty(key, "")));
				empty = false;
			}
			catch (ParseException e) {
			}
		}

		if (empty) {
			reset();
		}
	}

	public void accept() {
		int numCols = 0;

		for (int i = 0; i < NUM_COLUMNS; i++) {
			if ((type == TYPE_INCOME_STATEMENT &&
					!startDateFields[i].getText().isEmpty()) ||
					!endDateFields[i].getText().isEmpty()) {
				numCols++;
			}
		}

		if (numCols == 0) {
			if (type == TYPE_INCOME_STATEMENT) {
				SwingUtils.showInformationMessage(this, "Syötä alkamis- ja päättymispäivämäärä.");
			}
			else {
				SwingUtils.showInformationMessage(this, "Syötä päivämäärä.");
			}

			return;
		}

		Date[] startDates = new Date[numCols];
		Date[] endDates = new Date[numCols];
		int j = 0;

		for (int i = 0; i < NUM_COLUMNS; i++) {
			if (type == TYPE_INCOME_STATEMENT &&
					startDateFields[i].getText().isEmpty() &&
					endDateFields[i].getText().isEmpty()) {
				continue;
			}

			if (type != TYPE_INCOME_STATEMENT &&
					endDateFields[i].getText().isEmpty()) {
				continue;
			}

			if (type == TYPE_INCOME_STATEMENT) {
				try {
					startDates[j] = startDateFields[i].getDate();
				}
				catch (ParseException e) {
					SwingUtils.showInformationMessage(this, "Virheellinen alkamispäivämäärä");
					return;
				}

				if (startDates[j] == null) {
					SwingUtils.showInformationMessage(this, "Syötä alkamispäivämäärä");
					return;
				}
			}

			try {
				endDates[j] = endDateFields[i].getDate();
			}
			catch (ParseException e) {
				SwingUtils.showInformationMessage(this, "Virheellinen päättymispäivämäärä");
				return;
			}

			if (endDates[j] == null) {
				SwingUtils.showInformationMessage(this, "Syötä päättymispäivämäärä");
				return;
			}

			j++;
		}

		Settings settings = registry.getSettings();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String prefix = (type == TYPE_INCOME_STATEMENT) ? "income-statement." : "balance-sheet.";
		String key;

		for (int i = 0; i < NUM_COLUMNS; i++) {
			if (type == TYPE_INCOME_STATEMENT) {
				key = prefix + i + ".start-date";
				settings.setProperty(key, (i >= startDates.length || startDates[i] == null) ? null :
					dateFormat.format(startDates[i]));
			}

			key = prefix + i + ".end-date";
			settings.setProperty(key, (i >= endDates.length || endDates[i] == null) ? null :
				dateFormat.format(endDates[i]));
		}

		try {
			saveSettings();
		}
		catch (DataAccessException e) {
			String message = "Asetusten tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}

		this.startDates = startDates;
		this.endDates = endDates;
		dispose();
	}

	public void reset() {
		Period period = periods.get(periods.size() - 1);

		if (type == TYPE_INCOME_STATEMENT) {
			startDateSpinners[0].setValue(period.getStartDate());
		}

		endDateSpinners[0].setValue(period.getEndDate());

		if (periods.size() > 1) {
			period = periods.get(periods.size() - 2);

			if (type == TYPE_INCOME_STATEMENT) {
				startDateSpinners[1].setValue(period.getStartDate());
			}

			endDateSpinners[1].setValue(period.getEndDate());
		}
		else {
			if (type == TYPE_INCOME_STATEMENT) {
				startDateSpinners[1].setValue(null);
			}

			endDateSpinners[1].setValue(null);
		}

		if (type == TYPE_INCOME_STATEMENT) {
			startDateSpinners[2].setValue(null);
		}

		endDateSpinners[2].setValue(null);
	}

	private void createContentPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		add(panel, BorderLayout.CENTER);

		startDateSpinners = new JSpinner[NUM_COLUMNS];
		endDateSpinners = new JSpinner[NUM_COLUMNS];
		startDateFields = new DateTextField[NUM_COLUMNS];
		endDateFields = new DateTextField[NUM_COLUMNS];
		c.insets = new Insets(4, 8, 4, 8);
		c.anchor = GridBagConstraints.BASELINE_LEADING;
		c.gridx = 0;

		if (type == TYPE_INCOME_STATEMENT) {
			c.gridy = 1;
			panel.add(new JLabel("Alkamispäivämäärä"), c);
		}

		c.gridy = 2;
		panel.add(new JLabel((type == TYPE_INCOME_STATEMENT) ?
				"Päättymispäivämäärä" : "Päivämäärä"), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;

		for (int i = 0; i < NUM_COLUMNS; i++) {
			c.gridy = 0;
			c.gridx = 1 + i;
			c.insets = new Insets(20, 8, 4, 8);
			panel.add(new JLabel("Sarake " + (i + 1)), c);
			c.insets = new Insets(4, 8, 4, 8);

			if (type == TYPE_INCOME_STATEMENT) {
				c.gridy = 1;
				final int fieldIndex = i;
				startDateFields[i] = new DateTextField();
				startDateFields[i].setColumns(10);
				startDateFields[i].addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						try {
							Date startDate = startDateFields[fieldIndex].getDate();
							Date endDate = endDateFields[fieldIndex].getDate();

							if (startDate == null || (endDate != null && !endDate.before(startDate))) {
								return;
							}

							Calendar cal = Calendar.getInstance();
							cal.setTime(startDate);

							if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
								cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
								endDateFields[fieldIndex].setDate(cal.getTime());
							}
						}
						catch (ParseException ex) {
						}
					}
				});
				startDateSpinners[i] = new JSpinner(new SpinnerDateModel(startDateFields[i]));
				startDateSpinners[i].setEditor(startDateFields[i]);
				panel.add(startDateSpinners[i], c);
				focusPolicy.add(startDateFields[i]);
			}

			c.gridy = 2;
			endDateFields[i] = new DateTextField();
			endDateFields[i].setColumns(10);
			endDateSpinners[i] = new JSpinner(new SpinnerDateModel(endDateFields[i]));
			endDateSpinners[i].setEditor(endDateFields[i]);
			panel.add(endDateSpinners[i], c);
			focusPolicy.add(endDateFields[i]);
		}

		c.anchor = GridBagConstraints.SOUTHWEST;
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 4;
		c.weighty = 1.0;
		c.insets = new Insets(4, 12, 12, 12);

		if (type == TYPE_BALANCE_SHEET) {
			pageBreakCheckBox = new JCheckBox("Vastattavaa eri sivulle");
			pageBreakCheckBox.setMnemonic('V');
			panel.add(pageBreakCheckBox, c);
			focusPolicy.add(pageBreakCheckBox);
		}
		else {
			panel.add(new JLabel(), c);
		}
	}

	private void createButtonPanel() {
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());

		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				accept();
			}
		});

		cancelButton = new JButton("Peruuta");
		cancelButton.setMnemonic('P');
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		c.insets = new Insets(5, 10, 10, 5);
		c.ipadx = 20;
		c.ipady = 10;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		resetButton = new JButton((periods.size() == 1) ? "Koko tilikausi" :
			"Nykyinen ja edellinen tilikausi");
		resetButton.setMnemonic('t');
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});

		panel.add(resetButton, c);

		c.weightx = 0.0;
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		panel.add(cancelButton, c);

		c.gridx = 2;
		c.insets = new Insets(5, 5, 10, 10);
		c.weightx = 0.0;
		panel.add(okButton, c);
		add(panel, BorderLayout.SOUTH);
		rootPane.setDefaultButton(okButton);

		focusPolicy.add(okButton);
		focusPolicy.add(cancelButton);
		focusPolicy.add(resetButton);
	}

	private void saveSettings() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;

		try {
			sess = dataSource.openSession();
			dataSource.getSettingsDAO(sess).save(registry.getSettings());
			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}
	}

	private static class CustomFocusTraversalPolicy extends FocusTraversalPolicy {
		private ArrayList<Component> order;

		public CustomFocusTraversalPolicy() {
			order = new ArrayList<Component>();
		}

		public void add(Component component) {
			order.add(component);
		}

		public Component getComponentAfter(Container focusCycleRoot,
				Component component)
		{
			int idx = (order.indexOf(component) + 1) % order.size();
			return order.get(idx);
		}

		public Component getComponentBefore(Container focusCycleRoot,
				Component component)
		{
			int idx = order.indexOf(component) - 1;

			if (idx < 0) {
				idx = order.size() - 1;
			}

			return order.get(idx);
		}

		public Component getDefaultComponent(Container focusCycleRoot) {
			return order.get(0);
		}

		public Component getLastComponent(Container focusCycleRoot) {
			return order.get(order.size() - 1);
		}

		public Component getFirstComponent(Container focusCycleRoot) {
			return order.get(0);
		}
	}
}
