package kirjanpito.models;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JTextField;

import kirjanpito.ui.resources.Resources;

public class TextFieldWithLockIcon extends JTextField {
	private Image lockIcon;
	private boolean lockIconVisible;

	private static final long serialVersionUID = 1L;

	public TextFieldWithLockIcon() {
		lockIcon = new ImageIcon(Resources.load("lock-12x12.png")).getImage();
	}

	public boolean isLockIconVisible() {
		return lockIconVisible;
	}

	public void setLockIconVisible(boolean lockIconVisible) {
		this.lockIconVisible = lockIconVisible;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (lockIconVisible) {
			int x = getWidth() - 20;
			int y = (getHeight() - 12) / 2 + 1;
			g.drawImage(lockIcon, x, y, null);
		}
	}
}
