package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import kirjanpito.models.DataSourceInitializationModel;
import kirjanpito.models.DataSourceInitializationWorker;
import kirjanpito.util.Registry;

public class DataSourceInitializationDialog extends JDialog {
	private DataSourceInitializationModel model;
	private DataSourceInitializationWorker worker;
	private Registry registry;
	private JComboBox comboBox;
	
	private static final long serialVersionUID = 1L; 
	
	public DataSourceInitializationDialog(Frame owner, Registry registry,
			DataSourceInitializationModel model) {
		
		super(owner, "Tietokannan luonti", true);
		this.registry = registry;
		this.model = model;
	}

	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		String[] comboBoxItems = new String[model.getFileCount()];
		
		for (int i = 0; i < comboBoxItems.length; i++) {
			comboBoxItems[i] = model.getName(i);
		}
		
		comboBox = new JComboBox(comboBoxItems);
		JLabel label = new JLabel("Tilikarttamalli");
		label.setLabelFor(comboBox);
		c.insets = new Insets(10, 10, 10, 5);
		add(label, c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10, 5, 10, 10);
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(comboBox, c);
		
		JButton okButton = new JButton("OK");
		okButton.setPreferredSize(new Dimension(100, 30));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (comboBox.getSelectedIndex() >= 0)
					createWorker();
			}
		});
		
		JButton cancelButton = new JButton("Peruuta");
		cancelButton.setPreferredSize(new Dimension(100, 30));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = c.gridy = 1;
		c.gridwidth = 1;
		c.insets = new Insets(5, 10, 10, 5);
		c.weightx = 1.0;
		add(cancelButton, c);
		
		c.gridx = 2;
		c.insets = new Insets(5, 5, 10, 10);
		c.weightx = 0.0;
		add(okButton, c);
		
		pack();
		rootPane.setDefaultButton(okButton);
		setResizable(false);
		setLocationRelativeTo(getOwner());
	}
	
	public DataSourceInitializationWorker getWorker() {
		return worker;
	}
	
	public void createWorker() {
		int index = comboBox.getSelectedIndex();
		
		worker = new DataSourceInitializationWorker(
					registry.getDataSource(), model.getFile(index));
		
		dispose();
		
		/* Avataan ikkuna, joka näyttää työn edistymisen. */
		TaskProgressDialog dialog = new TaskProgressDialog(getOwner(),
				"Tietokannan luonti", worker);
		dialog.create();
		dialog.setVisible(true);
		worker.execute();
	}
}
