package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class AccountSummaryOptionsDialog extends PrintOptionsDialog {
	private static final long serialVersionUID = 1L;
	private JComboBox accountsComboBox;
	private JCheckBox previousPeriodCheckBox;

	public AccountSummaryOptionsDialog(Frame owner) {
		super(owner, "Tilien saldot");
	}
	
	public boolean isPreviousPeriodVisible() {
		return previousPeriodCheckBox.isSelected();
	}
	
	public void setPreviousPeriodVisible(boolean visible) {
		previousPeriodCheckBox.setSelected(visible);
	}
	
	public int getPrintedAccounts() {
		return accountsComboBox.getSelectedIndex();
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 250);
	}

	protected void addExtraOptions(JPanel panel) {
		accountsComboBox = new JComboBox(new String[] {
				"Kaikki tilit", "Taseen tilit", "Tuloslaskelman tilit" });
		previousPeriodCheckBox = new JCheckBox("Edellisen tilikauden vertailu");
		previousPeriodCheckBox.setMnemonic('E');
		JPanel container = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 8, 8, 8);
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		container.add(previousPeriodCheckBox, c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.EAST;
		container.add(accountsComboBox, c);
		c.insets = new Insets(8, 8, 8, 8);
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.SOUTH;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 1.0;
		panel.add(container, c);
	}
}