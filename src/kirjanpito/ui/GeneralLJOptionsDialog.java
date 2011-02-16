package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

public class GeneralLJOptionsDialog extends PrintOptionsDialog {
	private static final long serialVersionUID = 1L;
	private JRadioButton orderByNumberRadioButton;
	private JRadioButton orderByDateRadioButton;

	public GeneralLJOptionsDialog(Frame owner, String title) {
		super(owner, title);
	}
	
	public boolean isOrderByDate () {
		return orderByDateRadioButton.isSelected();
	}

	protected Dimension getFrameMinimumSize() {
		return new Dimension(450, 250);
	}

	protected void addExtraOptions(JPanel panel) {
		orderByNumberRadioButton = new JRadioButton("Tositenumerojärjestys");
		orderByDateRadioButton = new JRadioButton("Aikajärjestys");
		JPanel container = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 0, 8);
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		container.add(orderByNumberRadioButton, c);
		c.weightx = 1.0;
		container.add(orderByDateRadioButton, c);
		c.insets = new Insets(8, 8, 8, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 1;
		panel.add(new JSeparator(JSeparator.HORIZONTAL), c);
		c.gridy = 2;
		c.insets = new Insets(8, 8, 16, 8);
		panel.add(container, c);
		
		ButtonGroup group = new ButtonGroup();
		group.add(orderByNumberRadioButton);
		group.add(orderByDateRadioButton);
		orderByNumberRadioButton.setSelected(true);
	}
}
