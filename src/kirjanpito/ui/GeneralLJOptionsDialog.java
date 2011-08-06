package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class GeneralLJOptionsDialog extends PrintOptionsDialog {
	private static final long serialVersionUID = 1L;
	private JRadioButton orderByNumberRadioButton;
	private JRadioButton orderByDateRadioButton;
	private JCheckBox groupByDocumentTypesCheckBox;

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
		JPanel container = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 0, 0, 8);
		c.gridy = 0;
		container.add(orderByNumberRadioButton, c);
		container.add(orderByDateRadioButton, c);
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(0, 8, 0, 0);
		c.weightx = 1.0;
		container.add(groupByDocumentTypesCheckBox, c);
		c.insets = new Insets(8, 8, 8, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 1;
		c.insets = new Insets(8, 8, 16, 8);
		panel.add(container, c);

		ButtonGroup group = new ButtonGroup();
		group.add(orderByNumberRadioButton);
		group.add(orderByDateRadioButton);
		orderByNumberRadioButton.setSelected(true);
	}
}
