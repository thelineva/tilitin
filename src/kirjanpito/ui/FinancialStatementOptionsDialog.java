package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class FinancialStatementOptionsDialog extends PrintOptionsDialog {
	private static final long serialVersionUID = 1L;
	private JCheckBox previousPeriodCheckBox;

	public FinancialStatementOptionsDialog(Frame owner, String title) {
		super(owner, title);
	}
	
	public boolean isPreviousPeriodVisible() {
		return previousPeriodCheckBox.isSelected();
	}
	
	public void setPreviousPeriodVisible(boolean visible) {
		previousPeriodCheckBox.setSelected(visible);
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 250);
	}

	protected int addExtraOptions(JPanel panel) {
		previousPeriodCheckBox = new JCheckBox("Edellisen tilikauden vertailu");
		previousPeriodCheckBox.setMnemonic('E');
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(8, 8, 8, 8);
		panel.add(previousPeriodCheckBox, c);
		return 1;
	}
}
