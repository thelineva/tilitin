package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
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

	protected int addExtraOptions(JPanel panel) {
		accountsComboBox = new JComboBox(new String[] {
				"Kaikki tilit", "Taseen tilit", "Tuloslaskelman tilit" });
		previousPeriodCheckBox = new JCheckBox("Edellisen tilikauden vertailu");
		previousPeriodCheckBox.setMnemonic('E');
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 8, 8, 8);
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		panel.add(previousPeriodCheckBox, c);
		c.gridx = 1;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		panel.add(accountsComboBox, c);
		return 2;
	}
}