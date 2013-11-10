package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

import kirjanpito.ui.resources.Resources;

/**
 * Tietoja ohjelmasta -ikkuna.
 * 
 * @author Tommi Helineva
 */
public class AboutDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public AboutDialog(Frame owner) {
		super(owner, "Tietoja ohjelmasta " +
				Kirjanpito.APP_NAME, true);
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setLayout(new GridBagLayout());
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		
		GridBagConstraints c = new GridBagConstraints();

		ImageIcon icon = new ImageIcon(Resources.load("tilitin-48x48.png"));
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(15, 10, 5, 10);
		c.weightx = 1.0;
		c.weighty = 0.5;
		add(new JLabel(icon), c);
		
		JLabel label;
		
		label = new JLabel("<html><big><b>" +
				Kirjanpito.APP_NAME + " " + Kirjanpito.APP_VERSION +
				"</b></big></html>");
		
		c.gridy = 1;
		c.insets = new Insets(5, 40, 5, 40);
		c.weighty = 0.0;
		add(label, c);
		
		label = new JLabel("Kirjanpito-ohjelma");
		c.gridy = 2;
		c.insets = new Insets(5, 10, 5, 10);
		add(label, c);
		
		label = new JLabel(
			"<html><small>© 2009–2013 Tommi Helineva</small></html>");
		
		c.gridy = 3;
		add(label, c);
		
		JButton closeButton = new JButton("Sulje",
				new ImageIcon(Resources.load("close-22x22.png")));
		closeButton.setMnemonic('S');
		closeButton.setPreferredSize(new Dimension(100, 35));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		
		c.anchor = GridBagConstraints.LAST_LINE_END;
		c.gridy = 4;
		c.insets = new Insets(10, 10, 10, 10);
		c.weighty = 0.5;
		add(closeButton, c);
		
		getRootPane().setDefaultButton(closeButton);
		pack();
		setLocationRelativeTo(getOwner());
	}
}
