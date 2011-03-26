package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class FinancialStatementOptionsDialog extends PrintOptionsDialog {
	private static final long serialVersionUID = 1L;
	private JCheckBox previousPeriodCheckBox;
	private JCheckBox pageBreakCheckBox;

	public FinancialStatementOptionsDialog(Frame owner, String title) {
		super(owner, title);
	}
	
	public boolean isPreviousPeriodVisible() {
		return previousPeriodCheckBox.isSelected();
	}
	
	public void setPreviousPeriodVisible(boolean visible) {
		previousPeriodCheckBox.setSelected(visible);
	}

	public void setPageBreakCheckBoxVisible(boolean visible) {
		pageBreakCheckBox.setVisible(visible);
	}

	public boolean isPageBreakEnabled() {
		return pageBreakCheckBox.isSelected();
	}

	public void setPageBreakEnabled(boolean enabled) {
		pageBreakCheckBox.setSelected(enabled);
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 250);
	}

	protected void addExtraOptions(JPanel panel) {
		JPanel container = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(8, 8, 8, 8);
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new JSeparator(JSeparator.HORIZONTAL), c);

		c.gridy = 2;
		c.weighty = 1.0;
		panel.add(container, c);

		previousPeriodCheckBox = new JCheckBox("Edellisen tilikauden vertailu");
		previousPeriodCheckBox.setMnemonic('E');
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 0, 12);
		c.weightx = 0.0;
		c.weighty = 1.0;
		container.add(previousPeriodCheckBox, c);

		pageBreakCheckBox = new JCheckBox("Vastattavaa eri sivulle");
		pageBreakCheckBox.setMnemonic('V');
		c.gridx = 1;
		container.add(pageBreakCheckBox, c);
		
		c.weightx = 1.0;
		container.add(new JLabel(), c);
	}
}
