package kirjanpito.ui;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import kirjanpito.db.DataAccessException;
import kirjanpito.ui.resources.Resources;

public class SwingUtils {
	private SwingUtils() { }
	
	/**
	 * Näyttää virheilmoituksen.
	 * 
	 * @param parent ikkuna
	 * @param message teksti
	 */
	public static void showErrorMessage(Component parent, String message) {
		JOptionPane.showMessageDialog(parent,
				message, Kirjanpito.APP_NAME,
				JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Näyttää tietokantavirheilmoituksen.
	 * 
	 * @param parent ikkuna
	 * @param e poikkeus
	 * @param message teksti
	 */
	public static void showDataAccessErrorMessage(Component parent,
			DataAccessException e, String message) {
		
		showErrorMessage(parent, message + ". Tietokantavirhe: " + e.getMessage() +
				"\nTarkista tietokanta-asetukset.");
	}
	
	/**
	 * Näyttää tietokantavirheilmoituksen.
	 * 
	 * @param parent ikkuna
	 * @param e poikkeus
	 */
	public static void showDataAccessErrorMessage(Component parent,
			DataAccessException e) {
		
		showErrorMessage(parent, "Tietokantavirhe: " + e.getMessage() +
				"\nTarkista tietokanta-asetukset.");
	}
	
	/**
	 * Näyttää ilmoituksen.
	 * 
	 * @param parent ikkuna
	 * @param message teksti
	 */
	public static void showInformationMessage(Component parent, String message) {
		JOptionPane.showMessageDialog(parent,
				message, Kirjanpito.APP_NAME,
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Apumetodi, jolla luodaan JMenuItem-olioita.
	 * 
	 * @param text teksti
	 * @param imageName kuvatiedoston nimi
	 * @param mnemonic <i>mnemonic</i>
	 * @param accelerator pikanäppäin (<i>accelerator</i>)
	 * @param listener kuuntelija
	 * @return luotu JMenuItem-olio
	 */
	public static JMenuItem createMenuItem(String text,
			String imageName, char mnemonic, KeyStroke accelerator,
			ActionListener listener)
	{
		JMenuItem menuItem;
		
		if (imageName == null) {
			menuItem = new JMenuItem(text);
		}
		else {
			menuItem = new JMenuItem(text,
					new ImageIcon(Resources.load(imageName)));
		}
		
		menuItem.addActionListener(listener);
		menuItem.setMnemonic(mnemonic);
		menuItem.setAccelerator(accelerator);
		return menuItem;
	}
	
	/**
	 * Apumetodi, jolla luodaan työkalurivin painikkeita.
	 * 
	 * @param imageName kuvatiedoston nimi
	 * @param text teksti, joka näytetään painikkeessa tai työkaluvihjeessä
	 * @param listener kuuntelija
	 * @param textVisible <code>true</code>, jos painikkeen teksti näytetään
	 * @return luotu JButton-olio
	 */
	public static JButton createToolButton(String imageName,
			String text, ActionListener listener, boolean textVisible)
	{
		JButton button = new JButton();
		
		if (textVisible) {
			button.setText(text);
		}
		else {
			button.setToolTipText(text);
		}
		
		button.addActionListener(listener);
		button.setIcon(new ImageIcon(Resources.load(imageName)));
		return button;
	}
}
