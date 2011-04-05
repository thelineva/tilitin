package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

import kirjanpito.db.DataAccessException;
import kirjanpito.models.ReportEditorModel;
import kirjanpito.reports.DrawCommandParser;
import kirjanpito.util.AppSettings;

public class ReportEditorDialog extends JDialog {
	private ReportEditorModel model;
	private JTextArea[] textAreas;
	private JTabbedPane tabbedPane;
	private int printIndex;
	private JComboBox printComboBox;
	private JTextArea headerTextArea;
	private JTextArea footerTextArea;
	private JButton restoreHeaderButton;
	private JButton restoreFooterButton;
	private static final String[] REPORT_NAMES = new String[] {
		"Tuloslaskelma", "Tuloslaskelma erittelyin",
		"Tase", "Tase erittelyin"
	};
	
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME); 
	
	public ReportEditorDialog(Frame owner, ReportEditorModel model) {
		super(owner, "Tulosteiden muokkaus", true);
		this.model = model;
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(600, 500);
		setLocationRelativeTo(getOwner());
		
		tabbedPane = new JTabbedPane();
		add(tabbedPane, BorderLayout.CENTER);
		
		tabbedPane.add("Ylä- ja alatunnisteet", createHeaderEditorTab());
		loadHeaderAndFooter();
		
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 11);
		textAreas = new JTextArea[REPORT_NAMES.length];
		JTextArea textArea;
		int index = 0;
		
		for (String id : ReportEditorModel.REPORTS) {
			String name = REPORT_NAMES[index];
			textAreas[index] = textArea = new JTextArea();
			textArea.setFont(font);
			textArea.setText(model.getContent(id));
			tabbedPane.add(name, new JScrollPane(textArea));
			textArea.setCaretPosition(0);
			index++;
		}
		
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());
		
		JButton exportButton = new JButton("Vie");
		exportButton.setMnemonic('V');
		exportButton.setEnabled(Desktop.isDesktopSupported());
		exportButton.setPreferredSize(new Dimension(100, 30));
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToZip();
			}
		});
		
		JButton importButton = new JButton("Tuo");
		importButton.setMnemonic('T');
		importButton.setEnabled(Desktop.isDesktopSupported());
		importButton.setPreferredSize(new Dimension(100, 30));
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadFromZip();
			}
		});
		
		JButton helpButton = new JButton("Ohjeet");
		helpButton.setMnemonic('O');
		helpButton.setEnabled(Desktop.isDesktopSupported());
		helpButton.setPreferredSize(new Dimension(100, 30));
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showHelp();
			}
		});
		
		JButton saveButton = new JButton("Tallenna");
		saveButton.setMnemonic('T');
		saveButton.setPreferredSize(new Dimension(100, 30));
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		
		JButton cancelButton = new JButton("Peruuta");
		cancelButton.setMnemonic('P');
		cancelButton.setPreferredSize(new Dimension(100, 30));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(5, 10, 10, 5);
		c.gridx = 1;
		c.weightx = 0.0;
		panel.add(exportButton, c);
		
		c.gridx = 2;
		c.insets = new Insets(5, 5, 10, 5);
		panel.add(importButton, c);
		
		c.gridx = 3;
		c.weightx = 1.0;
		panel.add(helpButton, c);
		
		c.gridx = 4;
		c.weightx = 0.0;
		panel.add(saveButton, c);
		
		c.gridx = 5;
		c.insets = new Insets(5, 5, 10, 10);
		panel.add(cancelButton, c);
		add(panel, BorderLayout.SOUTH);
	}
	
	private JPanel createHeaderEditorTab() {
		Object[] comboBoxItems = new Object[] {
			"Tilien saldot", "Tosite", "Tiliote",
			"Tuloslaskelma", "Tuloslaskelma erittelyin",
			"Tase", "Tase erittelyin",
			"Päiväkirja", "Pääkirja",
			"ALV-laskelma tileittäin",
			"Tilikartta"
		};
		
		printComboBox = new JComboBox(comboBoxItems);
		printComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveHeaderAndFooter();
				loadHeaderAndFooter();
			}
		});
		
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 11);
		GridBagConstraints c1 = new GridBagConstraints();
		c1.weightx = 1.0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.insets = new Insets(8, 4, 2, 4);
		
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 1;
		c2.insets = new Insets(8, 4, 2, 4);
		
		GridBagConstraints c3 = new GridBagConstraints();
		c3.gridy = 1;
		c3.weightx = 1.0;
		c3.weighty = 1.0;
		c3.fill = GridBagConstraints.BOTH;
		c3.gridwidth = 2;
		c3.insets = new Insets(4, 4, 4, 4);
		
		JPanel tabPanel = new JPanel(new BorderLayout());
		tabPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		tabPanel.add(printComboBox, BorderLayout.NORTH);
		
		JPanel contentPanel = new JPanel(new GridLayout(2, 1));
		tabPanel.add(contentPanel, BorderLayout.CENTER);
		
		JPanel headerPanel = new JPanel(new GridBagLayout());
		contentPanel.add(headerPanel);
		headerTextArea = new JTextArea();
		headerTextArea.setFont(font);
		restoreHeaderButton = new JButton("Palauta");
		restoreHeaderButton.setToolTipText("Palauta alkuperäinen ylätunniste");
		restoreHeaderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				headerTextArea.setText(model.getDefaultHeader(printIndex));
			}
		});
		headerPanel.add(new JLabel("Ylätunniste"), c1);
		headerPanel.add(restoreHeaderButton, c2);
		headerPanel.add(new JScrollPane(headerTextArea), c3);
		
		JPanel footerPanel = new JPanel(new GridBagLayout());
		contentPanel.add(footerPanel);
		footerTextArea = new JTextArea();
		footerTextArea.setFont(font);
		restoreFooterButton = new JButton("Palauta");
		restoreFooterButton.setToolTipText("Palauta alkuperäinen alatunniste");
		restoreFooterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				footerTextArea.setText(model.getDefaultFooter(printIndex));
			}
		});
		footerPanel.add(new JLabel("Alatunniste"), c1);
		footerPanel.add(restoreFooterButton, c2);
		footerPanel.add(new JScrollPane(footerTextArea), c3);
		return tabPanel;
	}
	
	public void save() {
		saveHeaderAndFooter();
		DrawCommandParser parser = new DrawCommandParser();
		
		for (int i = 0; i < REPORT_NAMES.length; i++) {
			try {
				parser.parse(model.getHeader(i));
			}
			catch (ParseException e) {
				JOptionPane.showMessageDialog(this,
						String.format("Virhe tulosteen %s ylätunnisteessa rivillä %d:\n%s.",
								REPORT_NAMES[i].toLowerCase(), e.getErrorOffset(),
								e.getMessage()), Kirjanpito.APP_NAME, JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try {
				parser.parse(model.getFooter(i));
			}
			catch (ParseException e) {
				JOptionPane.showMessageDialog(this,
						String.format("Virhe tulosteen %s alatunnisteessa rivillä %d:\n%s.",
								REPORT_NAMES[i].toLowerCase(), e.getErrorOffset(),
								e.getMessage()), Kirjanpito.APP_NAME, JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		int index = 0;
		
		for (String id : ReportEditorModel.REPORTS) {
			try {
				model.parseContent(id, textAreas[index].getText());
			}
			catch (ParseException e) {
				tabbedPane.setSelectedIndex(index + 1);
				JOptionPane.showMessageDialog(this,
						"Virhe rivillä " + e.getErrorOffset() +
						": " + e.getMessage(), Kirjanpito.APP_NAME,
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			index++;
		}
		
		try {
			model.save();
		}
		catch (DataAccessException e) {
			String message = "Tulostetietojen tallentaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			JOptionPane.showMessageDialog(this,
					"Tulostetietojen tallentaminen epäonnistui.",
					Kirjanpito.APP_NAME, JOptionPane.ERROR_MESSAGE);
		}
		
		dispose();
	}
	
	public void showHelp() {
		try {
			Desktop.getDesktop().browse(
				new URI("http://helineva.net/tilitin/ohjeet/#tulosteiden-muokkaus"));
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this,
					"Web-selaimen avaaminen epäonnistui. " +
					"Ohjeet löytyvät osoitteesta\n" +
					"http://helineva.net/tilitin/ohjeet/#tulosteiden-muokkaus",
					Kirjanpito.APP_NAME, JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void saveHeaderAndFooter() {
		model.setHeader(printIndex, headerTextArea.getText());
		model.setFooter(printIndex, footerTextArea.getText());
	}
	
	public void loadHeaderAndFooter() {
		printIndex = printComboBox.getSelectedIndex();
		headerTextArea.setText(model.getHeader(printIndex));
		footerTextArea.setText(model.getFooter(printIndex));
	}
	
	public void saveToZip() {
		saveHeaderAndFooter();
		AppSettings settings = AppSettings.getInstance();
		String path = settings.getString("print-settings-directory", ".");
		JFileChooser fc = new JFileChooser(path);
		fc.setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".zip");
			}

			public String getDescription() {
				return "Tulosteasetukset";
			}
		});
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			settings.set("print-settings-directory",
					file.getParentFile().getAbsolutePath());
			
			try {
				if (!file.getName().endsWith(".zip")) {
					file = new File(file.getAbsolutePath() + ".zip");
				}
				
				model.saveToZip(file);
			}
			catch (IOException e) {
				logger.log(Level.SEVERE, "Tulosteasetusten vienti epäonnistui", e);
				SwingUtils.showErrorMessage(this,
						"Tulosteasetusten vienti epäonnistui. " + e.getMessage());
			}
		}
	}
	
	public void loadFromZip() {
		saveHeaderAndFooter();
		AppSettings settings = AppSettings.getInstance();
		String path = settings.getString("print-settings-directory", ".");
		JFileChooser fc = new JFileChooser(path);
		fc.setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".zip");
			}

			public String getDescription() {
				return "Tulosteasetukset";
			}
		});
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			settings.set("print-settings-directory",
					file.getParentFile().getAbsolutePath());
			
			try {
				if (!file.getName().endsWith(".zip")) {
					file = new File(file.getAbsolutePath() + ".zip");
				}
				
				model.loadFromZip(file);
			}
			catch (IOException e) {
				logger.log(Level.SEVERE, "Tulosteasetusten tuonti epäonnistui", e);
				SwingUtils.showErrorMessage(this,
						"Tulosteasetusten tuonti epäonnistui. " + e.getMessage());
				return;
			}
		}
		
		loadHeaderAndFooter();
		int index = 0;
		
		for (String id : ReportEditorModel.REPORTS) {
			textAreas[index].setText(model.getContent(id));
			textAreas[index].setCaretPosition(0);
			index++;
		}
	}
}
