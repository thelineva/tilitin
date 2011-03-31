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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.Period;
import kirjanpito.db.Settings;
import kirjanpito.models.PeriodTableModel;
import kirjanpito.models.PropertiesModel;

/**
 * Perustietojen muokkausikkuna.
 * 
 * @author Tommi Helineva
 */
public class PropertiesDialog extends JDialog {
	private PropertiesModel model;
	private JTextField nameTextField;
	private JTextField businessIdTextField;
	private JButton okButton;
	private JButton cancelButton;
	private JTable periodTable;
	private PeriodTableModel periodTableModel;
	
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	
	public PropertiesDialog(Frame owner, PropertiesModel model) {
		super(owner, "Perustiedot", true);
		this.model = model;
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
		
		createContentPanel();
		createButtonPanel();
		
		pack();
		setLocationRelativeTo(getOwner());
		
		Settings settings = model.getSettings();
		nameTextField.setText(settings.getName());
		businessIdTextField.setText(settings.getBusinessId());
	}
	
	private void createContentPanel() {
		GridBagConstraints c;
		
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.CENTER);
		
		c = new GridBagConstraints();
		c.insets = new Insets(0, 5, 5, 0);
		c.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel("Nimi"), c);
		
		nameTextField = new JTextField();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.insets = new Insets(0, 10, 5, 0);
		panel.add(nameTextField, c);
		
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.0;
		c.insets = new Insets(5, 5, 0, 0);
		panel.add(new JLabel("Y-tunnus"), c);
		
		businessIdTextField = new JTextField();
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.weightx = 1.0;
		c.insets = new Insets(5, 10, 0, 0);
		panel.add(businessIdTextField, c);
		
		JPanel periodPanel = new JPanel(new BorderLayout());
		periodPanel.setBorder(BorderFactory.createTitledBorder("Tilikaudet"));
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 2;
		c.gridy = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(15, 0, 0, 0);
		panel.add(periodPanel, c);
		
		periodTableModel = new PeriodTableModel(model);
		periodTable = new JTable(periodTableModel);
		
		TableColumn column;
		column = periodTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(30);
		
		column = periodTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(220);
		column.setCellRenderer(new DateCellRenderer());
		column.setCellEditor(new DateCellEditor());
		
		column = periodTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(220);
		column.setCellRenderer(new DateCellRenderer());
		column.setCellEditor(new DateCellEditor());
		
		periodPanel.add(new JScrollPane(periodTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		
		JButton deletePeriodButton = new JButton("Poista tilikausi");
		deletePeriodButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deletePeriod();
			}
		});
		
		JButton createPeriodButton = new JButton("Uusi tilikausi");
		createPeriodButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createPeriod();
			}
		});
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		c.insets = new Insets(10, 5, 5, 5);
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		buttonPanel.add(deletePeriodButton, c);
		c.weightx = 0.0;
		buttonPanel.add(createPeriodButton, c);
		periodPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		periodTable.setPreferredScrollableViewportSize(
				new Dimension(300, 120));
		periodTable.setRowHeight(24);
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
				save();
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
	
	/**
	 * Tallentaa asetukset tietokantaan.
	 */
	public void save() {
		if (!updateModel())
			return;
		
		try {
			model.save();
		}
		catch (DataAccessException e) {
			String message = "Asetusten tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}
		
		dispose();
	}
	
	/**
	 * Luo uuden tilikauden.
	 */
	public void createPeriod() {
		model.createPeriod();
		periodTableModel.fireTableDataChanged();
	}
	
	/**
	 * Poistaa valitun tilikauden kaikki tiedot.
	 */
	public void deletePeriod() {
		if (model.getPeriodCount() <= 1) {
			JOptionPane.showMessageDialog(this,
					"Tilikautta ei voi poistaa, jos tietokannassa on\n" +
					"vain yksi tilikausi.", Kirjanpito.APP_NAME,
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		int index = model.getCurrentPeriodIndex();
		Period period = model.getPeriod(index);
		
		int result = JOptionPane.showConfirmDialog(this,
				"Haluatko varmasti poistaa valitun tilikauden\n" +
				"(" + dateFormat.format(period.getStartDate()) + " – " +
				dateFormat.format(period.getEndDate()) + ") tiedot? Tietoja ei voi\n" +
				"palauttaa poistamisen jälkeen.",
				Kirjanpito.APP_NAME, JOptionPane.YES_NO_OPTION);
		
		if (result == 0) {
			try {
				model.deletePeriod(index);
			}
			catch (DataAccessException e) {
				String message = "Tilikauden poistaminen epäonnistui";
				logger.log(Level.SEVERE, message, e);
				SwingUtils.showDataAccessErrorMessage(this, e, message);
			}
			
			periodTableModel.fireTableDataChanged();
		}
	}
	
	/**
	 * Päivittää malliin ikkunan tiedot.
	 */
	private boolean updateModel() {
		Settings settings = model.getSettings();
		settings.setName(nameTextField.getText());
		settings.setBusinessId(businessIdTextField.getText());
		
		if (periodTable.isEditing())
			periodTable.getCellEditor().stopCellEditing();
		
		Period period = model.getPeriod(model.getPeriodCount() - 1);
		
		if (period.getStartDate() == null) {
			JOptionPane.showMessageDialog(this,
					"Syötä tilikauden alkamispäivämäärä " +
					"ennen tietojen tallentamista.", Kirjanpito.APP_NAME,
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		if (period.getEndDate() == null) {
			JOptionPane.showMessageDialog(this,
					"Syötä tilikauden päättymispäivämäärä " +
					"ennen tietojen tallentamista.", Kirjanpito.APP_NAME,
					JOptionPane.ERROR_MESSAGE);
			
			return false;
		}
		
		return true;
	}
}
