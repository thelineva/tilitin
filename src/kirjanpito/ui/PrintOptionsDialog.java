package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import kirjanpito.db.Period;

/**
 * Tulosteen asetusikkuna.
 *
 * @author Tommi Helineva
 */
public class PrintOptionsDialog extends JDialog {
	protected JButton okButton;
	protected JButton cancelButton;
	private JPanel contentPanel;
	private JRadioButton[] toggleButtons;
	private JSpinner startDateSpinner;
	private JSpinner endDateSpinner;
	private DateTextField startDateTextField;
	private DateTextField endDateTextField;
	private Date startDate;
	private Date endDate;
	private Period period;
	private Date documentDate;
	private int result;

	private static final long serialVersionUID = 1L;

	public PrintOptionsDialog(Frame owner, String title) {
		super(owner, title, true);
	}

	/**
	 * Palauttaa tilikauden.
	 *
	 * @return tilikausi
	 */
	public Period getPeriod() {
		return period;
	}

	/**
	 * Asettaa tilikauden.
	 *
	 * @param period tilikausi
	 */
	public void setPeriod(Period period) {
		this.period = period;
	}

	public void setDateSelectionMode(int mode) {
		toggleButtons[mode].setSelected(true);

		if (mode == 1) {
			monthActionListener.actionPerformed(null);
		}
		else {
			periodActionListener.actionPerformed(null);
		}
	}

	/**
	 * Palauttaa alkamispäivämäärän.
	 *
	 * @return alkamispäivämäärä
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Asettaa alkamispäivämäärän.
	 *
	 * @param startDate alkamispäivämäärä
	 */
	public void setStartDate(Date startDate) {
		startDateTextField.setDate(startDate);
	}

	/**
	 * Palauttaa päättymispäivämäärän.
	 *
	 * @return päättymispäivämäärä
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * Asettaa päättymispäivämäärän.
	 *
	 * @param endDate päättymispäivämäärä
	 */
	public void setEndDate(Date endDate) {
		endDateTextField.setDate(endDate);
	}

	/**
	 * Palauttaa tositepäivämäärän.
	 *
	 * @return tositepäivämäärä
	 */
	public Date getDocumentDate() {
		return documentDate;
	}

	/**
	 * Asettaa tositepäivämäärän.
	 *
	 * @param documentDate tositepäivämäärä
	 */
	public void setDocumentDate(Date documentDate) {
		this.documentDate = documentDate;
	}

	public int getResult() {
		return result;
	}

	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setMinimumSize(getFrameMinimumSize());
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				result = JOptionPane.CANCEL_OPTION;
			}
		});

		createToggleButtonPanel();
		createButtonPanel();
		contentPanel = new JPanel(new GridBagLayout());
		add(contentPanel, BorderLayout.CENTER);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		contentPanel.add(createDateSelectionPanel(), c);
		addExtraOptions(contentPanel);

		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "prevPeriod");
		rootPane.getActionMap().put("prevPeriod", prevPeriodListener);

		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "nextPeriod");
		rootPane.getActionMap().put("nextPeriod", nextPeriodListener);

		pack();
		setLocationRelativeTo(getOwner());
		startDateTextField.requestFocusInWindow();
	}

	private void createToggleButtonPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		BoxLayout layout = new BoxLayout(panel, BoxLayout.LINE_AXIS);
		panel.setLayout(layout);

		toggleButtons = new JRadioButton[3];
		toggleButtons[0] = new JRadioButton("Koko tilikausi");
		toggleButtons[0].setMnemonic('t');
		toggleButtons[0].setSelected(true);
		toggleButtons[0].addActionListener(periodActionListener);

		toggleButtons[1] = new JRadioButton("Kuukausi");
		toggleButtons[1].setMnemonic('K');
		toggleButtons[1].addActionListener(monthActionListener);

		toggleButtons[2] = new JRadioButton("Muu aikaväli");
		toggleButtons[2].setMnemonic('M');

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(toggleButtons[0]);
		buttonGroup.add(toggleButtons[1]);
		buttonGroup.add(toggleButtons[2]);

		panel.add(toggleButtons[0]);
		panel.add(Box.createRigidArea(new Dimension(10, 0)));
		panel.add(toggleButtons[1]);
		panel.add(Box.createRigidArea(new Dimension(10, 0)));
		panel.add(toggleButtons[2]);
		add(panel, BorderLayout.NORTH);
	}

	private JPanel createDateSelectionPanel() {
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());

		startDateTextField = new DateTextField();
		startDateTextField.addFocusListener(startDateFieldFocusListener);
		endDateTextField = new DateTextField();
		endDateTextField.addFocusListener(endDateFieldFocusListener);

		SpinnerDateModel startDateSpinnerModel = new SpinnerDateModel(startDateTextField);
		startDateSpinnerModel.addChangeListener(startDateSpinnerChangeListener);

		SpinnerDateModel endDateSpinnerModel = new SpinnerDateModel(endDateTextField);
		endDateSpinnerModel.addChangeListener(endDateSpinnerChangeListener);

		startDateSpinner = new JSpinner(startDateSpinnerModel);
		startDateSpinner.setEditor(startDateTextField);
		endDateSpinner = new JSpinner(endDateSpinnerModel);
		endDateSpinner.setEditor(endDateTextField);

		JLabel label;

		label = new JLabel("Alkamispäivämäärä");
		label.setDisplayedMnemonic('A');
		label.setLabelFor(startDateTextField);
		c1.anchor = GridBagConstraints.WEST;
		c1.insets = new Insets(10, 10, 10, 5);
		panel.add(label, c1);

		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.insets = new Insets(10, 5, 10, 10);
		c2.weightx = 1.0;
		panel.add(startDateSpinner, c2);

		label = new JLabel("Päättymispäivämäärä");
		label.setDisplayedMnemonic('P');
		label.setLabelFor(endDateTextField);
		c1.gridx = 0;
		c1.gridy = 1;
		panel.add(label, c1);

		c2.gridx = c2.gridy = 1;
		c2.insets = new Insets(10, 5, 10, 10);
		panel.add(endDateSpinner, c2);

		c1.fill = GridBagConstraints.BOTH;
		c1.gridy = 2;
		c1.insets = new Insets(4, 8, 4, 8);
		c1.weightx = 1.0;
		c1.gridwidth = 2;
		panel.add(new JSeparator(SwingConstants.HORIZONTAL), c1);
		return panel;
	}

	private void createButtonPanel() {
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());

		okButton = new JButton("OK");
		okButton.setPreferredSize(new Dimension(100, 30));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				accept();
			}
		});

		cancelButton = new JButton("Peruuta");
		cancelButton.setPreferredSize(new Dimension(100, 30));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = JOptionPane.CANCEL_OPTION;
				dispose();
			}
		});

		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(5, 10, 10, 5);
		c.weightx = 1.0;
		panel.add(cancelButton, c);

		c.gridx = 2;
		c.insets = new Insets(5, 5, 10, 10);
		c.weightx = 0.0;
		panel.add(okButton, c);
		add(panel, BorderLayout.SOUTH);
		rootPane.setDefaultButton(okButton);
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 200);
	}

	protected void addExtraOptions(JPanel panel) {
	}

	public void accept() {
		try {
			startDate = startDateTextField.getDate();
		}
		catch (ParseException e) {
			SwingUtils.showErrorMessage(this, "Virheellinen alkamispäivämäärä.");
			startDateTextField.requestFocus();
			return;
		}

		try {
			endDate = endDateTextField.getDate();
		}
		catch (ParseException e) {
			SwingUtils.showErrorMessage(this, "Virheellinen päättymispäivämäärä.");
			endDateTextField.requestFocus();
			return;
		}

		result = JOptionPane.OK_OPTION;
		dispose();
	}

	private void checkDates() {
		try {
			Date startDate = startDateTextField.getDate();
			Date endDate = endDateTextField.getDate();

			if (startDate == null || endDate == null) {
				return;
			}

			if (period.getStartDate().equals(startDate) && period.getEndDate().equals(endDate)) {
				toggleButtons[0].setSelected(true);
				return;
			}

			toggleButtons[2].setSelected(true);
		}
		catch (ParseException ex) {
		}
	}

	private ActionListener periodActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			startDateTextField.setDate(period.getStartDate());
			endDateTextField.setDate(period.getEndDate());
		}
	};

	private ActionListener monthActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(documentDate);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			startDateTextField.setDate(cal.getTime());
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			endDateTextField.setDate(cal.getTime());
		}
	};

	private AbstractAction prevPeriodListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (toggleButtons[1].isSelected()) {
				toggleButtons[0].setSelected(true);
				periodActionListener.actionPerformed(null);
			}
			else if (toggleButtons[2].isSelected()) {
				toggleButtons[1].setSelected(true);
				monthActionListener.actionPerformed(null);
			}

			startDateTextField.requestFocusInWindow();
		}
	};

	private AbstractAction nextPeriodListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (toggleButtons[0].isSelected()) {
				toggleButtons[1].setSelected(true);
				monthActionListener.actionPerformed(null);
			}
			else if (toggleButtons[1].isSelected()) {
				toggleButtons[2].setSelected(true);
			}

			startDateTextField.requestFocusInWindow();
		}
	};

	private FocusListener startDateFieldFocusListener = new FocusAdapter() {
		@Override
		public void focusLost(FocusEvent e) {
			if (toggleButtons[1].isSelected()) {
				Date date = null;

				try {
					date = startDateTextField.getDate();
				}
				catch (ParseException ex) {
				}

				if (date != null) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);

					if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
						cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
						endDateTextField.setDate(cal.getTime());
						return;
					}
				}
			}

			checkDates();
		}
	};

	private FocusListener endDateFieldFocusListener = new FocusAdapter() {
		@Override
		public void focusLost(FocusEvent e) {
			if (toggleButtons[1].isSelected()) {
				Date date = null;

				try {
					date = endDateTextField.getDate();
				}
				catch (ParseException ex) {
				}

				if (date != null) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);

					if (cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
						cal.set(Calendar.DAY_OF_MONTH, 1);
						startDateTextField.setDate(cal.getTime());
						return;
					}
				}
			}

			checkDates();
		}
	};

	private ChangeListener startDateSpinnerChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (toggleButtons[1].isSelected()) {
				Date date = null;

				try {
					date = startDateTextField.getDate();
				}
				catch (ParseException ex) {
				}

				if (date == null) {
					return;
				}

				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
				endDateTextField.setDate(cal.getTime());
			}
			else {
				checkDates();
			}
		}
	};

	private ChangeListener endDateSpinnerChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (toggleButtons[1].isSelected()) {
				Date date = null;

				try {
					date = endDateTextField.getDate();
				}
				catch (ParseException ex) {
				}

				if (date == null) {
					return;
				}

				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				startDateTextField.setDate(cal.getTime());
			}
			else {
				checkDates();
			}
		}
	};
}
