package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class GeneralLJOptionsDialog extends PrintOptionsDialog {
	private static final long serialVersionUID = 1L;
	private JRadioButton orderByNumberRadioButton;
	private JRadioButton orderByDateRadioButton;
	private JCheckBox groupByDocumentTypesCheckBox;
	private JCheckBox totalAmountVisibleCheckBox;

	public GeneralLJOptionsDialog(Frame owner, String title) {
		super(owner, title);
	}

	public boolean isOrderByDate () {
		return orderByDateRadioButton.isSelected();
	}

	public void setOrderByDate(boolean orderByDate) {
		orderByDateRadioButton.setSelected(orderByDate);
		orderByNumberRadioButton.setSelected(!orderByDate);
	}

	public boolean isGroupByDocumentTypesEnabled() {
		return groupByDocumentTypesCheckBox.isEnabled();
	}

	public void setGroupByDocumentTypesEnabled(boolean enabled) {
		groupByDocumentTypesCheckBox.setEnabled(enabled);
	}

	public boolean isGroupByDocumentTypesSelected() {
		return groupByDocumentTypesCheckBox.isEnabled() && groupByDocumentTypesCheckBox.isSelected();
	}

	public void setGroupByDocumentTypesSelected(boolean selected) {
		groupByDocumentTypesCheckBox.setSelected(groupByDocumentTypesCheckBox.isEnabled() && selected);
	}

	public boolean isTotalAmountVisible() {
		return totalAmountVisibleCheckBox.isSelected();
	}

	public void setTotalAmountVisible(boolean visible) {
		totalAmountVisibleCheckBox.setSelected(visible);
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 250);
	}

	protected void addExtraOptions(JPanel panel) {
		orderByNumberRadioButton = new JRadioButton("Tositenumerojärjestys");
		orderByNumberRadioButton.setMnemonic('n');
		orderByDateRadioButton = new JRadioButton("Aikajärjestys");
		orderByDateRadioButton.setMnemonic('r');
		groupByDocumentTypesCheckBox = new JCheckBox("Tositelajeittain");
		groupByDocumentTypesCheckBox.setMnemonic('l');
		totalAmountVisibleCheckBox = new JCheckBox("Näytä summarivi");
		totalAmountVisibleCheckBox.setMnemonic('s');

		GridBagConstraints c = new GridBagConstraints();
		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));

		container.add(orderByNumberRadioButton);
		container.add(Box.createRigidArea(new Dimension(10, 0)));
		container.add(orderByDateRadioButton);
		c.insets = new Insets(8, 8, 8, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 1;
		c.insets = new Insets(4, 8, 4, 8);
		panel.add(container, c);

		container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));
		container.add(totalAmountVisibleCheckBox);
		container.add(Box.createRigidArea(new Dimension(10, 0)));
		container.add(groupByDocumentTypesCheckBox);
		c.gridy = 2;
		panel.add(container, c);

		ButtonGroup group = new ButtonGroup();
		group.add(orderByNumberRadioButton);
		group.add(orderByDateRadioButton);
		orderByNumberRadioButton.setSelected(true);
	}
}
