package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import kirjanpito.db.DataAccessException;
import kirjanpito.models.StatisticsModel;
import kirjanpito.models.StatisticsTableModel;
import kirjanpito.util.AppSettings;
import kirjanpito.util.Registry;

public class BalanceComparisonDialog extends JDialog {
	private StatisticsModel model;
	private StatisticsTableModel tableModel;
	private JTable table;
	private DateTextField startDateTextField;
	private DateTextField endDateTextField;
	private JRadioButton weeklyRadioButton;
	private JRadioButton monthlyRadioButton;
	private JButton saveButton;
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	private static final long serialVersionUID = 1L;

	public BalanceComparisonDialog(Frame owner, Registry registry, StatisticsModel model) {
		super(owner, "Tilien saldojen vertailu", true); 
		this.model = model;
	}
	
	public void create() {
		createToolBar();
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		add(contentPanel, BorderLayout.CENTER);
		
		createPanel(contentPanel);
		createTable(contentPanel);
		pack();
		setLocationRelativeTo(getOwner());
		
		Calendar cal = Calendar.getInstance();
		endDateTextField.setDate(cal.getTime());
		cal.setLenient(true);
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		startDateTextField.setDate(cal.getTime());
	}
	
	private void createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		JButton updateButton = SwingUtils.createToolButton("refresh-22x22.png",
				"Päivitä", updateListener, true);
		
		saveButton = SwingUtils.createToolButton("save-22x22.png",
				"Vie", saveListener, true);
		saveButton.setEnabled(false);
		
		JButton closeButton = SwingUtils.createToolButton("close-22x22.png",
				"Sulje", closeListener, true);
		
		toolBar.add(updateButton);
		toolBar.add(saveButton);
		toolBar.add(closeButton);
		add(toolBar, BorderLayout.PAGE_START);
	}
	
	private void createPanel(JPanel container) {
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		container.add(panel, BorderLayout.NORTH);
		
		startDateTextField = new DateTextField();
		endDateTextField = new DateTextField();
		
		JLabel label = new JLabel("Alkaa");
		label.setDisplayedMnemonic('A');
		label.setLabelFor(startDateTextField);
		
		c1.insets = new Insets(5, 5, 5, 5);
		c1.anchor = GridBagConstraints.LINE_START;
		panel.add(label, c1);
		
		c2.insets = new Insets(2, 5, 2, 5);
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 0.7;
		panel.add(startDateTextField, c2);
		
		label = new JLabel("Päättyy");
		label.setDisplayedMnemonic('P');
		label.setLabelFor(endDateTextField);
		
		c1.gridx = 0;
		c1.gridy = 1;
		panel.add(label, c1);
		
		c2.gridx = 1;
		c2.gridy = 1;
		panel.add(endDateTextField, c2);
		
		c1.insets = new Insets(0, 5, 0, 5);
		c1.anchor = GridBagConstraints.CENTER;
		c1.fill = GridBagConstraints.VERTICAL;
		c1.gridheight = 2;
		c1.gridx = 2;
		c1.gridy = 0;
		panel.add(new JSeparator(SwingConstants.VERTICAL), c1);
		
		weeklyRadioButton = new JRadioButton("Viikoittain", true);
		weeklyRadioButton.setMnemonic('V');
		c2.weightx = 0.2;
		c2.gridx = 3;
		c2.gridy = 0;
		panel.add(weeklyRadioButton, c2);
		
		monthlyRadioButton = new JRadioButton("Kuukausittain", false);
		monthlyRadioButton.setMnemonic('K');
		c2.gridx = 3;
		c2.gridy = 1;
		panel.add(monthlyRadioButton, c2);
		
		ButtonGroup periodGroup = new ButtonGroup();
		periodGroup.add(weeklyRadioButton);
		periodGroup.add(monthlyRadioButton);
	}
	
	private void createTable(JPanel container) {
		tableModel = new StatisticsTableModel(model);
		table = new JTable(tableModel);
		table.setPreferredScrollableViewportSize(new Dimension(600, 380));
		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		container.add(new JScrollPane(table,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
	}
	
	public void updateTable() {
		try {
			model.setStartDate(startDateTextField.getDate());
			model.setEndDate(endDateTextField.getDate());
		}
		catch (ParseException e) {
			SwingUtils.showErrorMessage(this, "Virheellinen päivämäärä.");
			return;
		}
		
		try {
			if (monthlyRadioButton.isSelected()) {
				model.calculateMonthlyStatistics();
			}
			else {
				model.calculateWeeklyStatistics();
			}
		}
		catch (DataAccessException e) {
			String message = "Tositetietojen hakeminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}
		
		tableModel.fireTableStructureChanged();
		CurrencyCellRenderer renderer = new CurrencyCellRenderer();
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		int colWidth = (getWidth() - 200) / model.getPeriodCount();
		
		for (int i = 1; i <= model.getPeriodCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer(renderer);
			table.getColumnModel().getColumn(i).setPreferredWidth(colWidth);
		}
		
		saveButton.setEnabled(true);
	}
	
	public void save() {
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
			
			try {
				model.save(file);
			}
			catch (IOException e) {
				logger.log(Level.SEVERE, "Tietojen tallentaminen epäonnistui", e);
				SwingUtils.showErrorMessage(this,
						"Tietojen tallentaminen epäonnistui. " + e.getMessage());
			}
		}
	}
	
	private ActionListener updateListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateTable();
		}
	};
	
	private ActionListener saveListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			save();
		}
	};
	
	private ActionListener closeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	};
}
