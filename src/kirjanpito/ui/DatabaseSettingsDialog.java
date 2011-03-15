package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Tietokanta-asetuksien muokkausikkuna.
 * 
 * @author Tommi Helineva
 */
public class DatabaseSettingsDialog extends JDialog {
	private JTextField urlTextField;
	private JTextField usernameTextField;
	private JTextField passwordTextField;
	private JButton openButton;
	private JButton resetButton;
	private String defaultUrl;
	private int result;
	
	private static final long serialVersionUID = 1L;
	
	public DatabaseSettingsDialog(Frame owner) {
		super(owner, "Tietokanta-asetukset", true);
	}
	
	/**
	 * Näyttää virheilmoituksen.
	 * 
	 * @param message teksti
	 */
	public void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this,
				message, Kirjanpito.APP_NAME,
				JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Palauttaa ikkunan tuloksen.
	 * 
	 * @return JOptionPane.OK_OPTION, jos käyttäjä on klikannut
	 * OK-painiketta; JOptionPane.CANCEL_OPTION, jos käyttäjä
	 * on klikannut Peruuta-painiketta
	 */
	public int getResult() {
		return result;
	}
	
	/**
	 * Palauttaa salasanan.
	 * 
	 * @return salasana
	 */
	public String getPassword() {
		return passwordTextField.getText();
	}

	/**
	 * Asettaa salasanan.
	 * 
	 * @param password salasana
	 */
	public void setPassword(String password) {
		passwordTextField.setText(password);
	}

	/**
	 * Palauttaa tietokannan JDBC-URLin.
	 * 
	 * @return tietokannan JDBC-URL
	 */
	public String getURL() {
		return urlTextField.getText();
	}

	/**
	 * Asettaa tietokannan JDBC-URLin.
	 * 
	 * @param url tietokannan JDBC-URL
	 */
	public void setURL(String url) {
		urlTextField.setText(url);
	}

	/**
	 * Palauttaa käyttäjänimen.
	 * 
	 * @return käyttäjänimi
	 */
	public String getUsername() {
		return usernameTextField.getText();
	}

	/**
	 * Asettaa käyttäjänimen.
	 * 
	 * @param username käyttäjänimi
	 */
	public void setUsername(String username) {
		usernameTextField.setText(username);
	}

	/**
	 * Palauttaa tietokantapalvelimen oletusosoitteen.
	 * 
	 * @return oletusosoite
	 */
	public String getDefaultUrl() {
		return defaultUrl;
	}

	/**
	 * Asettaa tietokantapalvelimen oletusosoitteen.
	 * 
	 * @param defaultUrl oletusosoite
	 */
	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}

	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(460, 200));
		createContentPanel();
		createButtonPanel();
		pack();
		setLocationRelativeTo(getOwner());
	}
	
	private void createContentPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.CENTER);
		
		urlTextField = new JTextField();
		urlTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				updateOpenButtonEnabled();
			}
			
			public void insertUpdate(DocumentEvent e) {
				updateOpenButtonEnabled();
			}
			
			public void changedUpdate(DocumentEvent e) {
				updateOpenButtonEnabled();
			}
		});
		
		usernameTextField = new JTextField();
		passwordTextField = new JPasswordField();
		
		addComponent(panel, 0, "URL", urlTextField);
		addComponent(panel, 1, "Käyttäjänimi", usernameTextField);
		addComponent(panel, 2, "Salasana", passwordTextField);
	}
	
	/**
	 * Lisää Swing-komponentin <code>comp</code> säiliöön <code>container</code>
	 * riville <code>rowIndex</code> ja lisää komponentin eteen tekstin
	 * <code>labelText</code>.
	 * 
	 * @param container säiliö, johon komponentti lisätään
	 * @param rowIndex rivinumero
	 * @param labelText teksti
	 * @param comp lisättävä komponentti
	 */
	private void addComponent(JPanel container,
			int rowIndex, String labelText, JComponent comp)
	{
		GridBagConstraints c;
		
		c = new GridBagConstraints();
		c.gridy = rowIndex;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(4, 5, 4, 5);
		container.add(new JLabel(labelText), c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.gridx = 1;
		c.insets = new Insets(4, 5, 4, 5);
		container.add(comp, c);
	}
	
	private void createButtonPanel() {
		GridBagConstraints c = new GridBagConstraints();
		c.ipady = 6;
		c.ipadx = 30;
		c.insets = new Insets(5, 10, 10, 2);
		
		JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
		JPanel container = new JPanel(new GridBagLayout());
		
		openButton = new JButton("Avaa hakemisto");
		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				openDirectory();
			}
		});
		
		resetButton = new JButton("Palauta");
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				urlTextField.setText(defaultUrl);
				usernameTextField.setText("");
				passwordTextField.setText("");
			}
		});
		
		c.anchor = GridBagConstraints.LINE_START;
		container.add(openButton, c);
		c.insets = new Insets(5, 2, 10, 5);
		container.add(resetButton, c);
		
		c.anchor = GridBagConstraints.LINE_END;
		c.weightx = 1.0;
		c.insets = new Insets(5, 5, 10, 10);
		container.add(panel, c);
		add(container, BorderLayout.SOUTH);
		
		JButton cancelButton = new JButton("Peruuta");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = JOptionPane.CANCEL_OPTION;
				setVisible(false);
			}
		});
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = JOptionPane.OK_OPTION;
				setVisible(false);
			}
		});
		
		panel.add(cancelButton);
		panel.add(okButton);
	}
	
	private void openDirectory() {
		File file = new File(urlTextField.getText().substring(12));
		
		try {
			Desktop.getDesktop().open(file.getParentFile());
		}
		catch (Exception e) {
			SwingUtils.showErrorMessage(this,
					"Tiedostoselaimen avaaminen epäonnistui.");
		}
	}
	
	private void updateOpenButtonEnabled() {
		String url = urlTextField.getText();
		openButton.setEnabled(url.length() > 12 && url.startsWith("jdbc:sqlite:"));
	}
}
